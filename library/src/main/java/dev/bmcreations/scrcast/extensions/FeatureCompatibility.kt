package dev.bmcreations.scrcast.extensions

import android.os.Build
import androidx.annotation.RequiresApi

val supportsPauseResume = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
