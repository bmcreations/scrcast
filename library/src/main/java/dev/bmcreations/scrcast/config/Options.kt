package dev.bmcreations.scrcast.config

import android.media.MediaRecorder
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Options(
    val width: Int = 1080,
    val height: Int = 1920,
    val videoEncoder: Int = MediaRecorder.VideoEncoder.H264,
    val bitrate: Int = 300_000_000,
    val frameRate: Int = 60,
    val directory: String = "scrcast",
    val outputFormat: Int = MediaRecorder.OutputFormat.MPEG_4
): Parcelable
