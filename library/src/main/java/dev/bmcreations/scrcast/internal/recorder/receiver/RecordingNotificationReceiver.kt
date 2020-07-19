package dev.bmcreations.scrcast.internal.recorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dev.bmcreations.scrcast.internal.recorder.ACTION_PAUSE
import dev.bmcreations.scrcast.internal.recorder.ACTION_RESUME
import dev.bmcreations.scrcast.internal.recorder.ACTION_STOP

@RestrictTo(RestrictTo.Scope.LIBRARY)
class RecordingNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            val broadcaster = LocalBroadcastManager.getInstance(ctx)

            intent?.action?.let { action ->
                when (action) {
                    ACTION_STOP,
                    ACTION_PAUSE,
                    ACTION_RESUME -> {
                        broadcaster.sendBroadcast(Intent(action))
                    }
                    else -> {}
                }
            }
        }
    }
}
