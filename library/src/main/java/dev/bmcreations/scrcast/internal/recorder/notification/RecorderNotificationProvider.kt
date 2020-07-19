package dev.bmcreations.scrcast.internal.recorder.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RestrictTo
import dev.bmcreations.scrcast.config.Options
import dev.bmcreations.scrcast.extensions.supportsPauseResume
import dev.bmcreations.scrcast.internal.recorder.Action
import dev.bmcreations.scrcast.internal.recorder.receiver.RecordingNotificationReceiver
import dev.bmcreations.scrcast.recorder.RecordingState
import dev.bmcreations.scrcast.recorder.notification.NotificationProvider

@RestrictTo(RestrictTo.Scope.LIBRARY)
class RecorderNotificationProvider(
    private val context: Context,
    private val options: Options
): NotificationProvider(context) {

    init {
        createNotificationChannel()
    }

    private var startTime: Long = 0
    private var elapsedTime: Long = 0

    override fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = with(options.notification.channel) {
                android.app.NotificationChannel(
                    id,
                    name,
                    NotificationManager.IMPORTANCE_NONE
                ).apply {
                    lightColor = android.graphics.Color.BLUE
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun getChannelId(): String = options.notification.channel.id

    override fun get(state: RecordingState): Notification {
        return with(options.notification) {
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, channel.id)
            } else {
                Notification.Builder(context)
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

                if (showPause) {
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
                                    context, requestId, actionIntent, PendingIntent.FLAG_ONE_SHOT
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

                if (showStop) {
                    with (Action.Stop) {
                        val stopIntent = Intent(
                            context,
                            RecordingNotificationReceiver::class.java
                        ).apply {
                            action = name
                        }
                        val stopPendingIntent: PendingIntent =
                            PendingIntent.getBroadcast(
                                context, requestId, stopIntent, PendingIntent.FLAG_ONE_SHOT
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
            builder.build()
        }
    }

    override fun getNotificationId(): Int = options.notification.id

    override fun update(state: RecordingState) {
        if (state.isPaused) {
            elapsedTime += System.currentTimeMillis() - startTime
        }
        super.update(state)
    }
}
