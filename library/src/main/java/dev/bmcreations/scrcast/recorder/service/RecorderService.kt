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
import dev.bmcreations.scrcast.recorder.Action
import dev.bmcreations.scrcast.recorder.RecordingState
import dev.bmcreations.scrcast.recorder.receiver.RecordingNotificationReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class RecorderService : Service() {

    private val projectionManager: MediaProjectionManager by lazy {
        getSystemService(MediaProjectionManager::class.java)
    }

    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    private val broadcaster = LocalBroadcastManager.getInstance(this)

    private var screenHandler = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d("scrcast", "stopping recording with screen off per request")
                if (state == RecordingState.InDelay) {
                    stopRecording()
                }
            }
        }
    }

    private var state: RecordingState = RecordingState.IdleOrFinished
    set(value) {
        field = value
        broadcaster.sendBroadcast(Intent(value.name))
    }

    private var options: Options = Options()
    private lateinit var outputFile: String
    private var rotation = 0
    private val orientation by lazy {
        orientations.get(rotation + 90)
    }

    private var dpi: Float = 0f

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = with(options.notification.channel) {
                NotificationChannel(
                    id,
                    name,
                    NotificationManager.IMPORTANCE_NONE
                ).apply {
                    lightColor = Color.BLUE
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                }
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }


    private fun startRecording(code: Int, data: Intent) {
        GlobalScope.launch(Dispatchers.IO) {
            with(options.startDelayMs) {
                if (this > 0) {
                    state = RecordingState.InDelay
                }
                delay(options.startDelayMs)
            }

            GlobalScope.launch(Dispatchers.Main) {
                startForeground(options.notification.id, setupNotification())
                mediaProjection = projectionManager.getMediaProjection(code, data)

                virtualDisplay // touch

                if (options.stopOnScreenOff) {
                    with(IntentFilter(Intent.ACTION_SCREEN_OFF)) {
                        registerReceiver(screenHandler, this)
                    }
                }

                mediaProjection?.registerCallback(mediaProjectionCallback, Handler())
                mediaRecorder.start()
                state = RecordingState.Recording
            }
        }
    }

    private fun stopRecording() {
        if (_virtualDisplay == null) {
            return
        }
        _virtualDisplay?.release()
        destroyMediaProjection()
        state = RecordingState.IdleOrFinished
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

            createNotificationChannel()

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
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun setupNotification(): Notification {
        return with(options.notification) {
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this@RecorderService, options.notification.channel.id)
            } else {
                Notification.Builder(this@RecorderService)
            }

            builder.apply {
                setOngoing(true)

                if (icon != null) {
                    setSmallIcon(Icon.createWithBitmap(icon))
                } else {
                    setSmallIcon(android.R.drawable.ic_dialog_alert)
                }

                setContentTitle(title)
                setContentText(description)

                if (showTimer) {
                    setWhen(System.currentTimeMillis())
                    setUsesChronometer(true)
                }

                if (showStop) {
                    val stopIntent = Intent(
                        this@RecorderService,
                        RecordingNotificationReceiver::class.java
                    ).apply {
                        action = Action.Stop.name
                    }
                    val stopPendingIntent: PendingIntent =
                        PendingIntent.getBroadcast(
                            this@RecorderService, 0, stopIntent, 0
                        )

                    addAction(Notification.Action.Builder(
                        Icon.createWithResource(this@RecorderService, R.drawable.ic_stop),
                        this@RecorderService.getString(R.string.stop),
                        stopPendingIntent).build()
                    )
                }
            }
            builder.build()
        }
    }
}
