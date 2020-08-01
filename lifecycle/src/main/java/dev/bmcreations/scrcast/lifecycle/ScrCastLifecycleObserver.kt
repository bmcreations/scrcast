package dev.bmcreations.scrcast.lifecycle

import androidx.lifecycle.*
import dev.bmcreations.scrcast.ScrCast
import dev.bmcreations.scrcast.recorder.RecordingState

private class LiveEvent constructor(
    private val recorder: ScrCast,
    private val lifecycleOwner: LifecycleOwner,
    private val observer: Observer<RecordingState>
) : LifecycleObserver {
    init {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            lifecycleOwner.lifecycle.addObserver(this)
        }
    }

    private var isActive: Boolean = false

    private fun shouldBeActive(): Boolean {
        return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    private fun disposeObserver() {
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            stopListening()
            disposeObserver()
            return
        }
        checkIfActiveStateChanged(shouldBeActive())
    }

    private fun checkIfActiveStateChanged(newActive: Boolean) {
        if (newActive == isActive) {
            return
        }
        val wasActive = isActive
        isActive = newActive
        val isActive = isActive

        if (!wasActive && isActive) {
            stopListening()
            recorder.onRecordingStateChange { observer.onChanged(it)
            }
        }

        if (wasActive && !isActive) {
            stopListening()
        }
    }

    private fun stopListening() {
        recorder.onRecordingStateChange { }
    }
}

/**
 * Lifecycle observer for [RecordingState] using AndroidX lifecycle components
 * @see [LifecycleObserver]
 */
fun ScrCast.observeRecordingState(lifecycleOwner: LifecycleOwner, observer: Observer<RecordingState>) {
    LiveEvent(this, lifecycleOwner, Observer { observer.onChanged(it) })
}


class ScrCastLifecycleObserver  {
    companion object {
        /**
         * JVM accessible lifecycle observer for [RecordingState] using AndroidX lifecycle components
         * @see [LifecycleObserver]
         */
        @JvmStatic
        fun observeRecordingState(recorder: ScrCast, lifecycleOwner: LifecycleOwner, observer: Observer<RecordingState>) {
            recorder.observeRecordingState(lifecycleOwner, observer)
        }
    }
}

