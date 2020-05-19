package dev.bmcreations.scrcast.config

import android.media.MediaRecorder
import android.os.Environment
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class Options(
    val video: VideoConfig = VideoConfig(),
    val storage: StorageConfig = StorageConfig(),
    val moveTaskToBack: Boolean = false
): Parcelable

@Parcelize
data class VideoConfig(
    val width: Int = 1080,
    val height: Int = 1920,
    val videoEncoder: Int = MediaRecorder.VideoEncoder.H264,
    val bitrate: Int = 300_000_000,
    val frameRate: Int = 60,
    val maxLengthSecs: Int = 0
): Parcelable

@Parcelize
data class StorageConfig(
    val directoryName: String = "scrcast",
    val directory: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
    val fileNameFormatter: String = SimpleDateFormat("MM_dd_yyyy_hhmmss", Locale.getDefault()).format(Date()),
    val outputFormat: Int = MediaRecorder.OutputFormat.MPEG_4,
    val maxSizeMB: Float = 0f
): Parcelable {
    val mediaStorageLocation = File(directory, directoryName)
}

