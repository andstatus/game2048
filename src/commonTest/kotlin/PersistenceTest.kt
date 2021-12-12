import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korio.concurrent.atomic.korAtomic
import kotlinx.coroutines.CoroutineScope
import org.andstatus.game2048.Settings
import org.andstatus.game2048.gameIsLoading
import org.andstatus.game2048.model.GameClock
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.GameRecord
import org.andstatus.game2048.model.History
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PieceMoveOne
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.Ply
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.view.ViewData
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistenceTest : ViewsForTesting(log = true) {

    @Test
    fun persistenceTest()  {
        val testWasExecuted = korAtomic(false)
        viewsTest {

            var game1: GameRecord? = null
            unsetGameView()
            initializeViewDataInTest {
                val settings = Settings.load(stage)
                assertEquals(4, settings.boardWidth, "Settings are not initialized")
                assertTrue(settings.isTestRun, "Should be Test Run")
                game1 = saveTestHistory(settings).currentGame
            }

            unsetGameView()
            initializeViewDataInTest {
                persistGameRecordTest()
                assertTestHistory(game1!!)
                testWasExecuted.value = true
            }
        }
        waitFor("persistenceTest was executed") { -> testWasExecuted.value }
    }

    private suspend fun saveTestHistory(settings: Settings): History = with (History.load(settings)) {
        val board = settings.defaultBoard
        val placedPiece = PlacedPiece(Piece.N2, board.toSquare(1, 2))
        val ply1 = Ply.computerPly(placedPiece, 0)
        val ply2 = Ply.userPly(PlyEnum.DOWN, 1, listOf(PieceMoveOne(placedPiece,
            board.toSquare(1, 3))))
        val ply3 = Ply.computerPly(PlacedPiece(Piece.N4, board.toSquare(2, 1)), 2)
        val position = GamePosition(board, Ply.emptyPly,
            pieces = arrayOf(
                null, null, null, null,
                null, null, null, null,
                null, null, Piece.N4, null,
                null, Piece.N2, null, null
            ),
            score = 2,
            gameClock = GameClock(125),
            plyNumber = 3
        )
        GameRecord.newWithPositionAndPlies(
            settings,
            position,
            idForNewGame(),
            listOf(GamePosition(board), position),
            listOf(ply1, ply2, ply3)
        ).let {
            openGame(it, it.id)
        }
        assertTrue(canUndo(),  currentGame.toLongString())
        assertFalse(canRedo(), currentGame.toLongString())
        saveCurrent(CoroutineScope(coroutineContext)).also {
            waitFor { gameIsLoading.value == false }
        }
    }

    private fun ViewData.persistGameRecordTest() {
        persistGameRecordTest2(0)
        persistGameRecordTest2(1)
        persistGameRecordTest2(2)
    }

    private fun ViewData.persistGameRecordTest2(nPlies: Int) {
        val board = presenter.model.gamePosition.board
        val plies = ArrayList<Ply>()
        var nPliesActual = 0
        while (nPliesActual < nPlies) {
            val square = when (nPliesActual) {
                1 -> board.toSquare(2, 2)
                else -> board.toSquare(1, 3)
            }
            val ply = Ply.computerPly(PlacedPiece(Piece.N2, square), 0)
            assertTrue(ply.toMap().keys.size > 2, ply.toMap().toString())
            plies.add(ply)
            nPliesActual++
        }

        val gameRecord = GameRecord.newWithPositionAndPlies(
            settings,
            GamePosition(board),
            presenter.model.history.idForNewGame(),
            emptyList(),
            plies
        )
        val sharedJson = gameRecord.toSharedJson()
        val message = "nMoves:$nPlies, $sharedJson"

        if (nPlies > 0) {
            assertTrue(sharedJson.contains("place"), message)
        }
        val gameRecordOpened = GameRecord.fromSharedJson(settings, sharedJson, presenter.model.history.idForNewGame())
        assertTrue(gameRecordOpened != null, message)

        gameRecordOpened.gamePlies.load()
        assertEquals(gameRecord.gamePlies.toLongString(), gameRecordOpened.gamePlies.toLongString(), message)
    }

    private fun ViewData.assertTestHistory(expected: GameRecord) {
        val actualGame = presenter.model.history.currentGame.load()
        assertEquals(expected.gamePlies.toLongString(), actualGame.gamePlies.toLongString(), modelAndViews())
        assertEquals(expected.score, actualGame.score, modelAndViews())
        assertEquals(expected.shortRecord.bookmarks.size, actualGame.shortRecord.bookmarks.size, modelAndViews())
        assertEquals(expected.toSharedJson(), actualGame.toSharedJson(), modelAndViews())
        assertTrue(presenter.canUndo(), modelAndViews() + "\n" + currentGameString())
        assertFalse(presenter.canRedo(), modelAndViews() + "\n" + currentGameString())
    }
}