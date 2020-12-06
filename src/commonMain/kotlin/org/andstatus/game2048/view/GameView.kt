package org.andstatus.game2048.view

import com.soywiz.korev.Key
import com.soywiz.korev.PauseEvent
import com.soywiz.korev.addEventListener
import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onDown
import com.soywiz.korge.input.onOver
import com.soywiz.korge.input.onSwipe
import com.soywiz.korge.input.onUp
import com.soywiz.korge.ui.TextFormat
import com.soywiz.korge.ui.TextSkin
import com.soywiz.korge.ui.uiText
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.centerOn
import com.soywiz.korge.view.centerXBetween
import com.soywiz.korge.view.centerXOn
import com.soywiz.korge.view.container
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.positionX
import com.soywiz.korge.view.positionY
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.size
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.vector.roundRect
import org.andstatus.game2048.defaultLanguage
import org.andstatus.game2048.defaultPortraitGameWindowSize
import org.andstatus.game2048.defaultTextSize
import org.andstatus.game2048.gameWindowSize
import org.andstatus.game2048.loadSettings
import org.andstatus.game2048.myLog
import org.andstatus.game2048.myMeasured
import org.andstatus.game2048.presenter.Presenter
import org.andstatus.game2048.settings
import kotlin.math.abs
import kotlin.properties.Delegates

class GameView(val gameStage: Stage, val stringResources: StringResources, val animateViews: Boolean = true) {
    internal val gameViewLeft: Int
    internal val gameViewTop: Int
    internal val gameViewWidth: Int
    internal val gameViewHeight: Int
    private val gameScale: Double

    internal val buttonPadding: Double

    internal val buttonSize : Double
    var font: Font by Delegates.notNull()
    var gameColors: ColorTheme by Delegates.notNull()

    var presenter: Presenter by Delegates.notNull()

    internal var gameTime: Text by Delegates.notNull()
    private var usersMoveNumber: Text by Delegates.notNull()
    private var score: Text by Delegates.notNull()
    private var bestScore: Text by Delegates.notNull()

    private val pointNONE = Point(0, 0)
    private var buttonPointClicked = pointNONE
    internal val buttonXs: List<Double>
    internal val buttonYs: List<Double>
    internal val duplicateKeyPressFilter = DuplicateKeyPressFilter()

    private var appBar: AppBar by Delegates.notNull()

    private var boardControls: SolidRect by Delegates.notNull()

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
            view.appBar = AppBar(view).apply { view.setupAppBar() }
            view.setupScoreBar()
            view.boardControls = view.setupBoardControls()

