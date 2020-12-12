import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korio.serialization.json.toJson
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.GameClock
import org.andstatus.game2048.model.GameRecord
import org.andstatus.game2048.model.History
import org.andstatus.game2048.model.MoveOne
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.PlayerMove
import org.andstatus.game2048.model.PlayerMoveEnum
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.view.GameView
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
        val history = saveTestHistory(settings)

        testInitializeGameView() {
            persistGameRecordTest(settings)
            assertTestHistory(history)
            restartTest()
        }
    }

    private suspend fun saveTestHistory(settings: Settings) = with (History.load(settings)) {
        val placedPiece = PlacedPiece(Piece.N2, Square(1, 2))
        val move1 = PlayerMove.computerMove(placedPiece, 0)
        val move2 = PlayerMove.userMove(PlayerMoveEnum.DOWN, 1, listOf(MoveOne(placedPiece, Square(1, 3))))
        val move3 = PlayerMove.computerMove(PlacedPiece(Piece.N4, Square(2, 1)), 2)
        val board = Board(
            settings.boardWidth, settings.boardHeight,
            array = arrayOf(
                null, null, null, null,
                null, null, null, null,
                null, null, Piece.N4, null,
                null, Piece.N2, null, null
            ),
            score = 2,
            gameClock = GameClock(125),
            moveNumber = 3
        )
        currentGame = GameRecord.newWithBoardAndMoves(board, listOf(Board(settings), board), listOf(move1, move2, move3))
        saveCurrent()
    }

    private fun GameView.persistGameRecordTest(settings: Settings) {
        persistGameRecordTest2(settings, 0)
        persistGameRecordTest2(settings, 1)
        persistGameRecordTest2(settings, 2)
    }

    private fun GameView.persistGameRecordTest2(settings: Settings, nMoves: Int) {
        val moves = ArrayList<PlayerMove>()
        var nMovesActual = 0
        while (nMovesActual < nMoves) {
            val square = when (nMovesActual) {
                1 -> Square(2, 2)
                else -> Square(1, 3)
            }
            val move = PlayerMove.computerMove(PlacedPiece(Piece.N2, square), 0)
            assertTrue(move.toMap().keys.size > 2, move.toMap().toString())
            moves.add(move)
            nMovesActual++
        }

        val gameRecord = GameRecord.newWithBoardAndMoves(Board(settings), emptyList(), moves)
        val gameRecordJson = gameRecord.toMap().toJson()
        val message = "nMoves:$nMoves, $gameRecordJson"

        if (nMoves > 0) {
            assertTrue(gameRecordJson.contains("place"), message)
        }
        val gameRecordRestored = GameRecord.fromJson(gameRecordJson)
        assertTrue(gameRecordRestored != null, message)

        assertEquals(gameRecord.playerMoves, gameRecordRestored.playerMoves, message)
    }

    private fun GameView.assertTestHistory(expected: History) {
        val actual = presenter.model.history
        assertEquals(expected.currentGame.playerMoves, actual.currentGame.playerMoves, modelAndViews())
        assertEquals(expected.currentGame.score, actual.currentGame.score, modelAndViews())
        assertEquals(expected.currentGame.shortRecord.bookmarks.size, actual.currentGame.shortRecord.bookmarks.size, modelAndViews())
        assertEquals(expected.currentGame.toMap().toJson(), actual.currentGame.toMap().toJson(), modelAndViews())
        assertTrue(presenter.canUndo(), modelAndViews())
        assertFalse(presenter.canRedo(), modelAndViews())
    }

    private fun GameView.restartTest() {
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