package org.andstatus.game2048.view

import korlibs.image.color.Colors
import korlibs.image.format.readBitmap
import korlibs.image.text.TextAlignment
import korlibs.io.async.launch
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.korge.view.addUpdater
import korlibs.korge.view.align.centerOn
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.roundRect
import korlibs.korge.view.size
import korlibs.korge.view.text
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Size
import org.andstatus.game2048.myLog

suspend fun ViewData.barButton(icon: String, handler: () -> Unit): Container = Container().apply {
    val background =
        roundRect(Size(buttonSize, buttonSize), RectCorners(buttonRadius), fill = gameColors.buttonBackground)
    image(resourcesVfs["assets/$icon.png"].readBitmap()) {
        size(buttonSize * 0.6, buttonSize * 0.6)
        centerOn(background)
    }
    customOnClick { handler() }
}

fun ViewData.myWindow(titleKey: String, action: suspend MyWindow.() -> Unit): MyWindow =
    MyWindow(this, titleKey).apply {
        korgeCoroutineScope.launch {
            show()
            action()
            addTo(gameStage)
        }
    }

class MyWindow(val viewData: ViewData, val titleKey: String) : Container() {
    init {
        myLog("MyWindow: $titleKey")
    }

    val window = this
    val winLeft = viewData.gameViewLeft.toFloat()
    val winTop = viewData.gameViewTop.toFloat()
    val winWidth = viewData.gameViewWidth.toDouble()
    val winHeight = viewData.gameViewHeight.toDouble()

    suspend fun ViewData.wideButton(icon: String, labelKey: String = "", handler: () -> Unit): Container =
        Container().apply {
            val buttonWidth = if (isPortrait) winWidth - 2 * buttonMargin else winWidth / 2 - 2 * buttonMargin
            val borderWidth = 2.0
            roundRect(
                Size(buttonWidth, buttonSize), RectCorners(buttonRadius),
                fill = Colors.TRANSPARENT,
                stroke = gameColors.myWindowBorder, strokeThickness = borderWidth
            )
            roundRect(
                Size(buttonSize, buttonSize - borderWidth * 2),
                RectCorners(buttonRadius),
                fill = gameColors.buttonBackground
            ) {
                position(borderWidth, borderWidth)
            }
            image(resourcesVfs["assets/$icon.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                position(buttonSize / 5, buttonSize / 5)
            }
            if (labelKey.isNotEmpty()) {
                container {
                    text(
                        stringResources.text(labelKey), defaultTextSize, gameColors.labelText,
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
                Size(winWidth, winHeight), RectCorners(buttonRadius), stroke = gameColors.myWindowBorder,
                strokeThickness = 2.0, fill = gameColors.myWindowBackground
            ) {
                position(winLeft, winTop)
            }

            val xPos = buttonXs[if (isPortrait) 4 else 9]
            val yPos = buttonYs[0]
            barButton("close") {
                window.removeFromParent()
                presenter.onCloseMyWindowClick()
            }.apply {
                position(xPos, yPos)
            }.addTo(window)

            if (titleKey.isNotEmpty()) {
                text(
                    stringResources.text(titleKey), defaultTextSize, gameColors.labelText, font,
                    TextAlignment.MIDDLE_CENTER
                ) {
                    position((winLeft + xPos - cellMargin) / 2f, winTop + cellMargin + buttonSize / 2f)
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
