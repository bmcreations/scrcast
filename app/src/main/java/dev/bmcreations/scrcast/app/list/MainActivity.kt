package dev.bmcreations.scrcast.app.list


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.bmcreations.scrcast.ScrCast
import dev.bmcreations.scrcast.app.R
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    private val recorder: ScrCast by lazy {
        ScrCast.use(this).apply {
            setOnStateChangeListener { recording ->
                fab.reflectState(recording)
            }
        }
    }

    private val watcher: FileWatcher? by lazy {
        recorder.outputDirectory?.watch(::fileWatcher)
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

    override fun onResume() {
        super.onResume()
        if (watcher?.isWatching == false) {
            watcher?.watch(::fileWatcher)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        watcher?.close()
    }

    private fun fileWatcher(files: List<File>) {
        Log.d("Main", "files=${files.count()}")
    }

    private fun FloatingActionButton.reflectState(recording: Boolean) {
        setImageResource(if (recording) R.drawable.ic_stop else R.drawable.ic_camcorder)
    }
}
