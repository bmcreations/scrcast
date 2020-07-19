package dev.bmcreations.scrcast.recorder

import dev.bmcreations.scrcast.ScrCast
/**
 * Callback for [RecordingState] changes during a recording session.
 *
 * @see [RecordingState]
 * @see [ScrCast.setOnStateChangeListener]
 */
interface OnRecordingStateChange {
    /**
     * Triggered when the state changes for the current recording session.
     */
    fun onStateChange(state: RecordingState)
}

/**
 * Kotlin accessible lambda for [OnRecordingStateChange], used with [ScrCast.setOnStateChangeListener]
 */
internal typealias RecordingStateChangeCallback = (RecordingState) -> Unit
