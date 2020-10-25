import com.soywiz.klock.DateTimeTz
import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistenceTest : ViewsForTesting(log = true) {

    @Test
    fun persistenceTest() = viewsTest {
        loadSettings(this)
        assertEquals(4, settings.boardWidth, "Settings are not initialized")
        val history = saveTestHistory()
        appEntry(this, animateViews = false)

        persistGameRecordTest()
        assertTestHistory(history)

        restartTest()
    }

    private fun saveTestHistory() = with (History()) {
        val placedPiece = PlacedPiece(Piece.N2, Square(1, 2))
        val move1 = PlayerMove.computerMove(placedPiece)
        val move2 = PlayerMove.userMove(PlayerMoveEnum.DOWN, listOf(MoveOne(placedPiece, Square(1, 3))))
        val move3 = PlayerMove.computerMove(PlacedPiece(Piece.N4, Square(2, 1)))
        val finalBoard = Board(array = arrayOf(null, null, null, null,
                null, null, null, null,
                null, null, Piece.N4, null,
                null, Piece.N2, null, null))
        currentGame = GameRecord(DateTimeTz.nowLocal(), listOf(move1, move2, move3), finalBoard)
            onUpdate()
    }

    private fun persistGameRecordTest() {
        persistGameRecordTest2(0)
        persistGameRecordTest2(1)
        persistGameRecordTest2(2)
    }

    private fun persistGameRecordTest2(nMoves: Int) {
        val moves = ArrayList<PlayerMove>()
        var nMovesActual = 0
        while (nMovesActual < nMoves) {
            val square = when (nMovesActual) {
                1 -> Square(2, 2)
                else -> Square(1, 3)
            }
            val move = PlayerMove.computerMove(PlacedPiece(Piece.N2, square))
            assertTrue(move.toJson().keys.size > 2, move.toJson().toString())
            moves.add(move)
            nMovesActual++
        }

        val gameRecord = GameRecord(DateTimeTz.nowLocal(), moves, Board())
        val gameRecordJson = gameRecord.toJson()
        val message = "nMoves:$nMoves, $gameRecordJson"

        if (nMoves > 0) {
            assertTrue(gameRecordJson.contains("place"), message)
        }
        val gameRecordRestored = GameRecord.fromJson(gameRecordJson)
        assertTrue(gameRecordRestored != null, message)

        assertEquals(gameRecord.playerMoves, gameRecordRestored.playerMoves, message)
    }

    private fun assertTestHistory(expected: History) {
        val actual = presenter.model.history
        assertEquals(expected.currentGame.playerMoves, actual.currentGame.playerMoves, modelAndViews())
        assertEquals(expected.currentGame.finalBoard.score, actual.currentGame.finalBoard.score, modelAndViews())
        assertEquals(expected.currentGame.toJson(), actual.currentGame.toJson(), modelAndViews())
        assertTrue(presenter.canUndo(), modelAndViews())
        assertFalse(presenter.canRedo(), modelAndViews())
    }

    private fun restartTest() {
        presenter.computerMove()
        presenter.computerMove()
        assertTrue(presenter.boardViews.blocks.size > 1, modelAndViews())
        assertTrue(presenter.model.history.currentGame.playerMoves.size > 1, currentGameString())

        presenter.restart()
        assertEquals(1, presenter.boardViews.blocks.size, modelAndViews())
        assertEquals( 1, presenter.model.board.array.count { it != null }, modelAndViews())
        assertEquals(1, presenter.model.history.currentGame.playerMoves.size, currentGameString())

        presenter.computerMove()
        assertEquals(2, presenter.model.board.array.count { it != null }, modelAndViews())
        assertEquals(2, presenter.model.history.currentGame.playerMoves.size, currentGameString())
    }
}