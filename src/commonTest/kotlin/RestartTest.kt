import korlibs.korge.tests.ViewsForTesting
import org.andstatus.game2048.model.PlayerEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RestartTest : ViewsForTesting(log = true) {

    @Test
    fun restartTest() = myViewsTest(this) {
        waitForNextPresented {
            presenter.computerMove()
        }
        waitForNextPresented {
            presenter.computerMove()
        }
        assertTrue(presenter.boardViews.blocks.size > 1, modelAndViews())
        assertEquals(presenter.model.history.currentGame.gamePlies[1].player, PlayerEnum.COMPUTER, currentGameString())
        assertTrue(presenter.model.history.currentGame.gamePlies.size > 1, currentGameString())

        waitForNextPresented {
            presenter.onTryAgainClick()
        }
        assertEquals(1, presenter.boardViews.blocks.size, "Should be one block ${modelAndViews()}")
        assertEquals(1,
            presenter.model.history.currentGame.gamePlies.lastPage.firstPlyNumber, modelAndViews())
        assertEquals( 1, presenter.model.gamePosition.pieces.count { it != null }, modelAndViews())
        assertEquals(1, presenter.model.history.currentGame.gamePlies.size, currentGameString())

        waitForNextPresented {
            presenter.computerMove()
        }
        assertEquals(2, presenter.model.gamePosition.pieces.count { it != null }, modelAndViews())
        assertEquals(2, presenter.model.history.currentGame.gamePlies.size, currentGameString())
    }
}