            myLog("onAppEntry")
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
        gameStage.roundRect(boardWidth, boardWidth, buttonRadius, fill = gameColors.buttonBackground) {
            position(boardLeft, boardTop)
        }
        gameStage.graphics {
            position(boardLeft, boardTop)
            fill(gameColors.cellBackground) {
                for (x in 0 until settings.boardWidth) {
                    for (y in 0 until settings.boardHeight) {
                        roundRect(
                            cellMargin + (cellMargin + cellSize) * x, cellMargin + (cellMargin + cellSize) * y,
                            cellSize, cellSize, buttonRadius
                        )
                    }
                }
            }
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

    private fun setupScoreBar() {
        val scoreButtonWidth = (boardWidth - 2 * buttonPadding) / 3
        val scoreButtonTop = buttonYs[2]
        val textYPadding = 28 * gameScale
        val scoreLabelSize = cellSize * 0.30
        val scoreTextSize = cellSize * 0.5

        gameTime = gameStage.text("00:00:00", scoreLabelSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
            positionX(boardLeft + scoreButtonWidth / 2)
            positionY(scoreButtonTop + textYPadding)
        }
        usersMoveNumber = gameStage.text("", scoreTextSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(gameTime)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }.addTo(gameStage)

        val bgScore = gameStage.roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
            position(boardLeft + (scoreButtonWidth + buttonPadding), scoreButtonTop)
        }
        gameStage.text(stringResources.text("score_upper"), scoreLabelSize, gameColors.buttonLabelText, font,
            TextAlignment.MIDDLE_CENTER
        ) {
            positionX(bgScore.pos.x + scoreButtonWidth / 2)
            positionY(scoreButtonTop + textYPadding)
        }
        score = gameStage.text("", scoreTextSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(bgScore)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }

        val bgBest = gameStage.roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
            position(boardLeft + (scoreButtonWidth + buttonPadding) * 2, scoreButtonTop)
        }
        gameStage.text(stringResources.text("best"), scoreLabelSize, gameColors.buttonLabelText, font,
            TextAlignment.MIDDLE_CENTER
        ) {
            positionX(bgBest.pos.x + scoreButtonWidth / 2)
            positionY(scoreButtonTop + textYPadding)
        }
        bestScore = gameStage.text("", scoreTextSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(bgBest)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }
    }

    private fun setupBoardControls(): SolidRect {
        val controlsArea = SolidRect(boardWidth, boardWidth + buttonSize + buttonPadding, gameColors.transparent)
                .addTo(gameStage).position(boardLeft, boardTop)

        controlsArea.onSwipe(20.0) {
            duplicateKeyPressFilter.onSwipeOrOver {
                presenter.onSwipe(it.direction)
            }
        }

        controlsArea.addUpdater {
            val ifKey = { key: Key, action: () -> Unit ->
                if (gameStage.views.input.keys[key]) {
                    duplicateKeyPressFilter.onPress(key, action)
                }
            }
            ifKey(Key.LEFT) { presenter.onSwipe(SwipeDirection.LEFT) }
            ifKey(Key.RIGHT) { presenter.onSwipe(SwipeDirection.RIGHT) }
            ifKey(Key.UP) { presenter.onSwipe(SwipeDirection.TOP) }
            ifKey(Key.DOWN) { presenter.onSwipe(SwipeDirection.BOTTOM) }
            ifKey(Key.SPACE) { presenter.onPauseClick() }
            ifKey(Key.M) { presenter.onGameMenuClick() }
            ifKey(Key.BACKSPACE) {
                presenter.onCloseGameWindowClick()
            }
        }

        return controlsArea
    }

    fun showControls(appBarButtonsToShow: List<AppBarButtonsEnum>, playSpeed: Int) {
        appBar.show(appBarButtonsToShow)

        usersMoveNumber.text = presenter.model.usersMoveNumber.toString() +
                (if (playSpeed < 0) " «" else "") +
                (if (playSpeed > 0) " »" else "") +
                (if (playSpeed != 0) abs(playSpeed) else "")
        bestScore.text = presenter.bestScore.toString()
        score.text = presenter.score.toString()

        // Ensure the view is on the top to receive onSwipe events
        boardControls.addTo(gameStage)
    }

    fun showGameOver(): Container = gameStage.container {
        val window = this
        val format = TextFormat(gameColors.labelText, defaultTextSize.toInt(), font)
        val skin = TextSkin(
            normal = format,
            over = format.copy(gameColors.labelTextOver),
            down = format.copy(gameColors.labelTextDown)
        )

        position(boardLeft, boardTop)

        graphics {
            fill(gameColors.gameOverBackground) {
                roundRect(0.0, 0.0, boardWidth, boardWidth, buttonRadius)
            }
        }
        text(stringResources.text("game_over"),
            defaultTextSize, gameColors.labelText, font,
            TextAlignment.MIDDLE_CENTER
        ) {
            position(boardWidth / 2, (boardWidth - textSize) / 2)
        }
        uiText(stringResources.text("try_again"), 120.0, 35.0, skin) {
            centerXBetween(0.0, boardWidth)
            positionY((boardWidth + textSize) / 2)
            customOnClick {
                window.removeFromParent()
                presenter.restart()
            }
        }

        addUpdater {
            duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                window.removeFromParent()
                presenter.restart()
            }
        }
    }

}