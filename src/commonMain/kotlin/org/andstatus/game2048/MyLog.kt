package org.andstatus.game2048

import korlibs.time.Stopwatch
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.io.lang.currentThreadId

val gameStopWatch = Stopwatch().start()
val gameIsLoading = korAtomic(false)

fun myLog(message: Any?) = println("game2048.log ${gameStopWatch.elapsed.milliseconds.toInt()} [${currentThreadId}] $message")

inline fun <T> myMeasured(message: Any?, measuredAction: () -> T): T =
    Stopwatch().start().let { stopWatch ->
        measuredAction().also {
            myLog("$message in ${stopWatch.elapsed.milliseconds.toInt()} ms")
        }
    }

inline fun <T> myMeasuredIt(message: Any?, measuredAction: () -> T): T =
    Stopwatch().start().let { stopWatch ->
        measuredAction().also {
            myLog("$message in ${stopWatch.elapsed.milliseconds.toInt()} ms: $it")
        }
    }
