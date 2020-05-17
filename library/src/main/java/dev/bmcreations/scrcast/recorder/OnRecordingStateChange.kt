package dev.bmcreations.scrcast.recorder

interface OnRecordingStateChange {
    fun onStateChange(recording: Boolean)
}

internal typealias RecordingStateChangeCallback = (Boolean) -> Unit
