import korlibs.korge.tests.ViewsForTesting
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.Ply
import org.andstatus.game2048.myLog
import org.andstatus.game2048.view.ViewData
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistenceTest : ViewsForTesting(log = true) {

    @Test
    fun persistenceTest() = myViewsTest(this) {
        persistGameRecordTest(0)
        persistGameRecordTest(1)
        persistGameRecordTest(2)
    }

    private fun ViewData.persistGameRecordTest(nPlies: Int) {
        myLog("persistGameRecordTest started, n=$nPlies")
        val board = presenter.model.gamePosition.board
        val plies = ArrayList<Ply>()
        var nPliesActual = 0
        while (nPliesActual < nPlies) {
            val square = when (nPliesActual) {
                1 -> board.toSquare(2, 2)
                else -> board.toSquare(1, 3)
            }
            val ply = Ply.computerPly(PlacedPiece(Piece.N2, square), 0)
            assertTrue(ply.toMap().keys.size > 1, ply.toMap().toString())
            plies.add(ply)
            nPliesActual++
        }

        val gameRecord = newGameRecord(
            settings,
            GamePosition(board, plyNumber = nPlies),  // Final board is incorrect here - it is empty.
            presenter.model.history.idForNewGame(),
            emptyList(),
            plies
        )
        val id1 = gameRecord.id
        val sharedJson = gameRecord.toSharedJsonSequence().toList().asSequence()
        val sharedJsonString = gameRecord.toSharedJsonSequence().toTextLines()
        val longString1 = gameRecord.toLongString()
        val message = "nMoves:$nPlies, $sharedJsonString"

        if (nPlies > 0) {
            assertContains(sharedJsonString, "place", false, message)
        } else {
            assertFalse(sharedJsonString.contains("place"), message)
        }
        myLog("Current game 1, n=$nPlies: ${presenter.model.history.currentGame}")
        presenter.loadSharedJson(sharedJson)
        waitFor("currentGame loaded") {
            presenter.model.history.currentGame.gamePlies.isReady
        }

        val gameRecordOpened = presenter.model.history.currentGame
        myLog("Opened game 2, n=$nPlies: $gameRecordOpened")
        assertEquals(longString1, gameRecordOpened.toLongString()
            .replace("id:${gameRecordOpened.id} ", "id:$id1 "), message)
    }
}