package dev.bmcreations.scrcast.recorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dev.bmcreations.scrcast.recorder.ACTION_PAUSE
import dev.bmcreations.scrcast.recorder.ACTION_RESUME
import dev.bmcreations.scrcast.recorder.ACTION_STOP
import dev.bmcreations.scrcast.recorder.Action

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
