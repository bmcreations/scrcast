package dev.bmcreations.scrcast.recorder

import dev.bmcreations.scrcast.ScrCast
/**
 * Explicit defined state's during a recording session via [ScrCast]
 */
sealed class RecordingState {
    /** Defines when the session is in an active recording, non paused state */
    object Recording : RecordingState()
    /** Defines when the session is idle, either before a first session has started,
     * after a session has ended, or when an error has occurred.
     *
     * If it's idle become of an error, [error] will be non-null.
     */
    data class Idle(
        /** If this is non-null, we are Idle due to an error */
        val error: Throwable? = null
    ) : RecordingState()
    /** Defines when the session is in an active recording, but is currently paused */
    object Paused : RecordingState()
    /** Defines when the session is in a non idle, start is currently in a "start delay",
     * with the count of remaining seconds until the start is transferred to
     * either [Recording] if the session successfully starts, or to [Idle] with [Idle.error] being non-null.
     */
    data class Delay(
        /** Number of remaining seconds until the session is attempted to be started */
        val remainingSeconds: Int
    ): RecordingState()

    /**
     * Convenience state query for when the state is [Recording]
     */
    val isRecording: Boolean get() = this == Recording
    /**
     * Convenience state query for when the state is [Idle] and [Idle.error] is null
     * (e.g not in an error state)
     */
    val isIdle: Boolean get() = this is Idle && this.error == null
    /**
     * Convenience state query for when the state is [Idle] and [Idle.error] is non-null
     * (e.g in an error state)
     */
    val isError: Boolean get() = this is Idle && this.error != null
    /**
     * Convenience state query for when the state is [Paused]
     */
    val isPaused: Boolean get() = this == Paused
    /**
     * Convenience state query for when the state is [Delay]
     */
    val isInStartDelay: Boolean get() = this is Delay
}
