import korlibs.time.seconds
import korlibs.korge.input.onClick
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.korge.view.solidRect
import korlibs.image.color.Colors
import korlibs.io.async.runBlockingNoJs
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.math.geom.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class KorGeTest : ViewsForTesting() {
	@Test
	fun korgeTest() {
		val testStartedExecution = korAtomic(false)
		val testWasExecuted = korAtomic(false)
		runBlockingNoJs {
			viewsTest {
				testStartedExecution.value = true

				val log = arrayListOf<String>()
				val rect = solidRect(100, 100, Colors.RED)
				rect.onClick {
					log += "clicked"
				}
				assertEquals(1, views.stage.numChildren)
				rect.simulateClick()
				assertEquals(true, rect.isVisibleToUser())
				tween(rect::x[-102], time = 10.seconds)
				assertEquals(Rectangle(x = -102, y = 0, width = 100, height = 100), rect.globalBounds)
				assertEquals(false, rect.isVisibleToUser())
				assertEquals(listOf("clicked"), log)

				testWasExecuted.value = true
			}
			waitFor("korgeTest started execution") { -> testStartedExecution.value }
			waitFor("korgeTest was executed") { -> testWasExecuted.value }
		}
	}
}