package dev.bmcreations.scrcast.recorder.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dev.bmcreations.scrcast.recorder.RecordingState

abstract class NotificationProvider(private val context: Context) {
    val notificationManager: NotificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    abstract fun createNotificationChannel()

    abstract fun getNotificationId(): Int
    abstract fun getChannelId(): String
    abstract fun get(state: RecordingState): Notification

    open fun update(state: RecordingState) {
        notificationManager.notify(getNotificationId(), get(state))
    }
}
