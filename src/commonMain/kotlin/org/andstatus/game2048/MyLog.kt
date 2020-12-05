package org.andstatus.game2048

import com.soywiz.klock.Stopwatch
import com.soywiz.korio.lang.currentThreadId

val gameStopWatch = Stopwatch().start()

fun myLog(message: Any?) = println("game2048.log ${gameStopWatch.elapsed.milliseconds.toInt()} [${currentThreadId}] $message")

inline fun myMeasured(message: Any?, measuredAction: () -> Unit)  {
    Stopwatch().start().run {
        measuredAction()
        myLog("$message in ${this.elapsed.milliseconds.toInt()} ms")
    }
}
