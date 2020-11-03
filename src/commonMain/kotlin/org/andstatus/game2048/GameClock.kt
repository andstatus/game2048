package org.andstatus.game2048

import com.soywiz.klock.DateTimeTz
import com.soywiz.korio.concurrent.atomic.KorAtomicRef

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
}