package org.andstatus.game2048

import com.soywiz.klock.Stopwatch
import com.soywiz.korio.concurrent.atomic.KorAtomicRef

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
