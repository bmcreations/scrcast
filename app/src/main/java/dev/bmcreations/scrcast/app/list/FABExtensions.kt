package dev.bmcreations.scrcast.app.list

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.bmcreations.scrcast.app.R
import dev.bmcreations.scrcast.recorder.RecordingState
import dev.bmcreations.scrcast.recorder.RecordingState.Idle
import dev.bmcreations.scrcast.recorder.RecordingState.Recording

@SuppressLint("ObjectAnimatorBinding")
private fun FloatingActionButton.animateColorChange(@ColorInt fromColor: Int, @ColorInt toColor: Int, startDelay: Long = 0) {
    val colorAnimator = ObjectAnimator.ofArgb(
        this,
        "backgroundTintColor",
        fromColor, toColor
    ).apply {
        setStartDelay(startDelay)
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            backgroundTintList = ColorStateList.valueOf(animatedValue)
        }
    }

    colorAnimator.start()
}

fun FloatingActionButton.reflectState(state: RecordingState) {
    if (state == Recording || state is Idle) {
        val isRecording = state == Recording
        setImageResource(if (isRecording) R.drawable.ic_stop else R.drawable.ic_camcorder)
        animateColorChange(
            if (isRecording) ContextCompat.getColor(
                context,
                R.color.teal200
            ) else ContextCompat.getColor(context, R.color.stop_recording),
            if (isRecording) ContextCompat.getColor(
                context,
                R.color.stop_recording
            ) else ContextCompat.getColor(context, R.color.teal200)
        )
    }
}

class FABExtensions {
    companion object {
        @JvmStatic
        fun reflectRecorderState(fab: FloatingActionButton, state: RecordingState) {
            fab.reflectState(state)
        }
    }
}
