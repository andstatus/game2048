import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korio.concurrent.atomic.korAtomic
import org.andstatus.game2048.model.GameRecord
import org.andstatus.game2048.view.ViewData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HistoryTest : ViewsForTesting(log = true) {

    @Test
    fun historyTest() {
        val testWasExecuted = korAtomic(false)
        viewsTest {
            initializeViewDataInTest {

                val game1 = generateGame(7, 3)
                assertEquals(1, game1.shortRecord.bookmarks.size, currentGameString())

                if (presenter.model.history.recentGames.none { it.id != game1.id }) {
                    generateGame(5)
                }

                assertTestHistory(game1)
                testWasExecuted.value = true
            }
        }
        waitFor("historyTest was executed") { testWasExecuted.value }
    }

    private fun ViewData.assertTestHistory(expected: GameRecord) {
        val actualGame = presenter.model.history.openGame(expected.id)?.load()
        assertNotNull(actualGame, currentGameString())
        assertEquals(expected.toLongString(), actualGame.toLongString(), currentGameString())
        assertEquals(expected.score, actualGame.score, currentGameString())
        assertEquals(expected.shortRecord.bookmarks.size, actualGame.shortRecord.bookmarks.size, currentGameString())
        assertEquals(expected.toSharedJson(), actualGame.toSharedJson(), currentGameString())
        assertTrue(presenter.canUndo(), modelAndViews() + "\n" + currentGameString())
        assertFalse(presenter.canRedo(), modelAndViews() + "\n" + currentGameString())
    }
}