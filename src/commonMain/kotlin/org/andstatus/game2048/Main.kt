package org.andstatus.game2048

import com.soywiz.korge.Korge
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.SizeInt
import kotlin.coroutines.coroutineContext
import kotlin.properties.Delegates

val defaultPortraitGameWindowSize = SizeInt(720, 1280)
val defaultLandscapeWindowSize = SizeInt(defaultPortraitGameWindowSize.height, defaultPortraitGameWindowSize.width)
val defaultDesktopGameWindowSize get() = SizeInt(defaultPortraitGameWindowSize.width / 2,
    defaultPortraitGameWindowSize.height / 2)
const val defaultPortraitTextSize = 64.0
val defaultLandscapeTextSize = defaultPortraitTextSize  * defaultPortraitGameWindowSize.width /
        defaultPortraitGameWindowSize.height
var defaultTextSize: Double by Delegates.notNull()

suspend fun main() {
    val windowSize: SizeInt = coroutineContext.gameWindowSize
    val virtualWindowSize: SizeInt = if (windowSize.width < windowSize.height) defaultPortraitGameWindowSize
        else defaultLandscapeWindowSize
    defaultTextSize = if (virtualWindowSize.width < virtualWindowSize.height) defaultPortraitTextSize
        else defaultLandscapeTextSize
    val color = if (coroutineContext.isDarkThemeOn) Colors.BLACK else gameDefaultBackgroundColor
    Korge(width = windowSize.width, height = windowSize.height,
        virtualWidth = virtualWindowSize.width, virtualHeight = virtualWindowSize.height,
        bgcolor = color,
            gameId = "org.andstatus.game2048") {
        GameView.initialize(this, true)
    }
}