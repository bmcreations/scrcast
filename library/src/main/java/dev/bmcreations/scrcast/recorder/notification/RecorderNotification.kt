package dev.bmcreations.scrcast.recorder.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import dev.bmcreations.scrcast.config.Options
import dev.bmcreations.scrcast.recorder.Action
import dev.bmcreations.scrcast.recorder.RecordingState
import dev.bmcreations.scrcast.recorder.receiver.RecordingNotificationReceiver

class RecorderNotification(private val context: Context, private val options: Options) {

    private val notificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    private var notification: Notification? = null

    init {
        createNotificationChannel()
    }

    private var startTime: Long = 0
    private var elapsedTime: Long = 0

    private fun createNotificationChannel() {
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

    fun createFrom(state: RecordingState): Notification {
        notification = with(options.notification) {
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, options.notification.channel.id)
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
                        RecordingState.Idle,
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
        return notification!!
    }

    fun update(state: RecordingState) {
        if (state.isPaused) {
            elapsedTime += System.currentTimeMillis() - startTime
        }
        notificationManager.notify(options.notification.id, createFrom(state))
    }
}
