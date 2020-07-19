package dev.bmcreations.scrcast

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
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
import dev.bmcreations.scrcast.internal.config.dsl.OptionsBuilder
import dev.bmcreations.scrcast.extensions.supportsPauseResume
import dev.bmcreations.scrcast.internal.recorder.*
import dev.bmcreations.scrcast.recorder.*
import dev.bmcreations.scrcast.recorder.RecordingState.*
import dev.bmcreations.scrcast.recorder.RecordingStateChangeCallback
import dev.bmcreations.scrcast.recorder.notification.NotificationProvider
import dev.bmcreations.scrcast.internal.recorder.notification.RecorderNotificationProvider
import dev.bmcreations.scrcast.internal.recorder.service.RecorderService
import dev.bmcreations.scrcast.internal.request.MediaProjectionRequest
import dev.bmcreations.scrcast.internal.request.MediaProjectionResult
import java.io.File

/**
 * Main Interface for accessing [ScrCast] Library
 */
class ScrCast private constructor(private val activity: Activity) {

    /**
     * The current [RecordingState] of the recorder
     *
     * @see [RecordingState]
     */
    var state: RecordingState = Idle()
        private set(value) {
            val was = field
            field = value
            onStateChange?.invoke(value)
            if (was == Recording && value is Idle) {
                try {
                    broadcaster.unregisterReceiver(recordingStateHandler)
                } catch (swallow: Exception) { }

                activity.unbindService(connection)
                scanForOutputFile()
            }
        }

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as RecorderService.LocalBinder
            serviceBinder = binder.service
            serviceBinder?.setNotificationProvider(notificationProvider ?: defaultNotificationProvider)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBinder = null
        }
    }

    private val dialogPermissionListener: DialogOnAnyDeniedMultiplePermissionsListener = DialogOnAnyDeniedMultiplePermissionsListener.Builder
        .withContext(activity)
        .withTitle("Storage permissions")
        .withMessage("Storage permissions are needed to store the screen recording")
        .withButtonText(android.R.string.ok)
        .withIcon(R.drawable.ic_storage_permission_dialog)
        .build()

    private val defaultNotificationProvider by lazy {
        RecorderNotificationProvider(
            activity,
            options
        )
    }
    private var notificationProvider: NotificationProvider? = null

    private var onStateChange: RecordingStateChangeCallback? = null
    private var onRecordingOutput: RecordingOutputFileCallback? = null

    private val metrics by lazy {
        DisplayMetrics().apply { activity.windowManager.defaultDisplay.getMetrics(this) }
    }

    private val dpi by lazy { metrics.density }

    private var options = Options()

    private var serviceBinder: RecorderService? = null

    private val broadcaster by lazy {
        LocalBroadcastManager.getInstance(activity)
    }

    private val recordingStateHandler = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?.action?.let { action ->
                when(action) {
                    STATE_RECORDING -> state = Recording
                    STATE_IDLE -> state = Idle(p1.extras?.get(EXTRA_ERROR) as? Throwable)
                    STATE_DELAY -> {
                        state = Delay(p1.extras?.getInt(EXTRA_DELAY_REMAINING) ?: 0)
                    }
                    STATE_PAUSED -> state = Paused
                }
            }
        }
    }

    private val outputDirectory: File?
        get() {
            val mediaStorageDir = options.storage.mediaStorageLocation
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

    private var _outputFile: File? = null
    private val outputFile: File?
        get() {
            if (_outputFile == null) {
                outputDirectory?.let { dir ->
                    _outputFile = File("${dir.path}${File.separator}${options.storage.fileNameFormatter()}.mp4")
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

    /**
     * Updates the configurations of [ScrCast] via a DSL.
     *
     * @see [Options]
     *
     * This method is not accessible to the JVM.
     */
    @JvmSynthetic
    fun options(opts: OptionsBuilder.() -> Unit) {
        options = handleDynamicVideoSize(OptionsBuilder().apply(opts).build())
    }
    /**
     * Updates the configurations of [ScrCast].
     *
     * @see [Options]
     */
    fun updateOptions(options: Options) {
        this.options = handleDynamicVideoSize(options)
    }

    /**
     * Set the recording callbacks, emitting changes of [RecordingState] as they occur and a link to the output [File]
     */
    fun setRecordingCallback(listener : RecordingCallbacks) {
        onStateChange = { listener.onStateChange(it) }
        onRecordingOutput = { listener.onRecordingFinished(it) }
    }

    /**
     * Set an explicit state change listener, as a kotlin lambda, emitting changes of [RecordingState] as they occur.
     *
     * This is an alternative to providing the combined [RecordingCallbacks] if you are only interested in state changes or
     * want to define them independently.
     *
     * This method is not accessible to the JVM.
     */
    @JvmSynthetic
    fun onRecordingStateChange(callback: RecordingStateChangeCallback) {
        onStateChange = callback
    }

    /**
     * Set an explicit output file listener, as a kotlin lambda.
     *
     * This is an alternative to providing the combined [RecordingCallbacks] if you are only interested in the output file or
     * want to define them independently.
     *
     * This method is not accessible to the JVM.
     */
    @JvmSynthetic
    fun onRecordingComplete(callback: RecordingOutputFileCallback) {
        onRecordingOutput = callback
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
     * Set the [NotificationProvider] for the [ScrCast] instance.
     *
     * @see [NotificationProvider]
     */
    fun setNotificationProvider(provider: NotificationProvider) {
        notificationProvider = provider
    }

    /**
     * Triggers a recording session based on the configuration's defined by [options]
     *
     * @see [updateOptions]
     * @see [Options]
     * @see [MediaRecorder.start]
     */
    fun record() {
        when (state) {
            is Idle -> {
                Dexter.withContext(activity)
                    .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .withListener(CompositeMultiplePermissionsListener(permissionListener, dialogPermissionListener))
                    .check()
            }
            Paused -> resume()
            Recording -> stopRecording()
            is Delay -> { /* Prevent erroneous calls to record while in start delay */}
        }
    }

    /**
     * Triggers the end to a recording session that was started via [record]
     *
     * @see [MediaRecorder.stop]
     */
    fun stopRecording() {
        broadcaster.sendBroadcast(Intent(Action.Stop.name))
    }

    /**
     * Pauses a recording session that was started via [record]
     *
     * This only invokes a change to the recording state if the target device
     * is [Build.VERSION_CODES.N] or higher.
     *
     * @see [MediaRecorder.pause]
     */
    fun pause() {
        if (supportsPauseResume) {
            if (state.isRecording) {
                broadcaster.sendBroadcast(Intent(Action.Pause.name))
            }
        }
    }

    /**
     * Resumed a recording session that was paused via [pause], or triggers a new recording
     * if the [state] is not paused.
     *
     * * This only invokes a change to the recording state if the target device
     * is [Build.VERSION_CODES.N] or higher.
     *
     * @see [MediaRecorder.resume]
     */
    fun resume() {
        if (supportsPauseResume) {
            if (state.isPaused) {
                broadcaster.sendBroadcast(Intent(Action.Resume.name))
            } else {
                record()
            }
        }
    }

    private fun handleDynamicVideoSize(options: Options): Options {
        var reconfig: Options = options
        if (options.video.width == -1) {
            reconfig = reconfig.copy(video = reconfig.video.copy(width = metrics.widthPixels))
        }
        if (options.video.height == -1) {
            reconfig = reconfig.copy(video = reconfig.video.copy(height = metrics.heightPixels))
        }
        return reconfig
    }

    private fun scanForOutputFile() {
        MediaScannerConnection.scanFile(activity, arrayOf(outputFile.toString()), null) { path, uri ->
            Log.i("scrcast", "scanned: $path")
            Log.i("scrcast", "-> uri=$uri")
            if (uri != null) {
                onRecordingOutput?.invoke(File(path))
            }
            _outputFile = null
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
                    if (options.moveTaskToBack) activity.moveTaskToBack(true)
                    val output = outputFile
                    if (output != null) {
                        startService(result, output)
                    }
                }
            }
        })
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
                addAction(STATE_IDLE)
                addAction(STATE_RECORDING)
                addAction(STATE_PAUSED)
                addAction(STATE_DELAY)
            }
        )

        activity.bindService(service, connection, Context.BIND_AUTO_CREATE)
        activity.startService(service)
    }

    companion object {
        /**
         * Instance creator for [ScrCast].
         *
         * Requires an [Activity] reference for media projection creation, as well
         * as auto video-sizing in [Options].
         *
         * @see [Options.video]
         */
        @JvmStatic
        fun use(activity: Activity): ScrCast {
            return ScrCast(activity)
        }
    }
}
