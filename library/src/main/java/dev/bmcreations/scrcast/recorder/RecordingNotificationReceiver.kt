package dev.bmcreations.scrcast.recorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

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
