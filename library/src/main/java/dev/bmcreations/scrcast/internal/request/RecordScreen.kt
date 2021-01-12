package dev.bmcreations.scrcast.internal.request

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
@RestrictTo(RestrictTo.Scope.LIBRARY)

class RecordScreen : ActivityResultContract<Void, ActivityResult>() {
    override fun createIntent(context: Context, input: Void?): Intent {
        val pm = context.getSystemService(MediaProjectionManager::class.java)
        return pm.createScreenCaptureIntent()
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult {
        return ActivityResult(resultCode, intent)
    }
}
