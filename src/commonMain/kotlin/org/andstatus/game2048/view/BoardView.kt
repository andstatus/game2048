package org.andstatus.game2048.view

import korlibs.event.Key
import korlibs.korge.input.SwipeDirection
import korlibs.korge.input.onSwipe
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.addTo
import korlibs.korge.view.addUpdater
import korlibs.korge.view.graphics
import korlibs.korge.view.position
import korlibs.korge.view.roundRect
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.image.text.TextAlignment
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Size

class BoardView(val viewData: ViewData): Container() {
    private val controlsArea: SolidRect
    private val gameOver: Container

    init {
        with(viewData) {
            position(boardLeft, boardTop)

            roundRect(Size(boardWidth, boardWidth), RectCorners(buttonRadius), fill = gameColors.buttonBackground)
            graphics {
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

            controlsArea = solidRect(boardWidth, boardWidth + buttonSize + buttonMargin,
                gameColors.transparent)

            controlsArea.onSwipe(80.0) {
                    presenter.onSwipe(it.direction)
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
                ifKey(Key.SPACE, presenter::onPauseClick)
                ifKey(Key.M, presenter::onGameMenuClick)
                ifKey(Key.BACKSPACE, presenter::onExitAppClick)
            }

            gameOver = getGameOver()
        }
    }

    /** Ensure the view is on the top to receive onSwipe events */
    fun setOnTop(parent: Container) {
        if (gameOver.parent == null) {
            controlsArea.addTo(this)
        } else {
            gameOver.addTo(this)
        }
        this.addTo(parent)
    }

    fun showGameOver() {
        gameOver.addTo(this)
    }

    fun hideGameOver() {
        gameOver.removeFromParent()
    }

    fun getGameOver(): Container = Container().apply {
        val window = this
        with(viewData) {
            val gameColors = gameColors

            graphics {
                fill(gameColors.gameOverBackground) {
                    roundRect(0.0f, 0.0f, boardWidth, boardWidth, buttonRadius, buttonRadius)
                }
            }
            text(stringResources.text("game_over"),
                defaultTextSize, gameColors.labelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                position(boardWidth / 2, (boardWidth - textSize) / 2)
            }
            text(stringResources.text("try_again"),
                defaultTextSize, gameColors.labelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                position(boardWidth / 2, (boardWidth + textSize) / 2)
                customOnClick {
                    window.removeFromParent()
                    presenter.onRestartClick()
                }
            }
        }
    }

}
