package org.andstatus.game2048.view

import com.soywiz.korev.Key
import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.input.onSwipe
import com.soywiz.korge.ui.TextFormat
import com.soywiz.korge.ui.TextSkin
import com.soywiz.korge.ui.uiText
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.centerXBetween
import com.soywiz.korge.view.container
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.position
import com.soywiz.korge.view.positionY
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korma.geom.vector.roundRect
import org.andstatus.game2048.defaultTextSize
import org.andstatus.game2048.settings

class BoardView(val gameView: GameView): Container() {
    private val controlsArea: SolidRect

    init {
        position(boardLeft, boardTop)

        roundRect(boardWidth, boardWidth, buttonRadius, fill = gameView.gameColors.buttonBackground)
        graphics {
            fill(gameView.gameColors.cellBackground) {
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

        controlsArea = solidRect(boardWidth, boardWidth + gameView.buttonSize + gameView.buttonPadding,
            gameView.gameColors.transparent)

        controlsArea.onSwipe(20.0) {
            gameView.duplicateKeyPressFilter.onSwipeOrOver {
                gameView.presenter.onSwipe(it.direction)
            }
        }

        controlsArea.addUpdater {
            val ifKey = { key: Key, action: () -> Unit ->
                if (gameView.gameStage.views.input.keys[key]) {
                    gameView.duplicateKeyPressFilter.onPress(key, action)
                }
            }
            ifKey(Key.LEFT) { gameView.presenter.onSwipe(SwipeDirection.LEFT) }
            ifKey(Key.RIGHT) { gameView.presenter.onSwipe(SwipeDirection.RIGHT) }
            ifKey(Key.UP) { gameView.presenter.onSwipe(SwipeDirection.TOP) }
            ifKey(Key.DOWN) { gameView.presenter.onSwipe(SwipeDirection.BOTTOM) }
            ifKey(Key.SPACE) { gameView.presenter.onPauseClick() }
            ifKey(Key.M) { gameView.presenter.onGameMenuClick() }
            ifKey(Key.BACKSPACE) {
                gameView.presenter.onCloseGameWindowClick()
            }
        }
        this.addTo(gameView.gameStage)
    }

    /** Ensure the view is on the top to receive onSwipe events */
    fun setOnTop() {
        controlsArea.addTo(this)
        this.addTo(gameView.gameStage)
    }

    fun showGameOver(): Container = container {
        val window = this
        val gameColors = gameView.gameColors
        val format = TextFormat(gameColors.labelText, defaultTextSize.toInt(), gameView.font)
        val skin = TextSkin(
            normal = format,
            over = format.copy(gameColors.labelTextOver),
            down = format.copy(gameColors.labelTextDown)
        )

        graphics {
            fill(gameColors.gameOverBackground) {
                roundRect(0.0, 0.0, boardWidth, boardWidth, buttonRadius)
            }
        }
        text(gameView.stringResources.text("game_over"),
            defaultTextSize, gameColors.labelText, gameView.font,
            TextAlignment.MIDDLE_CENTER
        ) {
            position(boardWidth / 2, (boardWidth - textSize) / 2)
        }
        uiText(gameView.stringResources.text("try_again"), 120.0, 35.0, skin) {
            centerXBetween(0.0, boardWidth)
            positionY((boardWidth + textSize) / 2)
            with(gameView) {
                window.customOnClick {
                    window.removeFromParent()
                    presenter.restart()
                }
            }
        }

        addUpdater {
            gameView.duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameView.gameStage.views.input) {
                window.removeFromParent()
                gameView.presenter.restart()
            }
        }
    }

}