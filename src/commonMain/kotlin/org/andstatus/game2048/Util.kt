package org.andstatus.game2048

import com.soywiz.klock.Stopwatch

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