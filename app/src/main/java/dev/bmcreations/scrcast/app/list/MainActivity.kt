package dev.bmcreations.scrcast.app.list


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import dev.bmcreations.scrcast.ScrCast
import dev.bmcreations.scrcast.app.R
import dev.bmcreations.scrcast.recorder.RecordingState
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val recorder: ScrCast by lazy {
        ScrCast.use(this).apply {
            options {
                video {
                    maxLengthSecs = 360
                }
                storage {
                    directoryName = "scrcast-sample"
                }
                notification {
                    title = "Super cool library"
                    description = "shh session in progress"
                    icon = resources.getDrawable(R.drawable.ic_camcorder, null).toBitmap()
                    channel {
                        id = "1337"
                        name = "Recording Service"
                    }
                    showStop = true
                    showTimer = true
                }
                moveTaskToBack = false
                stopOnScreenOff = true
                startDelayMs = 5_000
            }

            setOnStateChangeListener { state ->
                fab.reflectState(state)
                start_timer.isVisible = state is RecordingState.Delay
                when (state) {
                    is RecordingState.Delay -> start_timer.text = state.remainingSeconds.toString()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener {
            if (recorder.isRecording) {
                recorder.stopRecording()
            } else {
                recorder.record()
            }
        }
    }
}
