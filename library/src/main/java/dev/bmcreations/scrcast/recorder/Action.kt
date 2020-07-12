package dev.bmcreations.scrcast.recorder

sealed class Action(val name: String) {
    object Pause : Action(ACTION_PAUSE)
    object Resume : Action(ACTION_RESUME)
    object Stop : Action(ACTION_STOP)
}

const val ACTION_PAUSE = "scrcast.internal.action.PAUSE"
const val ACTION_RESUME = "scrcast.internal.action.RESUME"
const val ACTION_STOP = "scrcast.internal.action.STOP"
