import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.PositionData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MovesTest : ViewsForTesting(log = true) {

    @Test
    fun movesTest() = viewsTest { initializeViewDataInTest() {
        this.presenter.onWatchClick()
        this.presenter.composerMove(PositionData(this.settings))
        assertEquals(0, this.presenter.boardViews.blocks.size, this.modelAndViews())
        val square1 = settings.squares.toSquare(1, 1)
        val piece1 = PlacedPiece(Piece.N2, square1)
        val square2 = settings.squares.toSquare(1, 2)
        val piece2 = PlacedPiece(Piece.N2, square2)
        this.presenter.computerMove(piece1)
        assertEquals(listOf(Piece.N2), this.blocksAt(square1), this.modelAndViews())
        this.presenter.computerMove(piece2)
        assertEquals(listOf(Piece.N2), this.blocksAt(square2), this.modelAndViews())
        val position1 = this.presenter.model.positionData.copy()
        val piecesOnBoardViews1 = this.presentedPieces()
        assertEquals(position1.array.asList(), piecesOnBoardViews1, this.modelAndViews())
        assertEquals(2, position1.plyNumber, this.modelAndViews())
        this.presenter.onBookmarkClick()
        assertEquals(
            position1.plyNumber,
            this.presenter.model.history.currentGame.shortRecord.bookmarks[0].plyNumber,
            this.modelAndViews()
        )
        this.presenter.onSwipe(SwipeDirection.BOTTOM)
        assertEquals(listOf(Piece.N4), this.blocksAt(settings.squares.toSquare(1, 3)), this.modelAndViews())
        assertEquals(2, this.presenter.boardViews.blocks.size, this.modelAndViews())
        val position2 = this.presenter.model.positionData.copy()
        val piecesOnBoardViews2 = this.presentedPieces()
        assertEquals(2, position2.score, this.modelAndViews())
        assertEquals(position2.array.asList(), piecesOnBoardViews2, this.modelAndViews())
        assertTrue(this.presenter.canUndo(), this.historyString())
        this.presenter.undo()
        val position3 = this.presenter.model.positionData.copy()
        val piecesOnBoardViews3 = this.presentedPieces()
        assertEquals(position1.array.asList(), position3.array.asList(), "Board after undo")
        assertEquals(position3.array.asList(), piecesOnBoardViews3, "Board views after undo")
        assertTrue(this.presenter.canRedo(), this.historyString())
        this.presenter.redo()
        val position4 = this.presenter.model.positionData.copy()
        val piecesOnBoardViews4 = this.presentedPieces()
        assertEquals(position2.array.asList(), position4.array.asList(), "Board after redo")
        assertEquals(position4.array.asList(), piecesOnBoardViews4, "Board views after redo")
        assertFalse(this.presenter.canRedo(), this.historyString())
        this.presenter.onSwipe(SwipeDirection.TOP)
        assertTrue(this.presenter.canUndo(), this.historyString())
        assertFalse(this.presenter.canRedo(), this.historyString())
        assertEquals(
            position1.plyNumber,
            this.presenter.model.history.currentGame.shortRecord.bookmarks[0].plyNumber,
            this.modelAndViews()
        )
        assertEquals(1, this.presenter.model.history.currentGame.shortRecord.bookmarks.size, this.modelAndViews())
        this.presenter.undo()
        val position5 = this.presenter.model.positionData.copy()
        val piecesOnBoardViews5 = this.presentedPieces()
        assertEquals(position4.array.asList(), position5.array.asList(), "Board after second undo")
        assertEquals(position5.array.asList(), piecesOnBoardViews5, "Board views second after undo")
        assertTrue(this.presenter.canRedo(), this.historyString())
        assertTrue(this.presenter.canUndo(), this.historyString())
    } }
}