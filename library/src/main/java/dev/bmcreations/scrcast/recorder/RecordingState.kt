package dev.bmcreations.scrcast.recorder

sealed class RecordingState(val action: String) {
    object Recording : RecordingState(STATE_RECORDING)
    object Idle : RecordingState(STATE_IDLE)
    data class Delay(val remainingSeconds: Int): RecordingState(STATE_DELAY)
}

const val STATE_RECORDING = "scrcast.internal.state.RECORDING"
const val STATE_IDLE = "scrcast.internal.state.IDLE"
const val STATE_DELAY = "scrcast.internal.state.DELAY"
const val EXTRA_DELAY_REMAINING = "extra_sec_remaining"

const val ACTION_STOP = "scrcast.internal.action.STOP"
