package dev.bmcreations.scrcast.app.list


import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import dev.bmcreations.scrcast.ScrCast
import dev.bmcreations.scrcast.app.R
import dev.bmcreations.scrcast.extensions.supportsPauseResume
import dev.bmcreations.scrcast.recorder.RecordingState
import kotlinx.android.synthetic.main.activity_main.*


abstract class MainActivity : AppCompatActivity() {

    protected val recorder: ScrCast by lazy {
        ScrCast.use(this).apply {
            options {
                video {
                    maxLengthSecs = 360
                }
                storage {
                    directoryName = "scrcast-sample"
                }
                notification {
                    icon = resources.getDrawable(R.drawable.ic_camcorder, null).toBitmap()
                    channel {
                        id = "1337"
                        name = "Recording Service"
                    }
                    showStop = true
                    showPause = true
                    showTimer = true
                }
                moveTaskToBack = false
                stopOnScreenOff = true
                startDelayMs = 5_000
            }

            // ScrCast supports running your own notifications completely
            // simply provide a NotificationProvider
            //
            //setNotificationProvider(SimpleNotificationProvider(this@MainActivity))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pause_fab.hide()
        pause_fab.setOnClickListener {
            if (recorder.state.isPaused) {
                recorder.resume()
            } else {
                recorder.pause()
            }
        }

        fab.setOnClickListener {
            if (recorder.state.isRecording) {
                recorder.stopRecording()
            } else {
                recorder.record()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        recorder.onRecordingComplete { file ->
            Snackbar.make(bottom_bar, "Recording located at ${file.absolutePath}", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        recorder.onRecordingComplete { }
    }

    protected fun handleRecorderState(state: RecordingState) {
        Log.d("sample", "state change: state = $state")
        fab.reflectState(state)

        start_timer.isVisible = state.isInStartDelay
        if (supportsPauseResume) {
            if (state.isRecording || state.isPaused) {
                pause_fab.show()
            } else {
                pause_fab.hide()
            }
        }

        when (state) {
            is RecordingState.Delay -> start_timer.text = state.remainingSeconds.toString()
            RecordingState.Recording -> {
                pause_fab.setIconResource(R.drawable.ic_pause)
                pause_fab.text = "Pause"
            }
            is RecordingState.Idle -> {
                fab.isExpanded = false
                if (state.error != null) {
                    Toast.makeText(this, "Unable to start recording", Toast.LENGTH_SHORT).show()
                }
            }
            RecordingState.Paused -> {
                pause_fab.setIconResource(R.drawable.ic_resume)
                pause_fab.text = "Resume"
            }
        }
    }
}
