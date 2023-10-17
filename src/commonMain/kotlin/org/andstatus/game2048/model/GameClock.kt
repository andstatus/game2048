package org.andstatus.game2048.model

import korlibs.time.DateTimeTz
import korlibs.time.seconds
import korlibs.util.format
import org.andstatus.game2048.compareAndSetFixed
import org.andstatus.game2048.initAtomicReference

class GameClock(initialSeconds: Int = 0) {

    private class Counter(val started: Boolean, val startedAt: DateTimeTz?, val initialSeconds: Int) {
        fun current(): Int =
            if (started) {
                initialSeconds + (startedAt?.let { (DateTimeTz.nowLocal() - it).seconds.toInt() } ?: 0)
            } else {
                initialSeconds
            }
    }

    // Needed until the fix of https://github.com/korlibs/korge-next/issues/166
    private val counterRef = initAtomicReference(Counter(false, null, initialSeconds))

    fun start() {
        do {
            val counter = counterRef.value
            if (!counter.started && counterRef.compareAndSetFixed(
                    counter,
                    Counter(true, DateTimeTz.nowLocal(), counter.initialSeconds)
                )
            ) {
                break
            }
        } while (!counter.started)
    }

    fun stop(): Int {
        do {
            val counter = counterRef.value
            if (counter.started &&
                counterRef.compareAndSetFixed(counter, Counter(false, null, counter.current()))
            ) {
                break
            }
        } while (counter.started)
        return counterRef.value.initialSeconds
    }

    fun copy(): GameClock = GameClock(playedSeconds)

    val started: Boolean get() = counterRef.value.started
    val playedSeconds: Int get() = counterRef.value.current()

    val playedSecondsString: String
        get() {
            fun Int.format(): String = "%02d".format(this)

            val seconds = playedSeconds
            val secOnly: Int = seconds.rem(60)
            val minutes = (seconds - secOnly) / 60
            val minOnly: Int = minutes.rem(60)
            val hours = (minutes - minOnly) / 60
            return hours.format() + ":" + minOnly.format() + ":" + secOnly.format()
        }
}