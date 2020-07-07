package dev.bmcreations.scrcast.recorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dev.bmcreations.scrcast.recorder.Action

class RecordingNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            val broadcaster = LocalBroadcastManager.getInstance(ctx)

            intent?.action?.let { action ->
                if (action == Action.Stop.name) {
                    broadcaster.sendBroadcast(Intent(Action.Stop.name))
                }
            }
        }
    }
}
