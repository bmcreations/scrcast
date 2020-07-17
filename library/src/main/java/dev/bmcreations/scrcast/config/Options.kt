package dev.bmcreations.scrcast.config

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.util.DisplayMetrics
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

typealias FileFormatter = () -> String

@Parcelize
data class Options @JvmOverloads constructor(
    val video: VideoConfig = VideoConfig(),
    val storage: StorageConfig = StorageConfig(),
    val notification: NotificationConfig = NotificationConfig(),
    val moveTaskToBack: Boolean = false,
    val startDelayMs: Long = 0,
    val stopOnScreenOff: Boolean = false
): Parcelable

@Parcelize
data class VideoConfig @JvmOverloads constructor(
    val width: Int = -1,
    val height: Int = -1,
    val videoEncoder: Int = MediaRecorder.VideoEncoder.H264,
    val bitrate: Int = 8_000_000,
    val frameRate: Int = 60,
    val maxLengthSecs: Int = 0
): Parcelable

@Parcelize
data class StorageConfig @JvmOverloads constructor(
    val directoryName: String = "scrcast",
    val directory: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
    val fileNameFormatter: @RawValue FileFormatter = { SimpleDateFormat("MM_dd_yyyy_hhmmss", Locale.getDefault()).format(Date()) },
    val outputFormat: Int = MediaRecorder.OutputFormat.MPEG_4,
    val maxSizeMB: Float = 0f
): Parcelable {
    val mediaStorageLocation = File(directory, directoryName)
}

@Parcelize
data class NotificationConfig @JvmOverloads constructor(
    val title: String = "scrcast",
    val description: String = "Recording in progress...",
    val icon: Bitmap? = null,
    val id: Int = 101,
    val showStop: Boolean = false,
    val showPause: Boolean = false,
    val showTimer: Boolean = false,
    val channel: ChannelConfig = ChannelConfig()
): Parcelable

@Parcelize
data class ChannelConfig @JvmOverloads constructor(
    val id: String = "1337",
    val name: String = "Recording Service",
    val lightColor: Int = Color.BLUE,
    val lockscreenVisibility: Int = Notification.VISIBILITY_PRIVATE
): Parcelable
