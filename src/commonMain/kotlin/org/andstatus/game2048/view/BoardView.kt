package org.andstatus.game2048.view

import com.soywiz.korev.Key
import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.input.onSwipe
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.vector.roundRect
import org.andstatus.game2048.settings
import kotlin.properties.Delegates

class BoardView(val gameView: GameView): Container() {
    var controlsArea: View by Delegates.notNull()

    fun setup(): BoardView {
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
        return this.addTo(gameView.gameStage)
    }

    /** Ensure the view is on the top to receive onSwipe events */
    fun setOnTop() {
        controlsArea.addTo(this)
        this.addTo(gameView.gameStage)
    }

}