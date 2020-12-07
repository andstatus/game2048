import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.loadSettings
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.view.GameView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MovesTest : ViewsForTesting(log = true) {

    @Test
    fun movesTest() = viewsTest {
        gameView = GameView.initialize(this, loadSettings(this), animateViews = false)

        presenter.onPlayClick()
        presenter.composerMove(Board(gameView.settings))
        assertEquals(0, presenter.boardViews.blocks.size, modelAndViews())

        val square1 = Square(1, 1)
        val piece1 = PlacedPiece(Piece.N2, square1)
        val square2 = Square(1, 2)
        val piece2 = PlacedPiece(Piece.N2, square2)
        presenter.computerMove(piece1)
        assertEquals(listOf(Piece.N2), blocksAt(square1), modelAndViews())
        presenter.computerMove(piece2)
        assertEquals(listOf(Piece.N2), blocksAt(square2), modelAndViews())

        val board1 = presenter.model.board.copy()
        val piecesOnBoardViews1 = presentedPieces()
        assertEquals(board1.array.asList(), piecesOnBoardViews1, modelAndViews())
        assertEquals(2, board1.moveNumber, modelAndViews())

        presenter.onBookmarkClick()
        assertEquals(board1.moveNumber, presenter.model.history.currentGame.shortRecord.bookmarks[0].moveNumber, modelAndViews())

        presenter.onSwipe(SwipeDirection.BOTTOM)
        assertEquals(listOf(Piece.N4), blocksAt(Square(1, 3)), modelAndViews())
        assertEquals(2, presenter.boardViews.blocks.size, modelAndViews())

        val board2 = presenter.model.board.copy()
        val piecesOnBoardViews2 = presentedPieces()
        assertEquals(board2.array.asList(), piecesOnBoardViews2, modelAndViews())
        assertTrue(presenter.canUndo(), historyString())

        presenter.undo()
        val board3 = presenter.model.board.copy()
        val piecesOnBoardViews3 = presentedPieces()

        assertEquals(board1.array.asList(), board3.array.asList(),"Board after undo")
        assertEquals(board3.array.asList(), piecesOnBoardViews3, "Board views after undo")
        assertTrue(presenter.canRedo(), historyString())

        presenter.redo()
        val board4 = presenter.model.board.copy()
        val piecesOnBoardViews4 = presentedPieces()

        assertEquals(board2.array.asList(), board4.array.asList(), "Board after redo")
        assertEquals(board4.array.asList(), piecesOnBoardViews4, "Board views after redo")
        assertFalse(presenter.canRedo(), historyString())

        presenter.onSwipe(SwipeDirection.TOP)
        assertTrue(presenter.canUndo(), historyString())
        assertFalse(presenter.canRedo(), historyString())

        assertEquals(board1.moveNumber, presenter.model.history.currentGame.shortRecord.bookmarks[0].moveNumber, modelAndViews())
        assertEquals(1, presenter.model.history.currentGame.shortRecord.bookmarks.size, modelAndViews())

        presenter.undo()
        val board5 = presenter.model.board.copy()
        val piecesOnBoardViews5 = presentedPieces()

        assertEquals(board4.array.asList(), board5.array.asList(),"Board after second undo")
        assertEquals(board5.array.asList(), piecesOnBoardViews5, "Board views second after undo")
        assertTrue(presenter.canRedo(), historyString())
        assertTrue(presenter.canUndo(), historyString())
    }
}