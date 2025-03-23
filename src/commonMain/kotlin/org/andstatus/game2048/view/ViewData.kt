package org.andstatus.game2048.view

import korlibs.event.PauseEvent
import korlibs.image.font.Font
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.io.lang.Closeable
import korlibs.korge.animate.Animator
import korlibs.korge.animate.moveTo
import korlibs.korge.input.singleTouch
import korlibs.korge.view.Container
import korlibs.korge.view.Stage
import korlibs.korge.view.View
import korlibs.korge.view.addTo
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.math.interpolation.Easing
import korlibs.platform.Platform
import korlibs.time.Stopwatch
import korlibs.time.TimeSpan
import korlibs.time.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.andstatus.game2048.MyContext
import org.andstatus.game2048.defaultLanguage
import org.andstatus.game2048.gameIsLoading
import org.andstatus.game2048.loadFont
import org.andstatus.game2048.model.History
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.myLog
import org.andstatus.game2048.myMeasured
import org.andstatus.game2048.presenter.Presenter
import org.andstatus.game2048.view.MainView.Companion.setupMainView
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

/** @author yvolk@yurivolkov.com */
suspend fun viewData(stage: Stage): ViewData = coroutineScope {
    stage.removeChildren()
    val quick = ViewDataQuick(stage)
    val splashDefault = stage.splashScreen(quick, ColorThemeEnum.deviceDefault(stage))
    waitForGameLoading()
    val strings = async { StringResources.load(defaultLanguage) }
    val font = async { loadFont(strings.await()) }
    val myContext = myMeasured("Loading settings") { MyContext.load(stage) }
    val history: Deferred<History> = async {
        History.load(myContext).also {
            it.loadRecentGames()
        }
    }
    val gameColors = async { ColorTheme.load(stage, myContext) }

    val splashThemed = if (myContext.settings.colorThemeEnum == ColorThemeEnum.deviceDefault(stage))
        splashDefault else stage.splashScreen(quick, myContext.settings.colorThemeEnum)
    stage.solidRect(
        stage.views.virtualWidth, stage.views.virtualHeight,
        color = gameColors.await().stageBackground
    )

    splashThemed.addTo(stage)
    if (splashThemed != splashDefault) {
        splashDefault.removeFromParent()
    }

    if (!Platform.isAndroid) launch {
        // We set window title in Android via AndroidManifest.xml
        stage.gameWindow.title = strings.await().text("app_name")
    }
    val historyLoaded: History = history.await().also { history1 ->
        if (history1.currentGame.shortRecord.board.boardSize != myContext.settings.boardSize) {
            myContext.update {
                it.copy(boardSize = history1.currentGame.shortRecord.board.boardSize)
            }
        }
    }

    val view = ViewData(quick, myContext, font.await(), strings.await(), gameColors.await())
    view.presenter = myMeasured("Presenter${view.id} created") { Presenter(view, historyLoaded) }
    view.mainView = myMeasured("MainView${view.id} created") { view.setupMainView(this) }

    splashThemed.removeFromParent()
    view.presenter.onAppEntry()
    view.gameStage.gameWindow.onEvent(PauseEvent) { view.presenter.onPauseEvent() }
        .also { view.closeables.add(it) }
    myLog("GameView${view.id} initialized")
    view
}

suspend fun waitForGameLoading() {
    if (!gameIsLoading.value) return

    myLog("Waiting for game loading to finish...")
    val stopWatch = Stopwatch().start()
    while (gameIsLoading.value && stopWatch.elapsed.minutes < 5) {
        delay(100)
    }
    if (gameIsLoading.value) myLog("Timeout waiting for game loading")
    else myLog("Game loading finished")

}

class ViewData(
    viewDataQuick: ViewDataQuick,
    val myContext: MyContext,
    val font: Font,
    val stringResources: StringResources,
    val gameColors: ColorTheme
) : ViewDataBase by viewDataQuick, Closeable {
    val korgeCoroutineScope: CoroutineScope get() = gameStage.views
    val korgeCoroutineContext: CoroutineContext get() = gameStage.views.coroutineContext

    val boardSize: BoardSizeEnum get() = myContext.settings.boardSize
    val cellSize: Float = (
        (if (isPortrait) gameViewWidth else gameViewWidth / 2) -
            cellMargin * (boardSize.width + 1) - 2 * buttonMargin) / boardSize.width
    val boardWidth: Float = cellSize * boardSize.width + cellMargin * (boardSize.width + 1)

    var presenter: Presenter by Delegates.notNull()
    var mainView: MainView by Delegates.notNull()

    val closeables = mutableListOf<Closeable>()
    private val closedRef = korAtomic(false)
    val closed get() = closedRef.value

    fun reInitialize(handler: suspend ViewData.() -> Unit = {}) {
        closedRef.value = true
        this.close()
        korgeCoroutineScope.launch {
            viewData(gameStage)
            handler()
        }
    }

    fun Container.customOnClick(handler: () -> Unit) = singleTouch {
        tap {
            handler()
        }
    }

    fun Container.position(square: Square) {
        position(square.positionX(), square.positionY())
    }

    fun Animator.moveTo(view: View, square: Square, time: TimeSpan, easing: Easing) {
        this.moveTo(view, square.positionX(), square.positionY(), time, easing)
    }

    private fun Square.positionX() = cellMargin + (cellSize + cellMargin) * x
    private fun Square.positionY() = cellMargin + (cellSize + cellMargin) * y

    override fun close() {
        closeables.forEach { it.close() }
    }
}
