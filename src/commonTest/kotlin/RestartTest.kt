import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korio.concurrent.atomic.korAtomic
import org.andstatus.game2048.model.PlayerEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RestartTest : ViewsForTesting(log = true) {

    @Test
    fun restartTest() {
        val testWasExecuted = korAtomic(false)
        viewsTest {
            unsetGameView()
            initializeViewDataInTest {
                waitForMainViewShown {
                    presenter.computerMove()
                }
                waitForMainViewShown {
                    presenter.computerMove()
                }
                assertTrue(presenter.boardViews.blocks.size > 1, modelAndViews())
                assertEquals(presenter.model.history.currentGame.gamePlies[1].player, PlayerEnum.COMPUTER, currentGameString())
                assertTrue(presenter.model.history.currentGame.gamePlies.size > 1, currentGameString())

                waitForMainViewShown {
                    presenter.onRestartClick()
                }
                assertEquals(1, presenter.boardViews.blocks.size, modelAndViews())
                assertEquals(1, presenter.model.history.currentGame.gamePlies.lastPage.firstPlyNumber, modelAndViews())
                assertEquals( 1, presenter.model.gamePosition.pieces.count { it != null }, modelAndViews())
                assertEquals(1, presenter.model.history.currentGame.gamePlies.size, currentGameString())

                waitForMainViewShown {
                    presenter.computerMove()
                }
                assertEquals(2, presenter.model.gamePosition.pieces.count { it != null }, modelAndViews())
                assertEquals(2, presenter.model.history.currentGame.gamePlies.size, currentGameString())

                testWasExecuted.value = true
            }
        }
        waitFor("restartTest was executed") { -> testWasExecuted.value }
    }
}