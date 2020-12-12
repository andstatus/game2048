import com.soywiz.korge.view.Stage
import com.soywiz.korio.async.launch
import kotlinx.coroutines.coroutineScope
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.myLog
import org.andstatus.game2048.view.GameView
import org.andstatus.game2048.view.initializeGameView

// TODO: Make some separate class for this...
private val uninitializedLazy = lazy { throw IllegalStateException("Value is not initialized yet") }
private var lazyGameView: Lazy<GameView> = uninitializedLazy
private var gameView: GameView get() = lazyGameView.value
    set(value) { lazyGameView = lazyOf(value) }
fun unsetGameView() {
    if (lazyGameView.isInitialized()) lazyGameView = uninitializedLazy
}
suspend fun Stage.testInitializeGameView(handler: suspend GameView.() -> Unit = {}) {
    if (lazyGameView.isInitialized()) {
        gameView.handler()
        return
    }

    coroutineScope {
        launch {
            initializeGameView(stage, animateViews = false) {
                myLog("Initialized in test")
                gameView = this
            }
        }.join()
        myLog("Initialized after join")
        gameView.handler()
    }
}

fun GameView.presentedPieces() = presenter.boardViews.blocksOnBoard.map { it.firstOrNull()?.piece }

fun GameView.blocksAt(square: Square) = presenter.boardViews.getAll(square).map { it.piece }

fun GameView.modelAndViews() =
        "Model:     " + presenter.model.board.array.mapIndexed { ind, piece ->
            ind.toString() + ":" + (piece?.text ?: "-")
        } +
        (if (presenter.model.history.currentGame.shortRecord.bookmarks.isNotEmpty())
            "  bookmarks: " + presenter.model.history.currentGame.shortRecord.bookmarks.size
            else "") +
        "\n" +
        "BoardViews:" + presenter.boardViews.blocksOnBoard.mapIndexed { ind, list ->
            ind.toString() + ":" + (if (list.isEmpty()) "-" else list.joinToString(transform = { it.piece.text }))
        }

fun GameView.currentGameString(): String = "CurrentGame" + presenter.model.history.currentGame.playerMoves
        .mapIndexed { ind, playerMove ->
            "\n" + (ind + 1).toString() + ":" + playerMove
        }

fun GameView.historyString(): String = with(presenter.model.history) {
    "History: index:$historyIndex, moves:${currentGame.playerMoves.size}"
}
