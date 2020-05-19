package dev.bmcreations.scrcast.recorder

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dev.bmcreations.scrcast.config.Options
import dev.bmcreations.scrcast.config.orientations


class RecorderService : Service() {

    private val projectionManager: MediaProjectionManager by lazy {
        getSystemService(MediaProjectionManager::class.java)
    }

    private val broadcaster = LocalBroadcastManager.getInstance(this)

    private var options: Options =
        Options()
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
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(options.storage.outputFormat)
            setOutputFile(outputFile)
            with(options.video) {
                setVideoSize(width, height)
                setVideoEncoder(videoEncoder)
                setVideoEncodingBitRate(bitrate)
                setVideoFrameRate(frameRate)
            }
            setOrientationHint(orientation)
            prepare()
        }
    }

    private fun startRecording() {
        mediaProjection?.registerCallback(mediaProjectionCallback, Handler())
        mediaRecorder.start()
        broadcaster.sendBroadcast(Intent(RecordingState.Recording.name))
    }

    private fun stopRecording() {
        if (_virtualDisplay == null) {
            return
        }
        _virtualDisplay?.release()
        destroyMediaProjection()
        broadcaster.sendBroadcast(Intent(RecordingState.IdleOrFinished.name))
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

            val code = it.getIntExtra("code", -1)
            val data = it.getParcelableExtra("data") ?: Intent()

            setupNotification()

            mediaProjection = projectionManager.getMediaProjection(code, data)

            virtualDisplay // touch

            startRecording()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun setupNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "1337"
            val channelName = "Screen Recording"
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            getSystemService(NotificationManager::class.java)?.let {
                it.createNotificationChannel(channel)

                val notification: Notification = Notification.Builder(this, channelId).apply {
                    setOngoing(true)
                    setSmallIcon(android.R.drawable.ic_dialog_alert)
                    setContentTitle("Recording")
                    setContentText("in process")
                }.build()

                startForeground(101, notification)
            }
        } else {
            startForeground(101, Notification())
        }

    }
}
