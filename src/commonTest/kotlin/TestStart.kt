import com.soywiz.klock.DateTimeTz
import com.soywiz.korge.tests.ViewsForTesting
import org.andstatus.game2048.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestStart : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        gameEntry(this)
        assertEquals(4, settings.boardWidth, "Settings are not initialized")

        persistGameRecordTest()

        presenter.usersMove(PlayersMoveEnum.RIGHT)
    }

    private fun persistGameRecordTest() {
        val square = Square(1, 2)
        val movePlace = MovePlace(PlacedPiece(Piece.N2, square))
        val playersMove = PlayersMove(PlayerEnum.SETTER, PlayersMoveEnum.PLACE, listOf(movePlace))
        playersMove.toJson()

        val gameRecord = GameRecord(DateTimeTz.nowLocal(), listOf(playersMove), Board())
        val gameRecordJson = gameRecord.toJson()
        assertTrue(gameRecordJson.contains("place"))
        val gameRecordRestored = GameRecord.fromJson(gameRecordJson)
        assertTrue(gameRecordRestored != null, gameRecordJson)

        assertEquals(gameRecord.playersMoves, gameRecordRestored.playersMoves)
    }
}