import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korio.concurrent.atomic.korAtomic
import kotlinx.coroutines.CoroutineScope
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.GameClock
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.GameRecord
import org.andstatus.game2048.model.History
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PieceMoveOne
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.Ply
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.view.ViewData
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryTest : ViewsForTesting(log = true) {

    @Test
    fun historyTest() {
        val testWasExecuted = korAtomic(false)
        viewsTest {

            var game1: GameRecord? = null
            unsetGameView()
            initializeViewDataInTest {
                val settings = Settings.load(stage)
                assertEquals(4, settings.boardWidth, "Settings are not initialized")
                assertTrue(settings.isTestRun, "Should be Test Run")
                game1 = saveTestHistory(settings).currentGame
            }

            sWaitFor("game1 is loaded") {
                game1 != null
            }

            unsetGameView()
            initializeViewDataInTest {
                assertTestHistory(game1!!)
                testWasExecuted.value = true
            }
        }
        waitFor("historyTest was executed") { testWasExecuted.value }
    }

    private suspend fun saveTestHistory(settings: Settings): History = with(History.load(settings)) {
        val board = settings.defaultBoard
        val placedPiece = PlacedPiece(Piece.N2, board.toSquare(1, 2))
        val ply1 = Ply.computerPly(placedPiece, 0)
        val ply2 = Ply.userPly(
            PlyEnum.DOWN, 1, 0, listOf(
                PieceMoveOne(
                    placedPiece,
                    board.toSquare(1, 3)
                )
            )
        )
        val ply3 = Ply.computerPly(PlacedPiece(Piece.N4, board.toSquare(2, 1)), 2)
        val position = GamePosition(
            board, Ply.emptyPly,
            pieces = arrayOf(
                null, null, null, null,
                null, null, null, null,
                null, null, Piece.N4, null,
                null, Piece.N2, null, null
            ),
            score = 2,
            gameClock = GameClock(125),
            plyNumber = 3
        )
        newGameRecord(
            settings,
            position,
            idForNewGame(),
            listOf(GamePosition(board), position),
            listOf(ply1, ply2, ply3)
        ).let {
            it.save()
            openGame(it, it.id)
        }
        assertTrue(canUndo(), currentGame?.toLongString())
        assertFalse(canRedo(), currentGame?.toLongString())
        saveCurrent(CoroutineScope(coroutineContext)).also {
            it.currentGame?.load()
            sWaitFor {
                currentGame?.notCompleted == false
            }
        }
    }

    private fun ViewData.assertTestHistory(expected: GameRecord) {
        val actualGame = presenter.model.history.currentGame?.load()
            ?: throw AssertionError("No current game")
        assertEquals(expected.gamePlies.toLongString(), actualGame.gamePlies.toLongString(), currentGameString())
        assertEquals(expected.score, actualGame.score, currentGameString())
        assertEquals(expected.shortRecord.bookmarks.size, actualGame.shortRecord.bookmarks.size, currentGameString())
        assertEquals(expected.toSharedJson(), actualGame.toSharedJson(), currentGameString())
        assertTrue(presenter.canUndo(), modelAndViews() + "\n" + currentGameString())
        assertFalse(presenter.canRedo(), modelAndViews() + "\n" + currentGameString())
    }
}