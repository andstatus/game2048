package org.andstatus.game2048

import com.soywiz.korge.Korge
import com.soywiz.korma.geom.SizeInt

val defaultGameWindowSize get() = SizeInt(720, 1000)
const val defaultTextSize = 64.0

suspend fun main() {
    val windowSize: SizeInt = defaultGameWindowSize
    val color = if (isDarkThemeOn) midnightColor else gameDefaultBackgroundColor
    Korge(width = windowSize.width, height = windowSize.height, bgcolor = color,
            gameId = "org.andstatus.game2048") {
        GameView.appEntry(this, true)
    }
}