package dev.bmcreations.scrcast.config

import android.media.MediaRecorder
import android.os.Environment
import android.os.Parcelable
import android.text.format.Formatter
import kotlinx.android.parcel.Parcelize
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class Options(
    val width: Int = 1080,
    val height: Int = 1920,
    val videoEncoder: Int = MediaRecorder.VideoEncoder.H264,
    val bitrate: Int = 300_000_000,
    val frameRate: Int = 60,
    val directory: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "scrcast"),
    val fileNameFormatter: String = SimpleDateFormat("MM_dd_yyyy_hhmmss", Locale.getDefault()).format(Date()),
    val outputFormat: Int = MediaRecorder.OutputFormat.MPEG_4
): Parcelable
