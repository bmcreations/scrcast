package dev.bmcreations.scrcast.internal.recorder.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaRecorder.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dev.bmcreations.scrcast.config.Options
import dev.bmcreations.scrcast.internal.extensions.countdown
import dev.bmcreations.scrcast.internal.recorder.*
import dev.bmcreations.scrcast.recorder.*
import dev.bmcreations.scrcast.recorder.notification.NotificationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

@RestrictTo(RestrictTo.Scope.LIBRARY)
class RecorderService : Service() {

    private val projectionManager: MediaProjectionManager by lazy {
        getSystemService(MediaProjectionManager::class.java)
    }

    private val broadcaster by lazy {
        LocalBroadcastManager.getInstance(this)
    }

    private val binder = LocalBinder()

    private lateinit var notificationProvider: NotificationProvider

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

    private var state: RecordingState = RecordingState.Idle()
        set(value) {
            field = value
            broadcaster.sendBroadcast(Intent(value.stateString()).apply {
                if (value is RecordingState.Delay) {
                    putExtra(EXTRA_DELAY_REMAINING, value.remainingSeconds)
                } else if (value is RecordingState.Idle) {
                    putExtra(EXTRA_ERROR, value.error)
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
                    mediaRecorder?.surface,
                    null,
                    null
                )
            }
            return _virtualDisplay
        }

    private var mediaRecorder: MediaRecorder? = null

    private fun createRecorder() {
        Log.d("scrcast", "createRecorder()")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(AudioSource.MIC)
            setVideoSource(VideoSource.SURFACE)
            setOutputFormat(options.storage.outputFormat)
            setAudioEncoder(AudioEncoder.HE_AAC)
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
                        Log.d(
                            "scrcast",
                            "max duration of ${options.video.maxLengthSecs} seconds reached. Stopping reconrding..."
                        )
                        stopRecording()
                    }
                    MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING -> Log.d(
                        "scrcast",
                        "Approaching max file size of ${options.storage.maxSizeMB}MB"
                    )
                    MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED -> {
                        Log.d(
                            "scrcast",
                            "max file size of ${options.storage.maxSizeMB}MB reached. Stopping reconrding..."
                        )
                        stopRecording()
                    }

                }
            }
            setOrientationHint(orientation)
        }
        mediaRecorder?.prepare()
    }

    fun setNotificationProvider(provider: NotificationProvider) {
        notificationProvider = provider
    }

    private fun pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (state.isRecording) {
                mediaRecorder?.pause()
            }
            state = RecordingState.Paused

            notificationProvider.update(state)
        }
    }

    private fun resume() {
        when (state) {
            is RecordingState.Idle -> startRecording()
            RecordingState.Paused -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.resume()
                    state = RecordingState.Recording
                    notificationProvider.update(state)
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
            startForeground(
                notificationProvider.getNotificationId(),
                notificationProvider.get(state)
            )
            mediaProjection = projectionManager.getMediaProjection(code, data)

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
            createRecorder()
            virtualDisplay // touch
            try {
                mediaRecorder?.start()
                state = RecordingState.Recording
                notificationProvider.update(state)
            } catch (e: Exception) {
                stopRecording(e)
            }
        }
    }

    private fun stopRecording(error: Throwable? = null) {
        mediaProjection?.stop()

        state = RecordingState.Idle(error)
        stopForeground(true)
    }

    private fun cleanupProjection() {
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection = null

        _virtualDisplay?.release()

        runCatching {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
        }
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            Log.d("scrcast", "projection on stop")
            cleanupProjection()
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
        Log.d("scrcast", "onDestroy: service")
        stopRecording()
        if (options.stopOnScreenOff) {
            unregisterReceiver(screenHandler)
        }
        try {
            broadcaster.unregisterReceiver(pauseResumeHandler)
        } catch (swallow: Exception) {
        }

        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = binder

    // Class used for the client Binder.
    inner class LocalBinder : Binder() {
        // Return this instance of MyService so clients can call public methods
        val service: RecorderService
            get() = this@RecorderService
    }
}
