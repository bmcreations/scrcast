package dev.bmcreations.scrcast.recorder

sealed class Action(val name: String) {
    object Stop : Action(ACTION_STOP)
}
