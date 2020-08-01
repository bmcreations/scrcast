package dev.bmcreations.scrcast.app.list.jvm;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import dev.bmcreations.scrcast.ScrCast;
import dev.bmcreations.scrcast.app.R;
import dev.bmcreations.scrcast.app.list.FABExtensions;
import dev.bmcreations.scrcast.config.ChannelConfig;
import dev.bmcreations.scrcast.config.Options;
import dev.bmcreations.scrcast.config.StorageConfig;
import dev.bmcreations.scrcast.config.VideoConfig;
import dev.bmcreations.scrcast.internal.config.dsl.NotificationConfigBuilder;
import dev.bmcreations.scrcast.lifecycle.ScrCastLifecycleObserver;
import dev.bmcreations.scrcast.recorder.RecordingCallbacks;
import dev.bmcreations.scrcast.recorder.RecordingState;

public class JavaMainActivity extends AppCompatActivity {

    private FloatingActionButton fab;
    private MaterialTextView startTimer;
    private ScrCast recorder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fab = findViewById(R.id.fab);
        startTimer = findViewById(R.id.start_timer);

        setupRecorder();

        bindViews();
    }

    private void setupRecorder() {
        recorder = ScrCast.use(this);

        // create configuration for video
        VideoConfig videoConfig = new VideoConfig(
                -1,
                -1,
                MediaRecorder.VideoEncoder.H264,
                8_000_000,
                360
        );

        // create configuration for storage
        StorageConfig storageConfig = new StorageConfig("scrcast-sample");

        // create configuration for notification channel for recording
        ChannelConfig channelConfig = new ChannelConfig("1337", "Recording Service");

        // create configuration for our notification
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_camcorder);
        NotificationConfigBuilder  notificationConfig = new NotificationConfigBuilder();
        notificationConfig.setShowPause(true);
        notificationConfig.setIcon(drawableToBitmap(icon));
        notificationConfig.setShowStop(true);
        notificationConfig.setShowTimer(true);
        notificationConfig.setChannel(channelConfig);

        Options options = new Options(
                videoConfig,
                storageConfig,
                notificationConfig.build(),
                false,
                5000,
                true
        );

        // set our options
        recorder.updateOptions(options);

        // listen for state changes
        recorder.setRecordingCallback(new RecordingCallbacks() {
            @Override
            public void onStateChange(@NotNull RecordingState state) {
                FABExtensions.reflectRecorderState(fab, state);
                startTimer.setVisibility(state instanceof RecordingState.Delay ? View.VISIBLE : View.GONE);
                if (state instanceof RecordingState.Delay) {
                    startTimer.setText(((RecordingState.Delay) state).getRemainingSeconds());
                }
            }

            @Override
            public void onRecordingFinished(@NotNull File file) {
                Toast.makeText(
                        JavaMainActivity.this,
                        "result file is located at " + file.getAbsolutePath(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void bindViews() {
        fab.setOnClickListener(v -> {
            if (recorder.getState().isRecording()) {
                recorder.stopRecording();
            } else {
                recorder.record();
            }
        });
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
