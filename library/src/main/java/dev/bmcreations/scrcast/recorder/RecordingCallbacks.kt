package dev.bmcreations.scrcast.recorder

import dev.bmcreations.scrcast.ScrCast
import java.io.File

/**
 * Callback for [RecordingState] changes during a recording session.
 *
 * @see [RecordingState]
 * @see [ScrCast.setRecordingCallback]
 */
interface RecordingCallbacks {
    /**
     * Triggered when the state changes for the current recording session.
     */
    fun onStateChange(state: RecordingState)
    fun onRecordingFinished(file: File)
}

/**
 * Kotlin accessible lambda for [RecordingCallbacks], used with [ScrCast.setRecordingCallback]
 */
internal typealias RecordingStateChangeCallback = (RecordingState) -> Unit
internal typealias RecordingOutputFileCallback = (File) -> Unit
