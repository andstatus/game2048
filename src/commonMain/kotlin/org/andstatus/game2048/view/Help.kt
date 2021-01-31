package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.position

fun ViewData.showHelp(): Container = myWindow("help_title") {
    wrappableText(stringResources.text("help"), winWidth - 2 * cellMargin , defaultTextSize,
        gameColors.labelText, font, Gravity.LEFT) {
        position(winLeft + cellMargin, winTop + buttonSize + buttonPadding + cellMargin)
    }
    customOnClick {
        this.removeFromParent()
        presenter.onCloseMyWindowClick()
    }
}