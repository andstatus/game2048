package org.andstatus.game2048.view

import korlibs.korge.view.position

fun ViewData.showHelp(): MyWindow = myWindow("help_title") {
    wrappableText(
        stringResources.text("help"), winWidth - 2 * cellMargin, defaultTextSize.toDouble(),
        gameColors.labelText, font, Gravity.LEFT
    ) {
        position(winLeft + cellMargin, winTop + buttonSize + buttonMargin + cellMargin)
    }
    customOnClick {
        this.removeFromParent()
        presenter.onCloseHelpClick()
    }
}
