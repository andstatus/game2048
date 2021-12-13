package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.centerOn
import com.soywiz.korge.view.container
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.size
import com.soywiz.korge.view.text
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korio.async.launch
import com.soywiz.korio.file.std.resourcesVfs

suspend fun ViewData.barButton(icon: String, handler: () -> Unit): Container = Container().apply {
    val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
    image(resourcesVfs["assets/$icon.png"].readBitmap()) {
        size(buttonSize * 0.6, buttonSize * 0.6)
        centerOn(background)
    }
    customOnClick { handler() }
}

fun ViewData.myWindow(titleKey: String, action: suspend MyWindow.() -> Unit) =
    MyWindow(this, titleKey).apply {
        gameStage.launch {
            show()
            action()
            addTo(gameStage)
        }
    }

class MyWindow(val viewData: ViewData, val titleKey: String) : Container() {
    val window = this
    val winLeft = viewData.gameViewLeft.toDouble()
    val winTop = viewData.gameViewTop.toDouble()
    val winWidth = viewData.gameViewWidth.toDouble()
    val winHeight = viewData.gameViewHeight.toDouble()

    suspend fun ViewData.wideButton(icon: String, labelKey: String = "", handler: () -> Unit): Container = Container().apply {
        val borderWidth = 2.0
        roundRect(winWidth - 2 * buttonMargin, buttonSize, buttonRadius,
                fill = Colors.TRANSPARENT_BLACK,
                stroke = gameColors.myWindowBorder, strokeThickness = borderWidth)
        roundRect(buttonSize, buttonSize - borderWidth * 2, buttonRadius, fill = gameColors.buttonBackground) {
            position(borderWidth, borderWidth)
        }
        image(resourcesVfs["assets/$icon.png"].readBitmap()) {
            size(buttonSize * 0.6, buttonSize * 0.6)
            position(buttonSize / 5, buttonSize / 5)
        }
        if (labelKey.isNotEmpty()) {
            container {
                text(stringResources.text(labelKey), defaultTextSize, gameColors.labelText,
                    font, TextAlignment.MIDDLE_LEFT
                ) {
                    position(buttonSize + cellMargin, buttonSize / 2)
                }
            }
        }
        customOnClick { handler() }
    }

    suspend fun Container.show() {
        with(viewData) {
            roundRect(
                winWidth, winHeight, buttonRadius, stroke = gameColors.myWindowBorder,
                strokeThickness = 2.0, fill = gameColors.myWindowBackground
            ) {
                position(winLeft, winTop)
            }

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
                    presenter.asyncShowMainView()
                }
            }
        }
    }
}
