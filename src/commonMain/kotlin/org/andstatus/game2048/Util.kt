package org.andstatus.game2048

import korlibs.time.Stopwatch
import korlibs.logger.Console
import korlibs.io.concurrent.atomic.KorAtomicRef
import korlibs.io.lang.substr
import org.andstatus.game2048.presenter.Presenter

private const val platformSourceFolder = "jvmMain"

inline fun <T> Iterable<T>.meanBy(selector: (T) -> Int): Int {
    var sum = 0L
    var count = 0
    for (element in this) {
        count++
        sum += selector(element)
    }
    return (if (count == 0) sum else sum / count).toInt()
}

fun timeIsUp(seconds: Int): () -> Boolean {
    val stopwatch: Stopwatch = Stopwatch().start()
    return { stopwatch.elapsed.seconds >= seconds }
}

fun <T> KorAtomicRef<T>.update(updater: (T) -> T): T {
    var newValue: T
    do {
        val oldValue = value
        newValue = updater(oldValue)
    } while (!this.compareAndSetFixed(oldValue, newValue))
    return newValue
}

fun Presenter.shareTextCommon(actionTitle: String, fileName: String, value: Sequence<String>) {
    Console.log("---- $platformSourceFolder, shareText '$fileName' start")
    Console.log("Title: $actionTitle")
    value.forEach { line ->
        Console.log(line)
    }
    Console.log("---- $platformSourceFolder, shareText '$fileName' end")
    asyncShowMainView()
}

fun String.appendToFileName(suffix: String): String {
    val dot = lastIndexOf('.')
    return if (dot < 0) {
        this + suffix
    } else {
        this.substr(0, dot) + suffix + this.substr(dot)
    }
}
