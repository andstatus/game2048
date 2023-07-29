import korlibs.korge.tests.ViewsForTesting
import kotlinx.coroutines.delay
import org.andstatus.game2048.model.PlyAndPosition.Companion.allowedRandomPly
import kotlin.test.Test
import kotlin.test.assertEquals

class RetriesTest : ViewsForTesting(log = true) {

    @Test
    fun retriesTest() = myViewsTest(this) {
        val game1 = generateGame(7, 3)
        assertEquals(1, game1.shortRecord.bookmarks.size, currentGameString())

        waitForNextPresented {
            presenter.onUndoClick()
        }
        waitForNextPresented {
            presenter.onUndoClick()
        }
        val positionAfterUndos = presenter.model.gamePosition.copy()

        allowedRandomPly(presenter.model.gamePosition).ply.plyEnum.swipeDirection?.let {
            waitForNextPresented {
                delay(2000)
                presenter.onSwipe(it)
            }
        }
        assertEquals(1, presenter.model.gamePosition.retries, currentGameString())

        waitForNextPresented {
            presenter.onUndoClick()
        }
        presenter.model.history.currentGame.gamePlies.get(presenter.model.history.redoPlyPointer).let { redoPly ->
            assertEquals(
                1, redoPly.retries, "redoPly: " +
                        "$redoPly, " + currentGameString()
            )
        }
        assertEquals(0, presenter.model.gamePosition.retries, currentGameString())

        waitForNextPresented {
            presenter.onRedoClick()
        }
        assertEquals(1, presenter.model.gamePosition.retries, currentGameString())

        waitForNextPresented {
            presenter.onUndoClick()
        }
        assertEquals(0, presenter.model.gamePosition.retries, currentGameString())
        // TODO: Time is different.
        //assertEquals(positionAfterUndos.toString(), presenter.model.gamePosition.toString(), currentGameString())
    }

}