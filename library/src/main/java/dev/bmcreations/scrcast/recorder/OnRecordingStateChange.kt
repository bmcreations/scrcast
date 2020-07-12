package dev.bmcreations.scrcast.recorder

interface OnRecordingStateChange {
    fun onStateChange(state: RecordingState)
}

internal typealias RecordingStateChangeCallback = (RecordingState) -> Unit
