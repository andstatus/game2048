import com.soywiz.korge.tests.ViewsForTesting
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistenceTest : ViewsForTesting(log = true) {

    @Test
    fun persistenceTest() = viewsTest {
        unsetGameView()
        val settings = Settings.load(stage)
        assertEquals(4, settings.boardWidth, "Settings are not initialized")
        assertTrue(settings.isTestRun, "Should be Test Run")
        val history = saveTestHistory(settings)

        initializeViewDataInTest {
            persistGameRecordTest(settings)
            assertTestHistory(history)
            restartTest()
        }
    }

    private suspend fun saveTestHistory(settings: Settings) = with (History.load(settings)) {
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
        currentGame = GameRecord.newWithPositionAndMoves(position,
            listOf(GamePosition(board), position),
            listOf(ply1, ply2, ply3))
        saveCurrent()
    }

    private fun ViewData.persistGameRecordTest(settings: Settings) {
        persistGameRecordTest2(settings, 0)
        persistGameRecordTest2(settings, 1)
        persistGameRecordTest2(settings, 2)
    }

    private fun ViewData.persistGameRecordTest2(settings: Settings, nMoves: Int) {
        val board = presenter.model.gamePosition.board
        val moves = ArrayList<Ply>()
        var nMovesActual = 0
        while (nMovesActual < nMoves) {
            val square = when (nMovesActual) {
                1 -> board.toSquare(2, 2)
                else -> board.toSquare(1, 3)
            }
            val move = Ply.computerPly(PlacedPiece(Piece.N2, square), 0)
            assertTrue(move.toMap().keys.size > 2, move.toMap().toString())
            moves.add(move)
            nMovesActual++
        }

        val gameRecord = GameRecord.newWithPositionAndMoves(GamePosition(board), emptyList(), moves)
        val gameRecordJson = gameRecord.toJsonString()
        val message = "nMoves:$nMoves, $gameRecordJson"

        if (nMoves > 0) {
            assertTrue(gameRecordJson.contains("place"), message)
        }
        val gameRecordOpened = GameRecord.fromJson(settings, gameRecordJson)
        assertTrue(gameRecordOpened != null, message)

        assertEquals(gameRecord.plies, gameRecordOpened.plies, message)
    }

    private fun ViewData.assertTestHistory(expected: History) {
        val actual = presenter.model.history
        assertEquals(expected.currentGame.plies, actual.currentGame.plies, modelAndViews())
        assertEquals(expected.currentGame.score, actual.currentGame.score, modelAndViews())
        assertEquals(expected.currentGame.shortRecord.bookmarks.size, actual.currentGame.shortRecord.bookmarks.size, modelAndViews())
        assertEquals(expected.currentGame.toJsonString(), actual.currentGame.toJsonString(), modelAndViews())
        assertTrue(presenter.canUndo(), modelAndViews())
        assertFalse(presenter.canRedo(), modelAndViews())
    }

    private fun ViewData.restartTest() {
        presenter.computerMove()
        presenter.computerMove()
        assertTrue(presenter.boardViews.blocks.size > 1, modelAndViews())
        assertTrue(presenter.model.history.currentGame.plies.size > 1, currentGameString())

        presenter.onRestartClick()
        assertEquals(1, presenter.boardViews.blocks.size, modelAndViews())
        assertEquals( 1, presenter.model.gamePosition.pieces.count { it != null }, modelAndViews())
        assertEquals(1, presenter.model.history.currentGame.plies.size, currentGameString())

        presenter.computerMove()
        assertEquals(2, presenter.model.gamePosition.pieces.count { it != null }, modelAndViews())
        assertEquals(2, presenter.model.history.currentGame.plies.size, currentGameString())
    }
}