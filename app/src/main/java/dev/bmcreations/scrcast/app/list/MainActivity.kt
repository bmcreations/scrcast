package dev.bmcreations.scrcast.app.list


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import dev.bmcreations.scrcast.ScrCast
import dev.bmcreations.scrcast.app.R
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

            setOnStateChangeListener { recording ->
                fab.reflectState(recording)
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
