import com.soywiz.klock.Stopwatch
import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PlacedPiece
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MovesTest : ViewsForTesting(log = true) {

    @Test
    fun movesTest() {
        var testWasExecuted = false
        viewsTest {
            unsetGameView()
            initializeViewDataInTest {
                waitFor { -> presenter.boardViews.blocks.isNotEmpty() }
                assertTrue(presenter.boardViews.blocks.isNotEmpty(), modelAndViews())

                val board = presenter.model.gamePosition.board
                presenter.onWatchClick()
                presenter.composerMove(GamePosition(board))
                assertEquals(0, presenter.boardViews.blocks.size, modelAndViews())
                val square1 = board.toSquare(1, 1)
                val piece1 = PlacedPiece(Piece.N2, square1)
                val square2 = board.toSquare(1, 2)
                val piece2 = PlacedPiece(Piece.N2, square2)
                presenter.computerMove(piece1)
                assertEquals(listOf(Piece.N2), this.blocksAt(square1), modelAndViews())
                presenter.computerMove(piece2)
                assertEquals(listOf(Piece.N2), this.blocksAt(square2), modelAndViews())
                val position1 = presenter.model.gamePosition.copy()
                val piecesOnBoardViews1 = this.presentedPieces()
                assertEquals(position1.pieces.asList(), piecesOnBoardViews1, modelAndViews())
                assertEquals(2, position1.plyNumber, modelAndViews())
                presenter.onBookmarkClick()
                waitFor { -> presenter.model.history.currentGame.shortRecord.bookmarks.isNotEmpty() }
                assertEquals(
                    position1.plyNumber,
                    presenter.model.history.currentGame.shortRecord.bookmarks[0].plyNumber,
                    modelAndViews()
                )
                presenter.onSwipe(SwipeDirection.BOTTOM)
                assertEquals(listOf(Piece.N4), this.blocksAt(board.toSquare(1, 3)), modelAndViews())
                assertEquals(2, presenter.boardViews.blocks.size, modelAndViews())
                val position2 = presenter.model.gamePosition.copy()
                val piecesOnBoardViews2 = this.presentedPieces()
                assertEquals(2, position2.score, modelAndViews())
                assertEquals(position2.pieces.asList(), piecesOnBoardViews2, modelAndViews())
                assertTrue(presenter.canUndo(), this.historyString())
                presenter.undo()
                val position3 = presenter.model.gamePosition.copy()
                val piecesOnBoardViews3 = this.presentedPieces()
                assertEquals(position1.pieces.asList(), position3.pieces.asList(), "Board after undo")
                assertEquals(position3.pieces.asList(), piecesOnBoardViews3, "Board views after undo")
                assertTrue(presenter.canRedo(), this.historyString())
                presenter.redo()
                val position4 = presenter.model.gamePosition.copy()
                val piecesOnBoardViews4 = this.presentedPieces()
                assertEquals(position2.pieces.asList(), position4.pieces.asList(), "Board after redo")
                assertEquals(position4.pieces.asList(), piecesOnBoardViews4, "Board views after redo")
                assertFalse(presenter.canRedo(), this.historyString())
                presenter.onSwipe(SwipeDirection.TOP)
                assertTrue(presenter.canUndo(), this.historyString())
                assertFalse(presenter.canRedo(), this.historyString())
                assertEquals(
                    position1.plyNumber,
                    presenter.model.history.currentGame.shortRecord.bookmarks[0].plyNumber,
                    modelAndViews()
                )
                assertEquals(1, presenter.model.history.currentGame.shortRecord.bookmarks.size, modelAndViews())
                presenter.onUndoClick()
                waitFor { -> position4.pieces.contentEquals(presenter.model.gamePosition.copy().pieces) }
                val position5 = presenter.model.gamePosition.copy()
                val piecesOnBoardViews5 = this.presentedPieces()
                assertEquals(position4.pieces.asList(), position5.pieces.asList(), "Board after second undo")
                assertEquals(position5.pieces.asList(), piecesOnBoardViews5, "Board views second after undo")
                assertTrue(presenter.canRedo(), this.historyString())
                assertTrue(presenter.canUndo(), this.historyString())

                with(presenter.model.gameClock) {
                    assertFalse(started, "GameClock should be stopped now: $playedSeconds")
                    presenter.onSwipe(SwipeDirection.TOP)
                    assertTrue(started, "GameClock should start: $playedSeconds")
                    val stopWatch = Stopwatch().start()
                    waitFor { -> stopWatch.elapsed.seconds > 1 }
                    val val1 = playedSeconds
                    assertTrue(val1 > 0, "GameClock should tick: $val1")
                    waitFor { -> stopWatch.elapsed.seconds > 3 }
                    assertTrue(playedSeconds > val1, "GameClock should tick: $playedSeconds")
                }

                testWasExecuted = true
            }
        }
        waitFor { -> testWasExecuted }
        assertTrue(testWasExecuted, "Test wasn't executed")
    }
}