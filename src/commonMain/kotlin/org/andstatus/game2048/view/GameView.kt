package org.andstatus.game2048.view

import com.soywiz.korev.PauseEvent
import com.soywiz.korev.addEventListener
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onDown
import com.soywiz.korge.input.onOver
import com.soywiz.korge.input.onUp
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.position
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.Point
import org.andstatus.game2048.defaultLanguage
import org.andstatus.game2048.defaultPortraitGameWindowSize
import org.andstatus.game2048.gameWindowSize
import org.andstatus.game2048.loadSettings
import org.andstatus.game2048.myLog
import org.andstatus.game2048.myMeasured
import org.andstatus.game2048.presenter.Presenter
import org.andstatus.game2048.settings
import org.andstatus.game2048.view.AppBar.Companion.setupAppBar
import kotlin.properties.Delegates

class GameView(val gameStage: Stage, val stringResources: StringResources, val animateViews: Boolean = true) {
    internal val gameViewLeft: Int
    internal val gameViewTop: Int
    internal val gameViewWidth: Int
    internal val gameViewHeight: Int
    internal val gameScale: Double

    internal val buttonPadding: Double

    internal val buttonSize : Double
    var font: Font by Delegates.notNull()
    var gameColors: ColorTheme by Delegates.notNull()

    var presenter: Presenter by Delegates.notNull()

    internal var scoreBar: ScoreBar by Delegates.notNull()

    private val pointNONE = Point(0, 0)
    private var buttonPointClicked = pointNONE
    internal val buttonXs: List<Double>
    internal val buttonYs: List<Double>
    internal val duplicateKeyPressFilter = DuplicateKeyPressFilter()

    private var appBar: AppBar by Delegates.notNull()

    var boardView: BoardView by Delegates.notNull()

    companion object {
        suspend fun initialize(stage: Stage, animateViews: Boolean): GameView {
            loadSettings(stage)
            val stringResources = StringResources.load(defaultLanguage)
            stage.gameWindow.title = stringResources.text("app_name")

            val view = GameView(stage, stringResources, animateViews)
            view.font = myMeasured("Font loaded") {
                resourcesVfs["assets/clear_sans.fnt"].readBitmapFont()
            }
            view.gameColors = ColorTheme.load(stage)
            view.presenter = myMeasured("Presenter created") { Presenter(view) }
            view.setupStageBackground()
            view.appBar = view.setupAppBar()
            view.scoreBar = view.setupScoreBar()
            view.boardView = BoardView(view)

            view.presenter.onAppEntry()
            stage.gameWindow.addEventListener<PauseEvent> { view.presenter.onPauseEvent() }
            myLog("GameView initialized")
            return view
        }
    }

    init {
        if (gameStage.views.virtualWidth < gameStage.views.virtualHeight) {
            if ( gameStage.views.virtualHeight / gameStage.views.virtualWidth >
                    defaultPortraitGameWindowSize.height / defaultPortraitGameWindowSize.width) {
                gameViewWidth = gameStage.views.virtualWidth
                gameViewHeight = gameViewWidth * defaultPortraitGameWindowSize.height / defaultPortraitGameWindowSize.width
                gameViewLeft = 0
                gameViewTop = (gameStage.views.virtualHeight - gameViewHeight) / 2
            } else {
                gameViewWidth = gameStage.views.virtualHeight * defaultPortraitGameWindowSize.width / defaultPortraitGameWindowSize.height
                gameViewHeight = gameStage.views.virtualHeight
                gameViewLeft = (gameStage.views.virtualWidth - gameViewWidth) / 2
                gameViewTop = 0
            }
        } else {
            gameViewWidth = gameStage.views.virtualHeight * defaultPortraitGameWindowSize.width / defaultPortraitGameWindowSize.height
            gameViewHeight = gameStage.views.virtualHeight
            gameViewLeft = (gameStage.views.virtualWidth - gameViewWidth) / 2
            gameViewTop = 0
        }
        gameScale = gameViewHeight.toDouble() / defaultPortraitGameWindowSize.height
        buttonPadding = 27 * gameScale

        cellMargin = 15 * gameScale
        buttonRadius = 8 * gameScale

        val allCellMargins = cellMargin * (settings.boardWidth + 1)
        cellSize = (gameViewWidth - allCellMargins - 2 * buttonPadding) / settings.boardWidth
        buttonSize = (gameViewWidth - buttonPadding * 6) / 5
        boardWidth = cellSize * settings.boardWidth + allCellMargins
        boardLeft = gameViewLeft + (gameViewWidth - boardWidth) / 2

        buttonXs = (0 .. 4).fold(emptyList()) { acc, i ->
            acc + (boardLeft + i * (buttonSize + buttonPadding))
        }
        buttonYs = (0 .. 8).fold(emptyList()) { acc, i ->
            acc + (buttonPadding + i * (buttonSize + buttonPadding))
        }
        boardTop = buttonYs[3]

        myLog(
            "Window:${gameStage.coroutineContext.gameWindowSize}" +
                    " -> Virtual:${gameStage.views.virtualWidth}x${gameStage.views.virtualHeight}" +
                    " -> Game:${gameViewWidth}x$gameViewHeight, top:$gameViewTop, left:$gameViewLeft"
        )
    }

    private fun setupStageBackground() {
        gameStage.solidRect(gameViewWidth, gameViewHeight, color = gameColors.stageBackground) {
            position(gameViewLeft, gameViewTop)
        }
    }

    /** Workaround for the bug: https://github.com/korlibs/korge-next/issues/56 */
    fun Container.customOnClick(handler: () -> Unit) {
        if (OS.isAndroid) {
            onOver {
                duplicateKeyPressFilter.onSwipeOrOver { myLog("onOver $pos") }
            }
            onDown {
                myLog("onDown $pos")
                buttonPointClicked = pos.copy()
            }
            onClick { myLog("onClick $pos}") }
            onUp {
                val clicked = buttonPointClicked == pos
                myLog("onUp $pos " + if (clicked) "- clicked" else "<- $buttonPointClicked")
                buttonPointClicked = pointNONE
                if (clicked) handler()
            }
        } else {
            onClick {
                handler()
            }
        }
    }

    fun showControls(appBarButtonsToShow: List<AppBarButtonsEnum>, playSpeed: Int) {
        appBar.show(appBarButtonsToShow)
        scoreBar.show(playSpeed)
        boardView.setOnTop()
    }

}