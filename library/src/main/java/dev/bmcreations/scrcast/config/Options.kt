@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package dev.bmcreations.scrcast.config

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.util.DisplayMetrics
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import kotlin.jvm.functions.Function0
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import dev.bmcreations.scrcast.ScrCast
import dev.bmcreations.scrcast.recorder.RecordingState
import dev.bmcreations.scrcast.recorder.OnRecordingStateChange
import kotlinx.android.parcel.IgnoredOnParcel

typealias FileFormatter = () -> String

/**
 * An immutable data object representing the Available configuration options for
 * storage, recording, and providing user interaction with the screen recording.
 *
 * @see [ScrCast.options]
 */
@Parcelize
data class Options @JvmOverloads constructor(
    /** @see [VideoConfig] */
    val video: VideoConfig = VideoConfig(),
    /** @see [StorageConfig] */
    val storage: StorageConfig = StorageConfig(),
    /** @see [NotificationConfig] */
    val notification: NotificationConfig = NotificationConfig(),
    /**
     * If the activity task should be moved the back once video starts recording.
     * Defaults to false.
     */
    val moveTaskToBack: Boolean = false,
    /**
     * The time (in milliseconds) to delay the recording after calling [ScrCast.record].
     *
     * Will emit [RecordingState.Delay] with a countdown from [staertDelayMs] down to zero (in seconds).
     *
     * @see [RecordingState]
     * @see [OnRecordingStateChange]
     */
    val startDelayMs: Long = 0,
    /**
     * If enabled, any current in-progress recording session will be ending once the device screen
     * is turned off.
     */
    val stopOnScreenOff: Boolean = false
): Parcelable

/**
 * An immutable data class representing configuration options for the recording.
 */
@Parcelize
data class VideoConfig @JvmOverloads constructor(
    /**
     * Width of the video recording frame.
     *
     * A value of [-1] will allow [ScrCast] to query the [DisplayMetrics.widthPixels] from the device for use.
     *
     * @see [MediaRecorder.setVideoSize]
     */
    val width: Int = -1,
    /**
     * Height of the video recording frame.
     *
     * A value of [-1] will allow [ScrCast] to query the [DisplayMetrics.heightPixels] from the device for use.
     *
     * @see [MediaRecorder.setVideoSize]
     */
    val height: Int = -1,
    /**
     * Defines the video encoding to be used for the recording.
     *
     * @see [MediaRecorder.VideoEncoder]
     * @see [MediaRecorder.setVideoEncoder]
     *
     * A value of [-1] will allow [ScrCast] to query the [DisplayMetrics.widthPixels] from the device for use.
     *
     * @see [MediaRecorder.setVideoSize]
     */
    val videoEncoder: Int = MediaRecorder.VideoEncoder.H264,
    /**
     * Defines the video bitrate to be used for the recording.
     *
     * A higher bitrate will result in a better quality recording, but will also result in a larger
     * output file.
     *
     * @see [MediaRecorder.setVideoEncodingBitRate]
     */
    val bitrate: Int = 8_000_000,
    /**
     * Defines the video frame rate to be used for the recording.
     *
     * A higher frame rate will result in a smoother recording, but will also result in a larger
     * output file.
     *
     * @see [MediaRecorder.setVideoFrameRate]
     */
    val frameRate: Int = 60,
    /**
     * Defines the maximum length of time (in seconds) desired for the recording. If the recording session hits this defined
     * time limit, the recording will auto end.
     *
     * @see [MediaRecorder.setMaxDuration]
     */
    val maxLengthSecs: Int = 0
): Parcelable

/**
 * An immutable data class representing configuration options for the storage of the output file.
 */
