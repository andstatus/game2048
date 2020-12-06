package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.position
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.text
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korio.async.launch
import org.andstatus.game2048.defaultTextSize

fun GameView.myWindow(titleKey: String, action: suspend MyWindow.() -> Unit) =
    MyWindow(this, titleKey).apply {
        gameStage.launch {
            show()
            action()
            addTo(gameStage)
        }
    }

class MyWindow(val gameView: GameView, val titleKey: String) : Container() {
    val window = this
    val winLeft = gameView.gameViewLeft.toDouble()
    val winTop = gameView.gameViewTop.toDouble()
    val winWidth = gameView.gameViewWidth.toDouble()
    val winHeight = gameView.gameViewHeight.toDouble()

    suspend fun Container.show() {
        roundRect(winWidth, winHeight, buttonRadius, stroke = gameView.gameColors.myWindowBorder, strokeThickness = 2.0,
                fill = gameView.gameColors.myWindowBackground) {
            position(winLeft, winTop)
        }

        with(gameView) {
            val xPos = buttonXs[4]
            val yPos = buttonYs[0]
            barButton("close") {
                window.removeFromParent()
                presenter.onCloseMyWindowClick()
            }.apply {
                position(xPos, yPos)
            }.addTo(window)

            if (titleKey.isNotEmpty()) {
                text(stringResources.text(titleKey), defaultTextSize, gameColors.labelText, font,
                    TextAlignment.MIDDLE_CENTER
                ) {
                    position((winLeft + xPos - cellMargin) / 2, winTop + cellMargin + buttonSize / 2)
                }
            }

            addUpdater {
                duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                    window.removeFromParent()
                    presenter.showControls()
                }
            }
        }
    }
}