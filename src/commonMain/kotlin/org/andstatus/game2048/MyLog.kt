package org.andstatus.game2048

import com.soywiz.klock.Stopwatch
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korio.lang.currentThreadId

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
