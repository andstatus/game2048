package org.andstatus.game2048.view

import com.soywiz.klock.TimeSpan
import com.soywiz.korev.PauseEvent
import com.soywiz.korev.addEventListener
import com.soywiz.korge.animate.Animator
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.View
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.position
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.font.Font
import com.soywiz.korio.util.OS
import com.soywiz.korma.interpolation.Easing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.defaultLanguage
import org.andstatus.game2048.loadFont
import org.andstatus.game2048.model.History
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.myLog
import org.andstatus.game2048.myMeasured
import org.andstatus.game2048.presenter.Presenter
import org.andstatus.game2048.view.AppBar.Companion.setupAppBar
import kotlin.properties.Delegates

/** @author yvolk@yurivolkov.com */
class GameView(gameViewQuick: GameViewQuick,
               val settings: Settings,
               val font: Font,
               val stringResources: StringResources,
               val gameColors: ColorTheme): GameViewBase by gameViewQuick {

    val cellSize: Double = (gameViewWidth - cellMargin * (settings.boardWidth + 1) - 2 * buttonPadding) / settings.boardWidth
    val boardWidth: Double = cellSize * settings.boardWidth + cellMargin * (settings.boardWidth + 1)

    var presenter: Presenter by Delegates.notNull()
    private var appBar: AppBar by Delegates.notNull()
    var scoreBar: ScoreBar by Delegates.notNull()
    var boardView: BoardView by Delegates.notNull()

    companion object {
        suspend fun Stage.initializeGameView(animateViews: Boolean, handler: GameView.() -> Unit = {}) {
            val viewQuick = GameViewQuick(stage, animateViews)

            if (OS.isWindows) {
                // This is faster for Android emulator
                parallelLoad(this, viewQuick, handler)
            } else {
                // This is faster for jvmRun and Android devices;
                // and this doesn't work on Windows, at all.
                launch(Dispatchers.Default) {
                    parallelLoad(this, viewQuick, handler)
                }
            }
        }

        private suspend fun parallelLoad(
            coroutineScope: CoroutineScope,
            quick: GameViewQuick,
            handler: GameView.() -> Unit
        ) {
            val splash = quick.gameStage.splashScreen()
            val font = coroutineScope.async { loadFont() }
            val settings = coroutineScope.async { Settings.load(quick.gameStage) }
            val history = coroutineScope.async { History.load(settings.await()) }
            val gameColors = coroutineScope.async { ColorTheme.load(quick.gameStage, settings.await()) }

            val view = GameView(
                quick, settings.await(), font.await(), StringResources.load(defaultLanguage), gameColors.await()
            )
            quick.gameStage.solidRect(quick.gameStage.views.virtualWidth, quick.gameStage.views.virtualHeight,
                    color = view.gameColors.stageBackground)
            splash.addTo(quick.gameStage)

            if (!OS.isAndroid) {
                // We set window title in Android via AndroidManifest.xml
                quick.gameStage.gameWindow.title = view.stringResources.text("app_name")
            }

            view.presenter = myMeasured("Presenter created") { Presenter(view, history.await()) }
            val appBar = coroutineScope.async { view.setupAppBar() }
            val scoreBar = coroutineScope.async { view.setupScoreBar() }
            val boardView = coroutineScope.async { BoardView(view) }

            view.appBar = appBar.await()
            view.scoreBar = scoreBar.await()
            view.boardView = boardView.await()

            splash.removeFromParent()
            view.presenter.onAppEntry()
            quick.gameStage.gameWindow.addEventListener<PauseEvent> { view.presenter.onPauseEvent() }
            myLog("GameView initialized")
            view.handler()
        }
    }

    suspend fun reInitialize(handler: GameView.() -> Unit = {}) {
        gameStage.removeChildren()
        gameStage.initializeGameView(animateViews, handler)
    }

    /** Workaround for the bug: https://github.com/korlibs/korge-next/issues/56 */
    fun Container.customOnClick(handler: () -> Unit) = duplicateKeyPressFilter.apply {
        customOnClick(handler)
    }

    fun Container.position(square: Square) {
        position(square.positionX(), square.positionY())
    }

    fun Animator.moveTo(view: View, square: Square, time: TimeSpan, easing: Easing) {
        view.moveTo(square.positionX(), square.positionY(), time, easing)
    }

    private fun Square.positionX() = cellMargin + (cellSize + cellMargin) * x
    private fun Square.positionY() = cellMargin + (cellSize + cellMargin) * y

    fun showControls(appBarButtonsToShow: List<AppBarButtonsEnum>, playSpeed: Int) {
        appBar.show(appBarButtonsToShow)
        scoreBar.show(playSpeed)
        boardView.setOnTop()
    }

}