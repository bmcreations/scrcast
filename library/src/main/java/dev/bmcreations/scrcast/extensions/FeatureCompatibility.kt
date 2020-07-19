package dev.bmcreations.scrcast.extensions

import android.media.MediaRecorder
import android.os.Build

/**
 * Whether the target device supports pause and resume operations via [MediaRecorder]
 * (e.g the device API level is [Build.VERSION_CODES.N] or higher)
 */
val supportsPauseResume = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
