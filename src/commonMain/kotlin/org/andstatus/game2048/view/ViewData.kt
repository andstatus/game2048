package org.andstatus.game2048.view

import com.soywiz.klock.Stopwatch
import com.soywiz.klock.TimeSpan
import com.soywiz.korev.PauseEvent
import com.soywiz.korev.ResumeEvent
import com.soywiz.korev.addEventListener
import com.soywiz.korge.animate.Animator
import com.soywiz.korge.input.onClick
import com.soywiz.korge.view.*
import com.soywiz.korim.font.Font
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.util.OS
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.*
import org.andstatus.game2048.*
import org.andstatus.game2048.model.History
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.presenter.Presenter
import org.andstatus.game2048.view.MainView.Companion.setupMainView
import kotlin.properties.Delegates

/** @author yvolk@yurivolkov.com */
suspend fun viewData(stage: Stage, animateViews: Boolean, handler: suspend ViewData.() -> Unit = {}) {
    stage.removeChildren()
    coroutineScope {
        val outerScope = this
        val handlerInOuterScope: suspend ViewData.() -> Unit = {
            outerScope.launch { handler() }
        }

        val multithreadedScope: CoroutineScope = if (OS.isNative) outerScope else
            CoroutineScope(outerScope.coroutineContext + Dispatchers.Default)
        multithreadedScope.initialize(stage, animateViews, handlerInOuterScope)
    }
}

private fun CoroutineScope.initialize(stage: Stage, animateViews: Boolean, handler: suspend ViewData.() -> Unit = {}) = launch {
    val quick = ViewDataQuick(stage, animateViews)
    val splashDefault = stage.splashScreen(quick, ColorThemeEnum.deviceDefault(stage))
    waitForGameLoading()
    val strings = async { StringResources.load(defaultLanguage) }
    val font = async { loadFont(strings.await()) }
    val settings = async { Settings.load(stage) }
    val history = async { History.load(settings.await()) }
    launch {
        history.await().loadRecentGames()
    }
    val gameColors = async { ColorTheme.load(stage, settings.await()) }

    val splashThemed = if (settings.await().colorThemeEnum == ColorThemeEnum.deviceDefault(stage))
        splashDefault else stage.splashScreen(quick, settings.await().colorThemeEnum)
    stage.solidRect(stage.views.virtualWidth, stage.views.virtualHeight,
            color = gameColors.await().stageBackground)

    splashThemed.addTo(stage)
    if (splashThemed != splashDefault) {
        splashDefault.removeFromParent()
    }

    if (!OS.isAndroid) launch {
        // We set window title in Android via AndroidManifest.xml
        stage.gameWindow.title = strings.await().text("app_name")
    }

    val view = ViewData(quick, settings.await(), font.await(), strings.await(), gameColors.await())
    view.presenter = myMeasured("Presenter${view.id} created") { Presenter(view, history.await()) }
    view.mainView = myMeasured("MainView${view.id} created") { view.setupMainView(this) }

    splashThemed.removeFromParent()
    view.presenter.onAppEntry()
    view.gameStage.gameWindow.addEventListener<PauseEvent> { view.presenter.onPauseEvent() }
            .also { view.closeables.add(it) }
    view.gameStage.gameWindow.addEventListener<ResumeEvent> { view.presenter.onResumeEvent() }
            .also { view.closeables.add(it) }
    myLog("GameView${view.id} initialized")
    view.handler()
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

class ViewData(viewDataQuick: ViewDataQuick,
               val settings: Settings,
               val font: Font,
               val stringResources: StringResources,
               val gameColors: ColorTheme): ViewDataBase by viewDataQuick, Closeable {

    val cellSize: Double = (gameViewWidth - cellMargin * (settings.boardWidth + 1) - 2 * buttonMargin) / settings.boardWidth
    val boardWidth: Double = cellSize * settings.boardWidth + cellMargin * (settings.boardWidth + 1)

    var presenter: Presenter by Delegates.notNull()
    var mainView: MainView by Delegates.notNull()

    val closeables = mutableListOf<Closeable>()
    var closed = false

    suspend fun reInitialize(handler: suspend ViewData.() -> Unit = {}) {
        closed = true
        this.close()
        viewData(gameStage, animateViews, handler)
    }

    fun Container.customOnClick(handler: () -> Unit) = onClick {
        handler()
    }

    fun Container.position(square: Square) {
        position(square.positionX(), square.positionY())
    }

    fun Animator.moveTo(view: View, square: Square, time: TimeSpan, easing: Easing) {
        view.moveTo(square.positionX(), square.positionY(), time, easing)
    }

    private fun Square.positionX() = cellMargin + (cellSize + cellMargin) * x
    private fun Square.positionY() = cellMargin + (cellSize + cellMargin) * y

    override fun close() {
        closeables.forEach { it.close() }
    }
}