@Parcelize
data class StorageConfig @JvmOverloads constructor(
    /**
     * The directory name for this recording to be stored in.
     * Will end up being located at [mediaStorageLocation]/[fileNameFormatter]
     *
     *  @see [mediaStorageLocation]
     *  @see [fileNameFormatter]
     */
    val directoryName: String = "scrcast",
    /**
     * The parent directory for all storage operations
     * Will end up being located at [mediaStorageLocation]/[fileNameFormatter]
     *
     *  @see [mediaStorageLocation]
     *  @see [fileNameFormatter]
     */
    val directory: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
    /**
     * the formatting of the file name for the resulting screen recording. This is done via a higher order lambda
     * in Kotlin and a [Function0] to ensure the formatter will run for each recording independently of one another.
     *
     * Defaults to the current time in `MM_dd_yyyy_hhmmss` format.
     *
     * Will end up being located at [mediaStorageLocation]/[fileNameFormatter]
     *
     *  @see [mediaStorageLocation]
     */
    val fileNameFormatter: @RawValue FileFormatter = { SimpleDateFormat("MM_dd_yyyy_hhmmss", Locale.getDefault()).format(Date()) },
    /**
     * The resulting video format of the screen recording file.
     *
     * @see [MediaRecorder.OutputFormat]
     */
    val outputFormat: Int = MediaRecorder.OutputFormat.MPEG_4,
    /**
     * Defines the maximum size  (in MB) desired for the recording. If the recording session hits this defined
     * size limit, the recording will auto end.
     *
     * @see [MediaRecorder.setMaxFileSize]
     */
    val maxSizeMB: Float = 0f
): Parcelable {
    /**
     * The resulting directory location of the screen recordings, derived from [directory] and [directoryName]
     *
     * @see [directory]
     * @see [directoryName]
     */
    @IgnoredOnParcel
    val mediaStorageLocation = File(directory, directoryName)
}

/**
 * An immutable data class representing configuration options for the
 * notification presented from the foreground service.
 */
@Parcelize
data class NotificationConfig @JvmOverloads constructor(
    /**
     * The title displayed in the notification
     *
     * @see [Notification.Builder.setContentTitle]
     */
    val title: String = "scrcast",
    /**
     * The message/description displayed in the notification
     *
     * @see [Notification.Builder.setContentText]
     */
    val description: String = "Recording in progress...",
    /**
     * The icon displayed in the notification
     *
     * @see [Notification.Builder.setSmallIcon]
     */
    val icon: Bitmap? = null,
    /**
     * The unique identifier for the displayed notification
     *
     * @see [NotificationManager.notify]
     */
    val id: Int = 101,
    /**
     * Whether to add a notification action for stopping the current recording.
     */
    val showStop: Boolean = false,
    /**
     * Whether to add a notification action for pause/resume of the current recording (visible action
     * is dependent on the state of the current recording session).
     *
     * NOTE: This option will only make an effect on [Build.VERSION_CODES.N] and above
     *
     */
    val showPause: Boolean = false,
    /**
     * Whether to add a the current recording time to the notification via a chronometer.
     *
     * @see [Notification.Builder.setUsesChronometer]
     */
    val showTimer: Boolean = false,
    /** @see [ChannelConfig] */
    val channel: ChannelConfig = ChannelConfig()
): Parcelable

/**
 * An immutable data class representing configuration options for the
 * notification channel of the recording notifications.
 *
 * @see [NotificationChannel]
 */
@Parcelize
data class ChannelConfig @JvmOverloads constructor(
    /**
     * The unique identifier for the notification channel
     *
     * @see [NotificationChannel]
     */
    val id: String = "1337",
    /**
     * The name for the notification channel
     *
     * @see [NotificationChannel]
     */
    val name: String = "Recording Service",
    /**
     * The LED color to used when this notification presents itself
     *
     * @see [NotificationChannel]
     */
    val lightColor: Int = Color.BLUE,
    /**
     * The visibility restrictions for the notifications in this channel.
     *
     * @see [NotificationChannel]
     * @see [Notification.VISIBILITY_PRIVATE]
     * @see [Notification.VISIBILITY_PUBLIC]
     * @see [Notification.VISIBILITY_SECRET]
     */
    val lockscreenVisibility: Int = Notification.VISIBILITY_PRIVATE
): Parcelable
