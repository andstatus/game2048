import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korio.concurrent.atomic.korAtomic
import kotlinx.coroutines.CoroutineScope
import org.andstatus.game2048.Settings
import org.andstatus.game2048.gameIsLoading
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

class PersistenceTest : ViewsForTesting(log = true) {

    @Test
    fun persistenceTest()  {
        val testWasExecuted = korAtomic(false)
        viewsTest {
            unsetGameView()
            val settings = Settings.load(stage)
            assertEquals(4, settings.boardWidth, "Settings are not initialized")
            assertTrue(settings.isTestRun, "Should be Test Run")
            val history = saveTestHistory(settings)

            initializeViewDataInTest {
                persistGameRecordTest(history)
                assertTestHistory(history)
                restartTest()
                testWasExecuted.value = true
            }
        }
        waitFor("persistenceTest was executed") { -> testWasExecuted.value }
    }

    private suspend fun saveTestHistory(settings: Settings): History = with (History.load(settings)) {
        val board = settings.defaultBoard
        val placedPiece = PlacedPiece(Piece.N2, board.toSquare(1, 2))
        val ply1 = Ply.computerPly(placedPiece, 0)
        val ply2 = Ply.userPly(PlyEnum.DOWN, 1, listOf(PieceMoveOne(placedPiece,
            board.toSquare(1, 3))))
        val ply3 = Ply.computerPly(PlacedPiece(Piece.N4, board.toSquare(2, 1)), 2)
        val position = GamePosition(board, Ply.emptyPly,
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
        currentGame = GameRecord.newWithPositionAndPlies(
            settings,
            position,
            idForNewGame(),
            listOf(GamePosition(board), position),
            listOf(ply1, ply2, ply3)
        )
        saveCurrent(CoroutineScope(coroutineContext)).also {
            waitFor { gameIsLoading.value == false }
        }
    }

    private fun ViewData.persistGameRecordTest(history: History) {
        persistGameRecordTest2(history, 0)
        persistGameRecordTest2(history, 1)
        persistGameRecordTest2(history, 2)
    }

    private fun ViewData.persistGameRecordTest2(history: History, nPlies: Int) {
        val board = presenter.model.gamePosition.board
        val plies = ArrayList<Ply>()
        var nPliesActual = 0
        while (nPliesActual < nPlies) {
            val square = when (nPliesActual) {
                1 -> board.toSquare(2, 2)
                else -> board.toSquare(1, 3)
            }
            val ply = Ply.computerPly(PlacedPiece(Piece.N2, square), 0)
            assertTrue(ply.toMap().keys.size > 2, ply.toMap().toString())
            plies.add(ply)
            nPliesActual++
        }

        val gameRecord = GameRecord.newWithPositionAndPlies(
            settings,
            GamePosition(board),
            history.idForNewGame(),
            emptyList(),
            plies
        )
        val sharedJson = gameRecord.toSharedJson()
        val message = "nMoves:$nPlies, $sharedJson"

        if (nPlies > 0) {
            assertTrue(sharedJson.contains("place"), message)
        }
        val gameRecordOpened = GameRecord.fromSharedJson(settings, sharedJson, history.idForNewGame())
        assertTrue(gameRecordOpened != null, message)

        gameRecordOpened.gamePlies.load()
        assertEquals(gameRecord.gamePlies.toLongString(), gameRecordOpened.gamePlies.toLongString(), message)
    }

    private fun ViewData.assertTestHistory(expected: History) {
        val actualGame = presenter.model.history.currentGame.load()
        assertEquals(expected.currentGame.gamePlies.toLongString(), actualGame.gamePlies.toLongString(), modelAndViews())
        assertEquals(expected.currentGame.score, actualGame.score, modelAndViews())
        assertEquals(expected.currentGame.shortRecord.bookmarks.size, actualGame.shortRecord.bookmarks.size, modelAndViews())
        assertEquals(expected.currentGame.toSharedJson(), actualGame.toSharedJson(), modelAndViews())
        assertTrue(presenter.canUndo(), modelAndViews())
        assertFalse(presenter.canRedo(), modelAndViews())
    }

    private fun ViewData.restartTest() {
        presenter.computerMove()
        presenter.computerMove()
        assertTrue(presenter.boardViews.blocks.size > 1, modelAndViews())
        assertTrue(presenter.model.history.currentGame.isReady, currentGameString())
        assertTrue(presenter.model.history.currentGame.gamePlies.size > 1, currentGameString())

        waitForMainViewShown {
            presenter.onRestartClick()
        }

        assertEquals(1, presenter.boardViews.blocks.size, modelAndViews())
        assertEquals( 1, presenter.model.gamePosition.pieces.count { it != null }, modelAndViews())
        assertEquals(1, presenter.model.history.currentGame.gamePlies.size, currentGameString())

        presenter.computerMove()
        assertEquals(2, presenter.model.gamePosition.pieces.count { it != null }, modelAndViews())
        assertEquals(2, presenter.model.history.currentGame.gamePlies.size, currentGameString())
    }
}