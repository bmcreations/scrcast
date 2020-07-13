package dev.bmcreations.scrcast.recorder.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaRecorder.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dev.bmcreations.scrcast.R
import dev.bmcreations.scrcast.config.Options
import dev.bmcreations.scrcast.config.orientations
import dev.bmcreations.scrcast.extensions.countdown
import dev.bmcreations.scrcast.recorder.*
import dev.bmcreations.scrcast.recorder.notification.RecorderNotification
import dev.bmcreations.scrcast.recorder.receiver.RecordingNotificationReceiver
import kotlinx.coroutines.*
import java.lang.Exception


class RecorderService : Service() {

    private val projectionManager: MediaProjectionManager by lazy {
        getSystemService(MediaProjectionManager::class.java)
    }

    private val broadcaster = LocalBroadcastManager.getInstance(this)

    private val recorderNotification by lazy {
        RecorderNotification(this, options)
    }

    private val pauseResumeHandler = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PAUSE -> pause()
                ACTION_RESUME -> resume()
                ACTION_STOP -> stopRecording()
            }
        }
    }
    private val screenHandler = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d("scrcast", "stopping recording with screen off per request")
                    if (state == RecordingState.Recording) {
                        stopRecording()
                    }
                }
            }
        }
    }

    private var state: RecordingState = RecordingState.Idle
    set(value) {
        field = value
        broadcaster.sendBroadcast(Intent(value.action).apply {
            if (value is RecordingState.Delay) {
                putExtra(EXTRA_DELAY_REMAINING, value.remainingSeconds)
            }
        })
    }

    private var options: Options = Options()
    private lateinit var outputFile: String
    private var rotation = 0
    private val orientation by lazy {
        orientations.get(rotation + 90)
    }
    private var dpi: Float = 0f

    private var requestCode: Int = -1
    private var requestData: Intent = Intent()

    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionCallback = MediaProjectionCallback()

    private var _virtualDisplay: VirtualDisplay? = null
    private val virtualDisplay: VirtualDisplay?
        get() {
            if (_virtualDisplay == null) {
                _virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "SrcCast",
                    options.video.width,
                    options.video.height,
                    dpi.toInt(),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.surface,
                    null,
                    null
                )
            }
            return _virtualDisplay
        }

    private val mediaRecorder: MediaRecorder by lazy {
        MediaRecorder().apply {
            setVideoSource(VideoSource.SURFACE)
            setOutputFormat(options.storage.outputFormat)
            setOutputFile(outputFile)
            with(options.video) {
                setVideoSize(width, height)
                setVideoEncoder(videoEncoder)
                setVideoEncodingBitRate(bitrate)
                setVideoFrameRate(frameRate)
                if (maxLengthSecs > 0) {
                    setMaxDuration(maxLengthSecs * 1000)
                }
            }
            with(options.storage) {
                if (maxSizeMB > 0) {
                    setMaxFileSize((maxSizeMB * (1024 * 1024)).toLong())
                }
            }
            setOnInfoListener { _, what, _ ->
                when (what) {
                    MEDIA_RECORDER_INFO_MAX_DURATION_REACHED -> {
                        Log.d("scrcast", "max duration of ${options.video.maxLengthSecs} seconds reached. Stopping reconrding...")
                        stopRecording()
                    }
                    MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING -> Log.d("scrcast", "Approaching max file size of ${options.storage.maxSizeMB}MB")
                    MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED -> {
                        Log.d("scrcast", "max file size of ${options.storage.maxSizeMB}MB reached. Stopping reconrding...")
                        stopRecording()
                    }

                }
            }
            setOrientationHint(orientation)
            prepare()
        }
    }

    private fun pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (state.isRecording) {
                mediaRecorder.pause()
            }
            state = RecordingState.Paused
            recorderNotification.update(state)
        }
    }

    private fun resume() {
        when (state) {
            RecordingState.Idle -> startRecording()
            RecordingState.Paused -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder.resume()
                    state = RecordingState.Recording
                    recorderNotification.update(state)
                }
            }
        }
    }

    private fun startRecording(code: Int = requestCode, data: Intent = requestData) {
        requestCode = code
        requestData = data

        if (options.startDelayMs > 0) {
            options.startDelayMs.countdown(
                repeatMillis = 1_000,
                onTick = { state = RecordingState.Delay((it / 1000).toInt() + 1) },
                after = { recordInternal(code, data) }
            )
        } else {
            recordInternal(code, data)
        }
    }

    private fun recordInternal(code: Int, data: Intent) {
        GlobalScope.launch(Dispatchers.Main) {
            startForeground(options.notification.id, recorderNotification.createFrom(state))
            mediaProjection = projectionManager.getMediaProjection(code, data)

            virtualDisplay // touch

            if (options.stopOnScreenOff) {
                with(IntentFilter(Intent.ACTION_SCREEN_OFF)) {
                    registerReceiver(screenHandler, this)
                }
            }

            with(IntentFilter(ACTION_PAUSE).apply {
                addAction(ACTION_RESUME)
                addAction(ACTION_STOP)
            }) {
                broadcaster.registerReceiver(pauseResumeHandler, this)
            }

            mediaProjection?.registerCallback(mediaProjectionCallback, Handler())
            mediaRecorder.start()
            state = RecordingState.Recording
        }
    }

    private fun stopRecording() {
        if (_virtualDisplay == null) {
            return
        }
        _virtualDisplay?.release()
        destroyMediaProjection()
        state = RecordingState.Idle
        stopForeground(true)
    }

    private fun destroyMediaProjection() {
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            mediaRecorder.stop()
            mediaRecorder.reset()
            stopRecording()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            options = it.getParcelableExtra("options") ?: Options()
            rotation = it.getIntExtra("rotation", 0)
            dpi = it.getFloatExtra("dpi", 0f)
            outputFile = it.getStringExtra("outputFile") ?: ""

            startRecording(
                code = it.getIntExtra("code", -1),
                data = it.getParcelableExtra("data") ?: Intent()
            )
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopRecording()
        if (options.stopOnScreenOff) {
            unregisterReceiver(screenHandler)
        }
        try {
            broadcaster.unregisterReceiver(pauseResumeHandler)
        } catch (swallow: Exception) {}

        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}
