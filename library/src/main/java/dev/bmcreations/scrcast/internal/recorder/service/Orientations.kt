package dev.bmcreations.scrcast.internal.recorder.service

import android.util.SparseIntArray
import android.view.Surface
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
val orientations = SparseIntArray().apply {
    append(Surface.ROTATION_0, 90)
    append(Surface.ROTATION_90, 0)
    append(Surface.ROTATION_180, 270)
    append(Surface.ROTATION_270, 180)
}


