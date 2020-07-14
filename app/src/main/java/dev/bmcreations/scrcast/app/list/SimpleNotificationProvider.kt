package dev.bmcreations.scrcast.app.list

import android.app.Notification
import android.content.Context
import android.os.Build
import dev.bmcreations.scrcast.app.R
import dev.bmcreations.scrcast.recorder.RecordingState
import dev.bmcreations.scrcast.recorder.notification.NotificationProvider

class SimpleNotificationProvider(private val context: Context) : NotificationProvider(context) {


    init {
        createNotificationChannel()
    }

    override fun getChannelId(): String = CHANNEL_ID

    override fun get(state: RecordingState): Notification {
        val builder = with(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }) {
            setOngoing(true)
            setContentTitle("scrcast-sample")
            setContentText(when (state) {
                RecordingState.Recording -> "state=recording"
                RecordingState.Idle -> "state=idle"
                RecordingState.Paused -> "state=paused"
                is RecordingState.Delay -> "state=delay"
            })

            setSmallIcon(R.drawable.ic_camcorder)
        }

        return builder.build()
    }

    override fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = android.app.NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lightColor = android.graphics.Color.BLUE
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun getNotificationId(): Int = 2000

    companion object {
        private const val CHANNEL_ID = "1338"
        private const val CHANNEL_NAME = "Recording Service Provided"
    }
}
