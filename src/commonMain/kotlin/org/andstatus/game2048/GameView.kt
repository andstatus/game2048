package org.andstatus.game2048

import com.soywiz.korev.Key
import com.soywiz.korge.Korge
import com.soywiz.korge.html.Html
import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onKeyDown
import com.soywiz.korge.input.onSwipe
import com.soywiz.korge.ui.TextFormat
import com.soywiz.korge.ui.TextSkin
import com.soywiz.korge.ui.uiText
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.roundRect
import kotlin.properties.Delegates

private const val buttonPadding = 18.0
private const val appBarTopIndent = buttonPadding
const val buttonRadius = 5.0

class GameView(val gameStage: Stage, val animateViews: Boolean = true) {
    private val bgColor = Colors["#b9aea0"]
    private var buttonSize : Double = 0.0

    var presenter: Presenter by Delegates.notNull()

    private var score: Text by Delegates.notNull()
    private var bestScore: Text by Delegates.notNull()

    private var appLogo: Container by Delegates.notNull()
    private var playBackwardsButton: Container by Delegates.notNull()
    private var playButton: Container by Delegates.notNull()
    private var stopButton: Container by Delegates.notNull()
    private var toStartButton: Container by Delegates.notNull()
    private var toCurrentButton: Container by Delegates.notNull()
    private var undoButton: Container by Delegates.notNull()
    private var redoButton: Container by Delegates.notNull()
    private var restartButton: Container by Delegates.notNull()

    private var boardControls: Container by Delegates.notNull()

    companion object {
        suspend fun mainEntry() = Korge(width = 480, height = 680, title = "2048", bgcolor = Colors["#fdf7f0"]) {
            appEntry(this, true)
        }

        suspend fun appEntry(stage: Stage, animateViews: Boolean): GameView {
            loadSettings(stage)

            val view = GameView(stage, animateViews)
            font = resourcesVfs["clear_sans.fnt"].readBitmapFont()
            val allCellMargins = cellMargin * (settings.boardWidth + 1)
            cellSize = (stage.views.virtualWidth - allCellMargins - 2 * buttonPadding) / settings.boardWidth
            view.buttonSize = cellSize * 0.8
            boardWidth = cellSize * settings.boardWidth + allCellMargins
            leftIndent = (stage.views.virtualWidth - boardWidth) / 2
            topIndent = appBarTopIndent + view.buttonSize + buttonPadding + view.buttonSize + buttonPadding

            view.presenter = Presenter(view)
            view.setupAppBar()
            view.setupStaticViews()
            view.boardControls = view.setupControls()
            view.presenter.onAppEntry()
            return view
        }
    }

