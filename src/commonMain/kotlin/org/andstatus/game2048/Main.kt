package org.andstatus.game2048

import com.soywiz.klock.DateTime
import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korev.Key
import com.soywiz.korge.Korge
import com.soywiz.korge.html.Html
import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onKeyDown
import com.soywiz.korge.input.onSwipe
import com.soywiz.korge.service.storage.NativeStorage
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
private val bgColor = Colors["#b9aea0"]
const val buttonRadius = 5.0
private var buttonSize : Double = 0.0
private var boardControls: Container by Delegates.notNull()

var presenter: Presenter by Delegates.notNull()

suspend fun main() = Korge(width = 480, height = 680, title = "2048", bgcolor = Colors["#fdf7f0"]) {
    gameEntry(this)
}

suspend fun gameEntry(stage: Stage) {
    loadSettings(getStorage(stage))

    font = resourcesVfs["clear_sans.fnt"].readBitmapFont()
    val allCellMargins = cellMargin * (settings.boardWidth + 1)
    cellSize = (stage.views.virtualWidth - allCellMargins - 2 * buttonPadding) / settings.boardWidth
    buttonSize = cellSize * 0.8
    boardWidth = cellSize * settings.boardWidth + allCellMargins
    leftIndent = (stage.views.virtualWidth - boardWidth) / 2
    topIndent = appBarTopIndent + buttonSize + buttonPadding + buttonSize + buttonPadding

    presenter = Presenter(stage)
    setupAppBar(stage)
    setupStaticViews(stage)
    boardControls = setupControls(stage)

    presenter.firstMove()
}

private fun getStorage(stage: Stage): NativeStorage {
    val storage = NativeStorage(stage.views)
    val keyOpened = "opened"

    Console.log("Storage: $storage" +
            (storage.getOrNull(keyOpened)?.let { "\n last opened: $it" } ?: "\n storage is new"))
    storage[keyOpened] = DateTime.now().toString()
    return storage
}

private var undoButton: Container by Delegates.notNull()
private var redoButton: Container by Delegates.notNull()
private suspend fun setupAppBar(stage: Stage) {
    val bgLogo = stage.roundRect(buttonSize, buttonSize, buttonRadius, color = RGBA(237, 196, 3)) {
        position(leftIndent, appBarTopIndent)
    }
    stage.text("2048", cellSize * 0.4, Colors.WHITE, font).centerOn(bgLogo)

    var nextXPosition = leftIndent + boardWidth - buttonSize
    stage.container {
        val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
        image(resourcesVfs["restart.png"].readBitmap()) {
            size(buttonSize * 0.8, buttonSize * 0.8)
            centerOn(background)
        }
        position(nextXPosition, appBarTopIndent)
        nextXPosition -= buttonSize + buttonPadding
        onClick {
            presenter.restart()
        }
    }

    redoButton = Container().apply {
        val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
        image(resourcesVfs["redo.png"].readBitmap()) {
            size(buttonSize * 0.6, buttonSize * 0.6)
            centerOn(background)
        }
        position(nextXPosition, appBarTopIndent)
        nextXPosition -= buttonSize + buttonPadding
        onClick {
            presenter.redo()
        }
    }

    undoButton = Container().apply {
        val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
        image(resourcesVfs["undo.png"].readBitmap()) {
            size(buttonSize * 0.6, buttonSize * 0.6)
            centerOn(background)
        }
        position(nextXPosition, appBarTopIndent)
        nextXPosition -= buttonSize + buttonPadding
        onClick {
            presenter.undo()
        }
    }

    showAppBar(stage)
}

private fun showAppBar(stage: Stage) {
    if (presenter.canRedo()) {
        redoButton.addTo(stage)
    } else {
        redoButton.removeFromParent()
    }

    if (presenter.canUndo()) {
        undoButton.addTo(stage)
    } else {
        undoButton.removeFromParent()
    }
}

private fun setupStaticViews(stage: Stage) {
    val bgBest = stage.roundRect(cellSize * 1.5, buttonSize, buttonRadius, color = bgColor) {
        position(leftIndent + boardWidth - cellSize * 1.5, appBarTopIndent + buttonSize + buttonPadding)
    }
    stage.text("BEST", cellSize * 0.25, RGBA(239, 226, 210), font) {
        centerXOn(bgBest)
        alignTopToTopOf(bgBest, buttonRadius)
    }
    stage.text("", cellSize * 0.5, Colors.WHITE, font) {
        setTextBounds(Rectangle(0.0, 0.0, bgBest.width, cellSize - 24.0))
        format = format.copy(align = Html.Alignment.MIDDLE_CENTER)
        alignTopToTopOf(bgBest, 12.0)
        centerXOn(bgBest)
        presenter.bestScore {
            text = it.toString()
        }
    }

    val bgScore = stage.roundRect(cellSize * 1.5, buttonSize, buttonRadius, color = bgColor) {
        alignRightToLeftOf(bgBest, buttonPadding)
        alignTopToTopOf(bgBest)
    }
    stage.text("SCORE", cellSize * 0.25, RGBA(239, 226, 210), font) {
        centerXOn(bgScore)
        alignTopToTopOf(bgScore, buttonRadius)
    }
    stage.text("", cellSize * 0.5, Colors.WHITE, font) {
        setTextBounds(Rectangle(0.0, 0.0, bgScore.width, cellSize - 24.0))
        format = format.copy(align = Html.Alignment.MIDDLE_CENTER)
        centerXOn(bgScore)
        alignTopToTopOf(bgScore, 12.0)
        presenter.score {
            text = it.toString()
        }
    }

    stage.roundRect(boardWidth, boardWidth, buttonRadius, color = bgColor) {
        position(leftIndent, topIndent)
    }
    stage.graphics {
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

private fun setupControls(stage: Stage): Container {
    val boardView = SolidRect(boardWidth, boardWidth, Colors.TRANSPARENT_WHITE)
            .addTo(stage).position(leftIndent, topIndent)

    boardView.onSwipe(20.0) {
        when (it.direction) {
            SwipeDirection.LEFT -> presenter.usersMove(PlayersMoveEnum.LEFT)
            SwipeDirection.RIGHT -> presenter.usersMove(PlayersMoveEnum.RIGHT)
            SwipeDirection.TOP -> presenter.usersMove(PlayersMoveEnum.UP)
            SwipeDirection.BOTTOM -> presenter.usersMove(PlayersMoveEnum.DOWN)
        }
    }

    boardView.onKeyDown {
        when (it.key) {
            Key.LEFT -> presenter.usersMove(PlayersMoveEnum.LEFT)
            Key.RIGHT -> presenter.usersMove(PlayersMoveEnum.RIGHT)
            Key.UP -> presenter.usersMove(PlayersMoveEnum.UP)
            Key.DOWN -> presenter.usersMove(PlayersMoveEnum.DOWN)
            else -> Unit
        }
    }

    return boardView
}

fun restoreControls(stage: Stage) {
    showAppBar(stage)
    // Ensure the view is on top to receive onSwipe events
    boardControls.addTo(stage)
}

fun showGameOver(stage: Stage, onRestart: () -> Unit): Container = stage.container {
    val format = TextFormat(RGBA(0, 0, 0), 40, Html.FontFace.Bitmap(font))
    val skin = TextSkin(
            normal = format,
            over = format.copy(RGBA(90, 90, 90)),
            down = format.copy(RGBA(120, 120, 120))
    )

    fun restart() {
        removeFromParent()
        onRestart()
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
        onClick { restart() }
    }
    onKeyDown {
        when (it.key) {
            Key.ENTER, Key.SPACE -> restart()
            else -> Unit
        }
    }
}
