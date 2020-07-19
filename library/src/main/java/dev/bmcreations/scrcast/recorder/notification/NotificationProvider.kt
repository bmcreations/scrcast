package dev.bmcreations.scrcast.recorder.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dev.bmcreations.scrcast.ScrCast
import dev.bmcreations.scrcast.recorder.RecordingState

/**
 * Provider contract for managing the recording notification used by [ScrCast]
 */
abstract class NotificationProvider(private val context: Context) {
    /**
     * [NotificationManager] instantiated via the provided [Context], available to subclasses
     * for [NotificationManager.notify] and other purposes
     */
    protected val notificationManager: NotificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    /**
     * Required method for subclasses to define their own [NotificationChannel]
     */
    abstract fun createNotificationChannel()
    /**
     * Required method for subclasses to define their [Notification] unique identifier,
     * queried by [update] to update the notification on state changes if required.
     */
    abstract fun getNotificationId(): Int
    /**
     * Required method for subclasses to define their [NotificationChannel] unique identifier.
     */
    abstract fun getChannelId(): String
    /**
     * Required method for subclasses to provide their [Notification] to the recording service.
     */
    abstract fun get(state: RecordingState): Notification

    /**
     * Updates the current notification based on the new [RecordingState].
     *
     * Can be overridden to handle internal events related to your [Notification].
     *
     * (e.g handle timer during pause/resume)
     */
    open fun update(state: RecordingState) {
        notificationManager.notify(getNotificationId(), get(state))
    }
}
