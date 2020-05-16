package dev.bmcreations.scrcast

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaRecorder
import android.media.MediaRecorder.AudioEncoder.AAC
import android.media.MediaRecorder.AudioEncoder.AMR_NB
import android.media.MediaRecorder.VideoEncoder.H264
import android.media.MediaRecorder.VideoEncoder.MPEG_4_SP
import android.media.MediaScannerConnection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dev.bmcreations.dispatcher.ActivityResult
import kotlinx.android.parcel.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class Options(
    val width: Int = 1080,
    val height: Int = 1920,
    val videoEncoder: Int = H264,
    val bitrate: Int = 300_000_000,
    val frameRate: Int = 60,
    val directory: String = "scrcast",
    val outputFormat: Int = MediaRecorder.OutputFormat.MPEG_4
): Parcelable

interface OnRecordingStateChange {
    fun onStateChange(recording: Boolean)
}

internal typealias RecordingStateChangeCallback = (Boolean) -> Unit

/**
 * Main Interface for accessing [scrcast] Library
 */
class ScrCast private constructor(val activity: Activity) {

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

    private var options = Options()

    private val broadcaster = LocalBroadcastManager.getInstance(activity)

    private val recordingStateHandler = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.action?.let { action ->
                when(action) {
                    RecordingStateChange.Recording.name -> isRecording = true
                    RecordingStateChange.IdleOrFinished.name -> isRecording = false
                }
            }
        }
    }

    private val dpi by lazy {
        DisplayMetrics().apply { activity.windowManager.defaultDisplay.getMetrics(this) }.density
    }

    private var _outputFile: File? = null

    val outputDirectory: File?
        get() {
            val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),options.directory)
            mediaStorageDir.apply {
                if (!exists()) {
                    if (!mkdirs()) {
                        Log.d("scrcast", "failed to created output directory")
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
                    val timestamp = SimpleDateFormat(
                        "MM_dd_yyyy_hhmmss",
                        Locale.getDefault()
                    ).format(Date())
                    _outputFile = File("${dir.path}${File.separator}$timestamp.mp4")
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
        MediaProjectionRequest(activity, projectionManager).start(object : MediaProjectionResult {
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
                addAction(RecordingStateChange.IdleOrFinished.name)
                addAction(RecordingStateChange.Recording.name)
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
