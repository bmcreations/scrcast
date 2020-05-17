package dev.bmcreations.scrcast.app.list


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.bmcreations.scrcast.ScrCast
import dev.bmcreations.scrcast.app.R
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val recorder: ScrCast by lazy {
        ScrCast.use(this).apply {
            updateOptions { copy(directory = "scrcast-sample") }
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

    private fun FloatingActionButton.reflectState(recording: Boolean) {
        setImageResource(if (recording) R.drawable.ic_stop else R.drawable.ic_camcorder)
    }
}
