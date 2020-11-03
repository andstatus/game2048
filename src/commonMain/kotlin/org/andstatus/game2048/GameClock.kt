package org.andstatus.game2048

import com.soywiz.klock.DateTimeTz
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.lang.format

class GameClock(initialSeconds: Int = 0) {

    private data class Counter(val started: Boolean, val startedAt: DateTimeTz?, val initialSeconds: Int) {
        fun current(): Int =
            if (started) {
                initialSeconds + (startedAt?.let { (DateTimeTz.nowLocal() - it).seconds.toInt() } ?: 0)
            } else {
                initialSeconds
            }
    }

    private val counterRef: KorAtomicRef<Counter> =
            KorAtomicRef(Counter(false, null, initialSeconds))

    fun start() {
        do {
            val counter = counterRef.value
            if (!counter.started && counterRef.compareAndSet(counter,
                    Counter(true, DateTimeTz.nowLocal(), counter.initialSeconds))) {
                break
            }
        } while (!counter.started)
    }

    fun stop(): Int {
        do {
            val counter = counterRef.value
            if (counter.started &&
                    counterRef.compareAndSet(counter, Counter(false, null, counter.current()))) {
                break
            }
        } while (counter.started)
        return counterRef.value.initialSeconds
    }

    val started: Boolean get() = counterRef.value.started
    val playedSeconds: Int get() = counterRef.value.current()

    val playedSecondsString: String get() {
        fun Int.format() = "%02d".format(this)

        val seconds = playedSeconds
        val sec: Int = seconds.rem(60)
        val min: Int = ((seconds - sec) / 60).rem(60)
        val hours: Int = (seconds - sec - (min * 60)) / 60
        return hours.format() + ":" + min.format() + ":" + sec.format()
    }
}