    private suspend fun setupAppBar() {
        var nextXPosition = leftIndent
        appLogo = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, color = RGBA(237, 196, 3))
            text("2048", cellSize * 0.4, Colors.WHITE, font) {
                centerOn(background)
            }
            position(nextXPosition, appBarTopIndent)
            onClick {
                presenter.onLogoClick()
            }
        }

        playBackwardsButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
            image(resourcesVfs["play_backwards.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(nextXPosition, appBarTopIndent)
            onClick {
                presenter.onPlayBackwardsClick()
            }
        }

        stopButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
            image(resourcesVfs["stop.png"].readBitmap()) {
                size(buttonSize * 0.4, buttonSize * 0.4)
                centerOn(background)
            }
            position(nextXPosition, appBarTopIndent)
            onClick {
                presenter.onStopClick()
            }
        }

        playButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
            image(resourcesVfs["play.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(nextXPosition, appBarTopIndent)
            onClick {
                presenter.onPlayClick()
            }
        }

        nextXPosition += buttonSize + buttonPadding
        toStartButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
            image(resourcesVfs["skip_previous.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(nextXPosition, appBarTopIndent)
            onClick {
                presenter.onToStartClick()
            }
        }

        toCurrentButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
            image(resourcesVfs["skip_next.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(nextXPosition, appBarTopIndent)
            onClick {
                presenter.onToCurrentClick()
            }
        }

        nextXPosition = leftIndent + boardWidth - buttonSize
        restartButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
            image(resourcesVfs["restart.png"].readBitmap()) {
                size(buttonSize * 0.8, buttonSize * 0.8)
                centerOn(background)
            }
            position(nextXPosition, appBarTopIndent)
            onClick {
                presenter.onRestartClick()
            }
        }

        nextXPosition -= buttonSize + buttonPadding
        redoButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
            image(resourcesVfs["redo.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(nextXPosition, appBarTopIndent)
            onClick {
                presenter.onRedoClick()
            }
        }

        nextXPosition -= buttonSize + buttonPadding
        undoButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
            image(resourcesVfs["undo.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(nextXPosition, appBarTopIndent)
            onClick {
                presenter.onUndoClick()
            }
        }
    }

    private fun setupStaticViews() {
        val bgBest = gameStage.roundRect(cellSize * 1.5, buttonSize, buttonRadius, color = bgColor) {
            position(leftIndent + boardWidth - cellSize * 1.5, appBarTopIndent + buttonSize + buttonPadding)
        }
        gameStage.text("BEST", cellSize * 0.25, RGBA(239, 226, 210), font) {
            centerXOn(bgBest)
            alignTopToTopOf(bgBest, buttonRadius)
        }
        bestScore = gameStage.text("", cellSize * 0.5, Colors.WHITE, font) {
            setTextBounds(Rectangle(0.0, 0.0, bgBest.width, cellSize - 24.0))
            format = format.copy(align = Html.Alignment.MIDDLE_CENTER)
            alignTopToTopOf(bgBest, 12.0)
            centerXOn(bgBest)
        }

        val bgScore = gameStage.roundRect(cellSize * 1.5, buttonSize, buttonRadius, color = bgColor) {
            alignRightToLeftOf(bgBest, buttonPadding)
            alignTopToTopOf(bgBest)
        }
        gameStage.text("SCORE", cellSize * 0.25, RGBA(239, 226, 210), font) {
            centerXOn(bgScore)
            alignTopToTopOf(bgScore, buttonRadius)
        }
        score = gameStage.text("", cellSize * 0.5, Colors.WHITE, font) {
            setTextBounds(Rectangle(0.0, 0.0, bgScore.width, cellSize - 24.0))
            format = format.copy(align = Html.Alignment.MIDDLE_CENTER)
            centerXOn(bgScore)
            alignTopToTopOf(bgScore, 12.0)
        }

        gameStage.roundRect(boardWidth, boardWidth, buttonRadius, color = bgColor) {
            position(leftIndent, topIndent)
        }
        gameStage.graphics {
            position(leftIndent, topIndent)
            fill(Colors["#cec0b2"]) {
                for (x in 0 until settings.boardWidth) {
                    for (y in 0 until settings.boardHeight) {
                        roundRect(cellMargin + (cellMargin + cellSize) * x, cellMargin + (cellMargin + cellSize) * y,
                                cellSize, cellSize, buttonRadius)
                    }
                }
            }
        }
    }

    private fun setupControls(): Container {
        val boardView = SolidRect(boardWidth, boardWidth, Colors.TRANSPARENT_WHITE)
                .addTo(gameStage).position(leftIndent, topIndent)

        boardView.onSwipe(20.0) {
            when (it.direction) {
                SwipeDirection.LEFT -> presenter.userMove(PlayerMoveEnum.LEFT)
                SwipeDirection.RIGHT -> presenter.userMove(PlayerMoveEnum.RIGHT)
                SwipeDirection.TOP -> presenter.userMove(PlayerMoveEnum.UP)
                SwipeDirection.BOTTOM -> presenter.userMove(PlayerMoveEnum.DOWN)
            }
        }

        boardView.onKeyDown {
            when (it.key) {
                Key.LEFT -> presenter.userMove(PlayerMoveEnum.LEFT)
                Key.RIGHT -> presenter.userMove(PlayerMoveEnum.RIGHT)
                Key.UP -> presenter.userMove(PlayerMoveEnum.UP)
                Key.DOWN -> presenter.userMove(PlayerMoveEnum.DOWN)
                else -> Unit
            }
        }

        return boardView
    }

    fun showControls(buttonsToShow: List<ButtonsEnum>) {
        val show = showButton(buttonsToShow)
        show(appLogo, ButtonsEnum.APP_LOGO)
        show(playBackwardsButton, ButtonsEnum.PLAY_BACKWARDS)
        show(playButton, ButtonsEnum.PLAY)
        show(stopButton, ButtonsEnum.STOP)
        show(toStartButton, ButtonsEnum.TO_START)
        show(toCurrentButton, ButtonsEnum.TO_CURRENT)
        show(undoButton, ButtonsEnum.UNDO)
        show(redoButton, ButtonsEnum.REDO)
        show(restartButton, ButtonsEnum.RESTART)

        bestScore.text = presenter.bestScore.toString()
        score.text = presenter.score.toString()

        // Ensure the view is on the top to receive onSwipe events
        boardControls.addTo(gameStage)
    }

    private fun showButton(buttonsToShow: List<ButtonsEnum>) = { button: Container, tag: ButtonsEnum ->
        if (buttonsToShow.contains(tag)) {
            button.addTo(gameStage)
        } else {
            button.removeFromParent()
        }
    }

    fun showGameOver(): Container = gameStage.container {
        val format = TextFormat(RGBA(0, 0, 0), 40, Html.FontFace.Bitmap(font))
        val skin = TextSkin(
                normal = format,
                over = format.copy(RGBA(90, 90, 90)),
                down = format.copy(RGBA(120, 120, 120))
        )

        fun removeMe() {
            removeFromParent()
            presenter.restart()
        }

        position(leftIndent, topIndent)

        graphics {
            fill(Colors.WHITE, 0.2) {
                roundRect(0.0, 0.0, boardWidth, boardWidth, buttonRadius)
            }
        }
        text("Game Over", 60.0, Colors.BLACK, font) {
            centerBetween(0.0, 0.0, boardWidth, boardWidth)
            y -= 60
        }
        uiText("Try again", 120.0, 35.0, skin) {
            centerBetween(0.0, 0.0, boardWidth, boardWidth)
            y += 20
            onClick { removeMe() }
        }
        onKeyDown {
            when (it.key) {
                Key.ENTER, Key.SPACE -> removeMe()
                else -> Unit
            }
        }
    }
}