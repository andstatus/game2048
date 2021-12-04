import com.soywiz.korio.concurrent.atomic.incrementAndGet
import com.soywiz.korio.concurrent.atomic.korAtomic
import org.andstatus.game2048.model.GameClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AtomicTest {

    @Test
    fun atomicTest() {
        val nextIdHolder = korAtomic(0)
        fun nextId(): Int = nextIdHolder.incrementAndGet()
        assertEquals(1, nextId())

        val boolVal = korAtomic(false)
        assertTrue(boolVal.compareAndSet(false,true))
        assertEquals(true, boolVal.value)

        val clock = GameClock()
        assertEquals(0, clock.playedSeconds)
        clock.start()
        assertTrue(clock.started)
    }
}