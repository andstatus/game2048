import com.soywiz.klock.DateTimeTz
import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.*
import kotlin.test.*

class GameStartTest : ViewsForTesting(log = true) {

    @Test
    fun test() = viewsTest {
        appEntry(this, animateViews = false)
        assertEquals(4, settings.boardWidth, "Settings are not initialized")

        persistGameRecordTest()
        restartTest()
        mergeTest()
    }

    private fun persistGameRecordTest() {
        val square = Square(1, 2)
        val movePlace = MovePlace(PlacedPiece(Piece.N2, square))
        val playersMove = PlayerMove(PlayerEnum.COMPUTER, PlayerMoveEnum.PLACE, listOf(movePlace))
        playersMove.toJson()

        val gameRecord = GameRecord(DateTimeTz.nowLocal(), listOf(playersMove), Board())
        val gameRecordJson = gameRecord.toJson()
        assertTrue(gameRecordJson.contains("place"))
        val gameRecordRestored = GameRecord.fromJson(gameRecordJson)
        assertTrue(gameRecordRestored != null, gameRecordJson)

        assertEquals(gameRecord.playerMoves, gameRecordRestored.playerMoves)
    }

    private fun restartTest() {
        presenter.computerMove()
        presenter.computerMove()
        assertTrue(presenter.boardViews.array.count { it != null } > 1, presenterString())
        assertTrue(presenter.model.history.currentGame.playerMoves.size > 1, currentGameString())

        presenter.restart()
        assertEquals(1, presenter.boardViews.array.count { it != null }, presenterString())
        assertEquals( 1, presenter.model.board.array.count { it != null }, modelString())
        assertEquals(1, presenter.model.history.currentGame.playerMoves.size, currentGameString())

        presenter.computerMove()
        assertEquals(2, presenter.model.board.array.count { it != null }, modelString())
        assertEquals(2, presenter.model.history.currentGame.playerMoves.size, currentGameString())
    }

    private fun mergeTest() {
        presenter.composerMove(Board())
        assertEquals(0, presenter.boardViews.array.count { it != null }, presenterString())
        val square1 = Square(1, 1)
        val piece1 = PlacedPiece(Piece.N2, square1)
        val square2 = Square(1, 2)
        val piece2 = PlacedPiece(Piece.N2, square2)
        presenter.computerMove(piece1)
        assertEquals(Piece.N2, presenter.boardViews[square1]?.piece, presenterString())
        presenter.computerMove(piece2)
        assertEquals(Piece.N2, presenter.boardViews[square2]?.piece, presenterString())

        val board1 = presenter.model.board.copy()
        val piecesOnBoardViews1 = presenter.boardViews.array.map { it?.piece }
        assertEquals(board1.array.asList(), piecesOnBoardViews1, presenterString())

        presenter.userMove(PlayerMoveEnum.DOWN)
        assertEquals(Piece.N4, presenter.boardViews[Square(1, 3)]?.piece, presenterString())
        assertEquals(2, presenter.boardViews.array.count { it != null }, presenterString())

        val board2 = presenter.model.board.copy()
        val piecesOnBoardViews2 = presenter.boardViews.array.map { it?.piece }
        assertEquals(board2.array.asList(), piecesOnBoardViews2, presenterString())
        assertTrue(presenter.canUndo(), historyString())

        presenter.undo()
        val board3 = presenter.model.board.copy()
        val piecesOnBoardViews3 = presenter.boardViews.array.map { it?.piece }

        assertEquals(board1.array.asList(), board3.array.asList(),"Board after undo")
        assertEquals(board3.array.asList(), piecesOnBoardViews3, "Board views after undo")
        assertTrue(presenter.canRedo(), historyString())

        presenter.redo()
        val board4 = presenter.model.board.copy()
        val piecesOnBoardViews4 = presenter.boardViews.array.map { it?.piece }

        assertEquals(board2.array.asList(), board4.array.asList(), "Board after redo")
        assertEquals(board4.array.asList(), piecesOnBoardViews4, "Board views after redo")
        assertFalse(presenter.canRedo(), historyString())

        presenter.userMove(PlayerMoveEnum.UP)
        assertTrue(presenter.canUndo(), historyString())
        assertFalse(presenter.canRedo(), historyString())

        presenter.undo()
        val board5 = presenter.model.board.copy()
        val piecesOnBoardViews5 = presenter.boardViews.array.map { it?.piece }

        assertEquals(board4.array.asList(), board5.array.asList(),"Board after second undo")
        assertEquals(board5.array.asList(), piecesOnBoardViews5, "Board views second after undo")
        assertTrue(presenter.canRedo(), historyString())
        assertTrue(presenter.canUndo(), historyString())
    }

    private fun presenterString(): String = "BoardViews:" + presenter.boardViews.array.mapIndexed { ind, block ->
        ind.toString() + ":" + (block?.piece ?: "-")
    }.toString()

    private fun currentGameString(): String = "CurrentGame" + presenter.model.history.currentGame.playerMoves
            .mapIndexed { ind, playerMove ->
                "\n" + (ind+1).toString() + ":" + playerMove
            }

    private fun modelString(): String = "Model:" + presenter.model.board.array.mapIndexed { ind, piece ->
        ind.toString() + ":" + (piece ?: "-")
    }

    private fun historyString(): String = with(presenter.model.history) {
        "History: index:$historyIndex, moves:${currentGame.playerMoves.size}"
    }
}