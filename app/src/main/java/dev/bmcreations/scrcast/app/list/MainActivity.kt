package dev.bmcreations.scrcast.app.list


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import dev.bmcreations.scrcast.ScrCast
import dev.bmcreations.scrcast.app.R
import dev.bmcreations.scrcast.config.ChannelConfig
import dev.bmcreations.scrcast.config.NotificationConfig
import dev.bmcreations.scrcast.config.StorageConfig
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val recorder: ScrCast by lazy {
        ScrCast.use(this).apply {
            updateOptions {
                copy(
                    storage = StorageConfig(
                        directoryName = "scrcast-sample"
                    ),
                    notification = NotificationConfig(
                        title = "Super cool library",
                        description = "shh session in progress",
                        icon = resources.getDrawable(R.drawable.ic_camcorder, null).toBitmap(),
                        channel = ChannelConfig(
                            id = "1337",
                            name = "Recording Service"
                        ),
                        showStop = true,
                        showTimer = true
                    ),
                    moveTaskToBack = false
                )
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
