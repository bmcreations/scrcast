package dev.bmcreations.scrcast.internal.recorder

import androidx.annotation.RestrictTo
import dev.bmcreations.scrcast.recorder.RecordingState

@RestrictTo(RestrictTo.Scope.LIBRARY)
const val STATE_RECORDING = "scrcast.internal.state.RECORDING"

@RestrictTo(RestrictTo.Scope.LIBRARY)
const val STATE_IDLE = "scrcast.internal.state.IDLE"

@RestrictTo(RestrictTo.Scope.LIBRARY)
const val STATE_PAUSED = "scrcast.internal.state.PAUSED"

@RestrictTo(RestrictTo.Scope.LIBRARY)
const val STATE_DELAY = "scrcast.internal.state.DELAY"

@RestrictTo(RestrictTo.Scope.LIBRARY)
const val EXTRA_DELAY_REMAINING = "extra_sec_remaining"

@RestrictTo(RestrictTo.Scope.LIBRARY)
const val EXTRA_ERROR = "extra_error"

fun RecordingState.stateString(): String {
    return when (this) {
        RecordingState.Recording -> STATE_RECORDING
        is RecordingState.Idle -> STATE_IDLE
        RecordingState.Paused -> STATE_PAUSED
        is RecordingState.Delay -> STATE_DELAY
    }
}
