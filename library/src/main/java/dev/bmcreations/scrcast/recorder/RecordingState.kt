package dev.bmcreations.scrcast.recorder

sealed class RecordingState(val action: String) {
    object Recording : RecordingState(STATE_RECORDING)
    object Idle : RecordingState(STATE_IDLE)
    object Paused : RecordingState(STATE_PAUSED)
    data class Delay(val remainingSeconds: Int): RecordingState(STATE_DELAY)

    val isRecording: Boolean get() = this == Recording
    val isIdle: Boolean get() = this == Idle
    val isPaused: Boolean get() = this == Paused
    val isInStartDelay: Boolean get() = this is Delay
}

const val STATE_RECORDING = "scrcast.internal.state.RECORDING"
const val STATE_IDLE = "scrcast.internal.state.IDLE"
const val STATE_PAUSED = "scrcast.internal.state.PAUSED"
const val STATE_DELAY = "scrcast.internal.state.DELAY"
const val EXTRA_DELAY_REMAINING = "extra_sec_remaining"
