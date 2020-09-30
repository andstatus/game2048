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
import com.soywiz.korio.async.ObservableProperty
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.roundRect
import kotlin.properties.Delegates

var board = Board()
val score = ObservableProperty(0)
val bestScore = ObservableProperty(0)

private var history: History by Delegates.notNull()

private const val appBarTopIndent = 30.0
private const val buttonPadding = 18.0
private val bgColor = Colors["#b9aea0"]
const val buttonRadius = 5.0
private var buttonSize : Double = 0.0
private var boardControls: Container by Delegates.notNull()

suspend fun main() = Korge(width = 480, height = 680, title = "2048", bgcolor = Colors["#fdf7f0"]) {
    font = resourcesVfs["clear_sans.fnt"].readBitmapFont()
    cellSize = stage.views.virtualWidth * 1.0 / (board.width + 1)
    buttonSize = cellSize * 0.8
    boardWidth = cellSize * board.width + cellMargin * (board.width + 1)
    leftIndent = (stage.views.virtualWidth - boardWidth) / 2
    topIndent = appBarTopIndent + buttonSize + buttonPadding + buttonSize + buttonPadding

    setupStorage()
    setupAppBar(this)
    setupStaticViews(this)
    boardControls = setupControls(this)

    history.currentElement?.let { restoreState(this, it) } ?: computersMove(this)
}

private fun Stage.setupStorage() {
    val storage = NativeStorage(views)
    val keyOpened = "opened"
    val keyBest = "best"
    val keyHistory = "history"

    Console.log("Storage: $storage" +
            (storage.getOrNull(keyOpened)?.let { "\n last opened: $it" } ?: "\n storage is new") +
            (storage.getOrNull(keyBest)?.let { "\n best score: $it" } ?: "")
    )
    storage[keyOpened] = DateTime.now().toString()

    history = History(storage.getOrNull(keyHistory)) { storage[keyHistory] = it.toString() }
    score.observe {
        if (it > bestScore.value) bestScore.update(it)
    }
    bestScore.observe {
        storage[keyBest] = it.toString()
    }
}

private suspend fun setupAppBar(stage: Stage) {
    val bgLogo = stage.roundRect(cellSize, cellSize, buttonRadius, color = RGBA(237, 196, 3)) {
        position(leftIndent, appBarTopIndent)
    }
    stage.text("2048", cellSize * 0.5, Colors.WHITE, font).centerOn(bgLogo)

    val restartBlock = stage.container {
        val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
        image(resourcesVfs["restart.png"].readBitmap()) {
            size(buttonSize * 0.8, buttonSize * 0.8)
            centerOn(background)
        }
        positionX(leftIndent + boardWidth - buttonSize)
        alignTopToTopOf(bgLogo)
        onClick {
            restart()
            computersMove(stage)
        }
    }

    if (allowUndo) {
        stage.container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, color = bgColor)
            image(resourcesVfs["undo.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            alignTopToTopOf(restartBlock)
            alignRightToLeftOf(restartBlock, buttonPadding)
            onClick {
                restoreState(stage, history.undo())
            }
        }
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
    stage.text(bestScore.value.toString(), cellSize * 0.5, Colors.WHITE, font) {
        setTextBounds(Rectangle(0.0, 0.0, bgBest.width, cellSize - 24.0))
        format = format.copy(align = Html.Alignment.MIDDLE_CENTER)
        alignTopToTopOf(bgBest, 12.0)
        centerXOn(bgBest)
        bestScore {
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
    stage.text(score.value.toString(), cellSize * 0.5, Colors.WHITE, font) {
        setTextBounds(Rectangle(0.0, 0.0, bgScore.width, cellSize - 24.0))
        format = format.copy(align = Html.Alignment.MIDDLE_CENTER)
        centerXOn(bgScore)
        alignTopToTopOf(bgScore, 12.0)
        score {
            text = it.toString()
        }
    }

    stage.roundRect(boardWidth, boardWidth, buttonRadius, color = bgColor) {
        position(leftIndent, topIndent)
    }
    stage.graphics {
        position(leftIndent, topIndent)
        fill(Colors["#cec0b2"]) {
            for (x in 0 until board.width) {
                for (y in 0 until board.height) {
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
            SwipeDirection.LEFT -> moveBlocksTo(stage, Direction.LEFT)
            SwipeDirection.RIGHT -> moveBlocksTo(stage, Direction.RIGHT)
            SwipeDirection.TOP -> moveBlocksTo(stage, Direction.UP)
            SwipeDirection.BOTTOM -> moveBlocksTo(stage, Direction.DOWN)
        }
    }

    boardView.onKeyDown {
        when (it.key) {
            Key.LEFT -> moveBlocksTo(stage, Direction.LEFT)
            Key.RIGHT -> moveBlocksTo(stage, Direction.RIGHT)
            Key.UP -> moveBlocksTo(stage, Direction.UP)
            Key.DOWN -> moveBlocksTo(stage, Direction.DOWN)
            else -> Unit
        }
    }

    return boardView
}

private fun restoreControls(stage: Stage) {
    // Ensure the view is on top to receive onSwipe events
    boardControls.addTo(stage)
}

fun computersMove(stage: Stage) {
    placeRandomBlock(stage)
    history.add(board.save(), score.value)
    restoreControls(stage)
}

fun restoreState(stage: Stage, history: History.Element) {
    board.removeFromParent()
    val newBoard = Board()
    newBoard.load(stage, history.pieceIds)
    score.update(history.score)
    board = newBoard
    restoreControls(stage)
}

fun restart() {
    board.removeFromParent()
    board = Board()
    score.update(0)
    history.clear()
}

fun showGameOver(stage: Stage, onRestart: () -> Unit) {
    val newGameOver = stage.container {
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
    board = board.apply { gameOver?.removeFromParent() }
            .copy().apply { gameOver = newGameOver }
}
