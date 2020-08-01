package dev.bmcreations.scrcast.app.list

import android.os.Bundle
import androidx.lifecycle.Observer
import dev.bmcreations.scrcast.lifecycle.observe

class StateObserverActivity : MainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recorder.observe(this, Observer { state -> handleRecorderState(state) })
    }
}
