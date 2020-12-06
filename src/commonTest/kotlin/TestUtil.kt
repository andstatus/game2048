import org.andstatus.game2048.model.Square
import org.andstatus.game2048.presenter.Presenter
import org.andstatus.game2048.view.GameView
import kotlin.properties.Delegates

var gameView: GameView by Delegates.notNull()
val presenter: Presenter get() = gameView.presenter

fun presentedPieces() = presenter.boardViews.blocksOnBoard.map { it.firstOrNull()?.piece }

fun blocksAt(square: Square) = presenter.boardViews.getAll(square).map { it.piece }

fun modelAndViews() =
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

fun currentGameString(): String = "CurrentGame" + presenter.model.history.currentGame.playerMoves
        .mapIndexed { ind, playerMove ->
            "\n" + (ind + 1).toString() + ":" + playerMove
        }

fun historyString(): String = with(presenter.model.history) {
    "History: index:$historyIndex, moves:${currentGame.playerMoves.size}"
}
