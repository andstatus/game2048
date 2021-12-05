import com.soywiz.klock.Stopwatch
import com.soywiz.korge.view.Stage
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korio.lang.Thread_sleep
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.myLog
import org.andstatus.game2048.view.ViewData
import org.andstatus.game2048.view.viewData


// TODO: Make some separate class for this...
private val isViewDataInitialized = korAtomic(false)
private val viewDataRef: KorAtomicRef<ViewData?> = korAtomic(null)
private val viewData: ViewData
    get() = if (isViewDataInitialized.value) viewDataRef.value
        ?: throw IllegalStateException("Value is not initialized yet")
    else throw IllegalStateException("Value is not initialized yet")

fun unsetGameView() {
    isViewDataInitialized.value = false
}

suspend fun Stage.initializeViewDataInTest(handler: suspend ViewData.() -> Unit = {}) {
    if (isViewDataInitialized.value) {
        viewData.handler()
    } else {
        viewData(stage, animateViews = false) {
            myLog("Initialized in test")
            viewDataRef.value = this
            isViewDataInitialized.value = true
            viewData.handler()
        }
        myLog("initializeViewDataInTest after 'viewData' function ended")
    }
}

fun ViewData.presentedPieces() = presenter.boardViews.blocksOnBoard.map { it.firstOrNull()?.piece }

fun ViewData.blocksAt(square: Square) = presenter.boardViews.getAll(square).map { it.piece }

fun ViewData.modelAndViews() =
    "Model:     " + presenter.model.gamePosition.pieces.mapIndexed { ind, piece ->
        ind.toString() + ":" + (piece?.text ?: "-")
    } +
            (if (presenter.model.history.currentGame.shortRecord.bookmarks.isNotEmpty())
                "  bookmarks: " + presenter.model.history.currentGame.shortRecord.bookmarks.size
            else "") +
            "\n" +
            "BoardViews:" + presenter.boardViews.blocksOnBoard.mapIndexed { ind, list ->
        ind.toString() + ":" + (if (list.isEmpty()) "-" else list.joinToString(transform = { it.piece.text }))
    }

fun ViewData.currentGameString(): String = "CurrentGame " + presenter.model.history.currentGame.plies.toLongString()

fun ViewData.historyString(): String = with(presenter.model.history) {
    "History: index:$historyIndex, moves:${currentGame.plies.size}"
}

fun ViewData.waitForMainViewShown(action: () -> Any? = { -> null }) {
    presenter.mainViewShown.value = false
    action()
    waitFor("Main view shown") { -> presenter.mainViewShown.value }
}

fun waitFor(message: String = "???", condition: () -> Boolean) {
    val stopWatch = Stopwatch().start()
    while (stopWatch.elapsed.seconds < 20) {
        if (condition()) return
        Thread_sleep(50)
    }
    throw AssertionError("Condition wasn't met after timeout: $message")
}
