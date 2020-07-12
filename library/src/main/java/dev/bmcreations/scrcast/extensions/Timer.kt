package dev.bmcreations.scrcast.extensions

import android.os.CountDownTimer


fun Long.countdown(repeatMillis: Long = 0, onTick: (millis: Long) -> Unit, after: () -> Unit)  {
    val timer = object : CountDownTimer(this, repeatMillis) {
        override fun onFinish() {
            after()
        }

        override fun onTick(millisUntilFinished: Long) {
           onTick(millisUntilFinished)
        }

    }
    timer.start()
}
