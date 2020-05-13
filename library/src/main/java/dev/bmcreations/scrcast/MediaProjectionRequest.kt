package dev.bmcreations.scrcast

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import dev.bmcreations.dispatcher.ActivityResult
import dev.bmcreations.dispatcher.ActivityResultContainer
import dev.bmcreations.dispatcher.ActivityResultDispatcher
import dev.bmcreations.dispatcher.ReactiveActivityResult
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainScope

interface MediaProjectionResult : ActivityResultDispatcher<ActivityResult>

@OptIn(InternalCoroutinesApi::class)
class MediaProjectionRequest(
    private val context: Context?,
    private val projection: MediaProjectionManager
) : ActivityResultContainer<MediaProjectionResult>(MainScope()) {

    override fun start(cb: MediaProjectionResult?) {
        context?.let { ctx ->
            // create reactive dispatcher
            val reactiveDispatcher = ReactiveActivityResult(ctx)

            // send it
            sendIntent(
                dispatcher = reactiveDispatcher,
                intent = projection.createScreenCaptureIntent(),
                callback = cb
            )
        }
    }

    override fun handleResult(result: ActivityResult, callback: MediaProjectionResult?) {
        if (result.resultCode == Activity.RESULT_OK) {
            callback?.onSuccess(result)
        } else {
            callback?.onFailure(Throwable("Unable to get projection"))
        }
    }
}
