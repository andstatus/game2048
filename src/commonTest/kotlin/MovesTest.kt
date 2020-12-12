import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.Square
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MovesTest : ViewsForTesting(log = true) {

    @Test
    fun movesTest() = viewsTest { testInitializeGameView() {
        this.presenter.onPlayClick()
        this.presenter.composerMove(Board(this.settings))
        assertEquals(0, this.presenter.boardViews.blocks.size, this.modelAndViews())
        val square1 = Square(1, 1)
        val piece1 = PlacedPiece(Piece.N2, square1)
        val square2 = Square(1, 2)
        val piece2 = PlacedPiece(Piece.N2, square2)
        this.presenter.computerMove(piece1)
        assertEquals(listOf(Piece.N2), this.blocksAt(square1), this.modelAndViews())
        this.presenter.computerMove(piece2)
        assertEquals(listOf(Piece.N2), this.blocksAt(square2), this.modelAndViews())
        val board1 = this.presenter.model.board.copy()
        val piecesOnBoardViews1 = this.presentedPieces()
        assertEquals(board1.array.asList(), piecesOnBoardViews1, this.modelAndViews())
        assertEquals(2, board1.moveNumber, this.modelAndViews())
        this.presenter.onBookmarkClick()
        assertEquals(
            board1.moveNumber,
            this.presenter.model.history.currentGame.shortRecord.bookmarks[0].moveNumber,
            this.modelAndViews()
        )
        this.presenter.onSwipe(SwipeDirection.BOTTOM)
        assertEquals(listOf(Piece.N4), this.blocksAt(Square(1, 3)), this.modelAndViews())
        assertEquals(2, this.presenter.boardViews.blocks.size, this.modelAndViews())
        val board2 = this.presenter.model.board.copy()
        val piecesOnBoardViews2 = this.presentedPieces()
        assertEquals(board2.array.asList(), piecesOnBoardViews2, this.modelAndViews())
        assertTrue(this.presenter.canUndo(), this.historyString())
        this.presenter.undo()
        val board3 = this.presenter.model.board.copy()
        val piecesOnBoardViews3 = this.presentedPieces()
        assertEquals(board1.array.asList(), board3.array.asList(), "Board after undo")
        assertEquals(board3.array.asList(), piecesOnBoardViews3, "Board views after undo")
        assertTrue(this.presenter.canRedo(), this.historyString())
        this.presenter.redo()
        val board4 = this.presenter.model.board.copy()
        val piecesOnBoardViews4 = this.presentedPieces()
        assertEquals(board2.array.asList(), board4.array.asList(), "Board after redo")
        assertEquals(board4.array.asList(), piecesOnBoardViews4, "Board views after redo")
        assertFalse(this.presenter.canRedo(), this.historyString())
        this.presenter.onSwipe(SwipeDirection.TOP)
        assertTrue(this.presenter.canUndo(), this.historyString())
        assertFalse(this.presenter.canRedo(), this.historyString())
        assertEquals(
            board1.moveNumber,
            this.presenter.model.history.currentGame.shortRecord.bookmarks[0].moveNumber,
            this.modelAndViews()
        )
        assertEquals(1, this.presenter.model.history.currentGame.shortRecord.bookmarks.size, this.modelAndViews())
        this.presenter.undo()
        val board5 = this.presenter.model.board.copy()
        val piecesOnBoardViews5 = this.presentedPieces()
        assertEquals(board4.array.asList(), board5.array.asList(), "Board after second undo")
        assertEquals(board5.array.asList(), piecesOnBoardViews5, "Board views second after undo")
        assertTrue(this.presenter.canRedo(), this.historyString())
        assertTrue(this.presenter.canUndo(), this.historyString())
    } }
}