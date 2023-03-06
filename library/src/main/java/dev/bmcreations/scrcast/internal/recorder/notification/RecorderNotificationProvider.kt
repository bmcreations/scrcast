package dev.bmcreations.scrcast.internal.recorder.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat
import dev.bmcreations.scrcast.config.NotificationConfig
import dev.bmcreations.scrcast.extensions.supportsPauseResume
import dev.bmcreations.scrcast.internal.recorder.Action
import dev.bmcreations.scrcast.internal.recorder.receiver.RecordingNotificationReceiver
import dev.bmcreations.scrcast.recorder.RecordingState
import dev.bmcreations.scrcast.recorder.notification.NotificationProvider

@RestrictTo(RestrictTo.Scope.LIBRARY)
class RecorderNotificationProvider(
    private val context: Context,
    private val config: NotificationConfig
): NotificationProvider(context) {

    init {
        createNotificationChannel()
    }

    private var startTime: Long = 0
    private var elapsedTime: Long = 0

    override fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = with(config.channel) {
                android.app.NotificationChannel(
                    id,
                    name,
                    NotificationManager.IMPORTANCE_NONE
                ).apply {
                    lightColor = config.channel.lightColor
                    lockscreenVisibility = config.channel.lockscreenVisibility
                }
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun getChannelId(): String = config.channel.id

    override fun get(state: RecordingState): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, config.channel.id)
        } else {
            Notification.Builder(context)
        }

        builder.apply {
            setOngoing(true)

            if (config.icon != null) {
                setSmallIcon(Icon.createWithBitmap(config.icon))
            } else {
                setSmallIcon(android.R.drawable.ic_dialog_alert)

            }

            if (config.useMediaStyle) {
                style = Notification.MediaStyle().setShowActionsInCompactView(0)
            }

            setColor(ContextCompat.getColor(context, config.accentColor))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (config.colorAsBackground) {
                    setColorized(true)
                }
            }
            setContentTitle(config.title)
            if (config.description.isEmpty().not()) {
                setContentText(config.description)
            }

            addTimer(state)

            if (config.showPause) {
                addPauseResume(state)
            }

            if (config.showStop) {
                addStop()
            }
        }

        return builder.build()
    }

    override fun getNotificationId(): Int = config.id

    override fun update(state: RecordingState) {
        if (state.isPaused) {
            elapsedTime += System.currentTimeMillis() - startTime
        }
        super.update(state)
    }

    private fun Notification.Builder.addTimer(state: RecordingState) {
        if (config.showTimer) {
            when (state) {
                is RecordingState.Idle,
                is RecordingState.Delay -> {
                    startTime = System.currentTimeMillis()
                    setWhen(startTime)
                    setUsesChronometer(true)
                }
                RecordingState.Recording -> {
                    setWhen(System.currentTimeMillis() - elapsedTime)
                    setUsesChronometer(true)

                    startTime = System.currentTimeMillis()
                }
                RecordingState.Paused -> {
                    setUsesChronometer(false)
                }
            }
        }
    }

    private fun Notification.Builder.addPauseResume(state: RecordingState) {
        if (supportsPauseResume) {
            with(if (state.isPaused) Action.Resume else Action.Pause) {
                val actionIntent = Intent(
                    context,
                    RecordingNotificationReceiver::class.java
                ).apply {
                    action = name
                }

                val actionPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(
                        context, requestId, actionIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                    )

                addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, icon),
                        context.getString(label),
                        actionPendingIntent
                    ).build()
                )
            }
        }
    }

    private fun Notification.Builder.addStop() {
        with (Action.Stop) {
            val stopIntent = Intent(
                context,
                RecordingNotificationReceiver::class.java
            ).apply {
                action = name
            }
            val stopPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context, requestId, stopIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )

            addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, icon),
                    context.getString(label),
                    stopPendingIntent
                ).build()
            )
        }
    }
}
