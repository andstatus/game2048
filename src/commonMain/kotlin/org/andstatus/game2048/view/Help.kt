package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.position
import com.soywiz.korge.view.text
import com.soywiz.korim.text.TextAlignment
import org.andstatus.game2048.defaultTextSize

fun GameView.showHelp(): Container = myWindow("help_title") {
    text(stringResources.text("help"), defaultTextSize, gameColors.labelText, font, TextAlignment.TOP_LEFT) {
        position(winLeft + cellMargin, winTop + buttonSize + buttonPadding + cellMargin)
    }
}