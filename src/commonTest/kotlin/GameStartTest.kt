import com.soywiz.klock.DateTimeTz
import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        presenter.usersMove(PlayerMoveEnum.DOWN)
        assertEquals(Piece.N4, presenter.boardViews[Square(1, 3)]?.piece, presenterString())
        assertEquals(2, presenter.boardViews.array.count { it != null }, presenterString())
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
}