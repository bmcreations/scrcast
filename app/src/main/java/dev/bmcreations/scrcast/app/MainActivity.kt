package dev.bmcreations.scrcast.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.bmcreations.scrcast.ScrCast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var recording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recorder = ScrCast.use(this).apply {

        }

        fab.setOnClickListener {
            if (recording) {
                recorder.stopRecording()
            } else {
                recorder.record()
            }
            recording = !recording
        }
    }
}
