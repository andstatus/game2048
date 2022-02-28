import com.soywiz.korge.tests.ViewsForTesting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HistoryTest : ViewsForTesting(log = true) {

    @Test
    fun historyTest() = myViewsTest(this) {
        val game1 = generateGame(7, 3)
        assertEquals(1, game1.shortRecord.bookmarks.size, currentGameString())

        if (presenter.model.history.recentGames.none { it.id != game1.id }) {
            generateGame(5)
        }

        val actualGame = presenter.model.history.openGame(game1.id)?.load()
        assertNotNull(actualGame, currentGameString())
        assertEquals(game1.toLongString(), actualGame.toLongString(), currentGameString())
        assertEquals(game1.score, actualGame.score, currentGameString())
        assertEquals(game1.shortRecord.bookmarks.size, actualGame.shortRecord.bookmarks.size, currentGameString())
        assertEquals(game1.toSharedJsonSequence().toTextLines(), actualGame.toSharedJsonSequence().toTextLines(), currentGameString())
        assertTrue(presenter.canUndo(), modelAndViews() + "\n" + currentGameString())
        assertFalse(presenter.canRedo(), modelAndViews() + "\n" + currentGameString())
    }

}