package dev.bmcreations.scrcast

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dev.bmcreations.dispatcher.ActivityResult
import dev.bmcreations.scrcast.config.Options
import dev.bmcreations.scrcast.recorder.OnRecordingStateChange
import dev.bmcreations.scrcast.recorder.RecorderService
import dev.bmcreations.scrcast.recorder.RecordingState
import dev.bmcreations.scrcast.recorder.RecordingStateChangeCallback
import dev.bmcreations.scrcast.request.MediaProjectionRequest
import dev.bmcreations.scrcast.request.MediaProjectionResult
import java.io.File

/**
 * Main Interface for accessing [scrcast] Library
 */
class ScrCast private constructor(private val activity: Activity) {

    var isRecording = false
        private set(value) {
            field = value
            onStateChange?.invoke(value)
            if (!value) {
                try {
                    broadcaster.unregisterReceiver(recordingStateHandler)
                } catch (swallow: Exception) { }
            }
        }

    private var onStateChange: RecordingStateChangeCallback? = null

    private val metrics by lazy {
        DisplayMetrics().apply { activity.windowManager.defaultDisplay.getMetrics(this) }
    }

    private val dpi by lazy { metrics.density }

    private var options = Options(width = metrics.widthPixels, height = metrics.heightPixels)

    private val broadcaster = LocalBroadcastManager.getInstance(activity)

    private val recordingStateHandler = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.action?.let { action ->
                when(action) {
                    RecordingState.Recording.name -> isRecording = true
                    RecordingState.IdleOrFinished.name -> isRecording = false
                }
            }
        }
    }

    private var _outputFile: File? = null

    val outputDirectory: File?
        get() {
            val mediaStorageDir = options.mediaStorageLocation
            mediaStorageDir.apply {
                if (!exists()) {
                    if (!mkdirs()) {
                        Log.d("scrcast", "failed to create designated output directory")
                        return null
                    }
                }
            }

            return mediaStorageDir
        }

    private val outputFile: File?
        get() {
            if (_outputFile == null) {
                outputDirectory?.let { dir ->
                    _outputFile = File("${dir.path}${File.separator}${options.fileNameFormatter}.mp4")
                } ?: return null
            }
            return _outputFile
        }


    private val projectionManager: MediaProjectionManager by lazy {
        activity.getSystemService(MediaProjectionManager::class.java)
    }

    private val permissionListener = object : MultiplePermissionsListener {
        override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
            startRecording()
        }

        override fun onPermissionRationaleShouldBeShown(
            p0: MutableList<PermissionRequest>?,
            p1: PermissionToken?
        ) {
            p1?.continuePermissionRequest()
        }
    }

    private fun startRecording() {
        MediaProjectionRequest(
            activity,
            projectionManager
        ).start(object : MediaProjectionResult {
            override fun onCancel() = Unit
            override fun onFailure(error: Throwable) = Unit

            override fun onSuccess(result: ActivityResult?) {
                if (result != null) {
                   // activity.moveTaskToBack(true)
                    val output = outputFile
                    if (output != null) {
                        startService(result, output)
                    }
                }
            }
        })
    }

    fun updateOptions(block: Options.() -> Options) {
        options = block(options)
    }

    @JvmSynthetic
    fun updateOptions(options: Options) {
        this.options = options
    }

    fun setOnStateChangeListener(listener : OnRecordingStateChange) {
        onStateChange = { listener.onStateChange(it) }
    }

    @JvmSynthetic
    fun setOnStateChangeListener(callback: (Boolean) -> Unit) {
        onStateChange = callback
    }

    /**
     * Convenience method for clients to easily check if the required permissions are enabled for storage
     * Even though we internally will bubble up the permission request and handle the allow/deny,
     * some clients may want to onboard users via an OOBE or some UX state involving previously recorded files.
     */
    fun hasStoragePermissions(): Boolean {
        val perms = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        return perms.all { ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }
    }

    /**
     *
     */
    fun record() {
        if (!isRecording) {
            Dexter.withContext(activity)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(CompositeMultiplePermissionsListener(permissionListener, dialogPermissionListener))
                .check()
        } else {
            stopRecording()
        }
    }

    fun stopRecording() {
        val service = Intent(activity, RecorderService::class.java)
        activity.stopService(service)

        MediaScannerConnection.scanFile(activity, arrayOf(outputFile.toString()), null) { path, uri ->
            Log.i("External", "scanned: $path")
            Log.i("External", "-> uri=$uri")
            _outputFile = null
        }
    }

    private fun startService(result: ActivityResult, file : File) {
        val service = Intent(activity, RecorderService::class.java).apply {
            putExtra("code", result.resultCode)
            putExtra("data", result.data)
            putExtra("options", options)
            putExtra("outputFile", file.absolutePath)
            putExtra("dpi", dpi)
            putExtra("rotation", activity.windowManager.defaultDisplay.rotation)
        }

        broadcaster.registerReceiver(
            recordingStateHandler,
            IntentFilter().apply {
                addAction(RecordingState.IdleOrFinished.name)
                addAction(RecordingState.Recording.name)
            }
        )

        activity.startService(service)
    }

    private val dialogPermissionListener: DialogOnAnyDeniedMultiplePermissionsListener = DialogOnAnyDeniedMultiplePermissionsListener.Builder
        .withContext(activity)
        .withTitle("Storage permissions")
        .withMessage("Storage permissions are needed to store the screen recording")
        .withButtonText(android.R.string.ok)
        .withIcon(R.drawable.ic_storage_permission_dialog)
        .build()

    companion object {
        @JvmStatic
        fun use(activity: Activity): ScrCast {
            return ScrCast(activity)
        }
    }
}
