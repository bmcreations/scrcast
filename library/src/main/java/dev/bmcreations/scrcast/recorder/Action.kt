package dev.bmcreations.scrcast.recorder

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.bmcreations.scrcast.R

sealed class Action(
    val name: String,
    val requestId: Int,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    object Pause : Action(ACTION_PAUSE, 1, R.string.pause, R.drawable.ic_pause)
    object Resume : Action(ACTION_RESUME, 0, R.string.resume, R.drawable.ic_resume)
    object Stop : Action(ACTION_STOP, 2, R.string.stop, R.drawable.ic_stop)
}

const val ACTION_PAUSE = "scrcast.internal.action.PAUSE"
const val ACTION_RESUME = "scrcast.internal.action.RESUME"
const val ACTION_STOP = "scrcast.internal.action.STOP"
