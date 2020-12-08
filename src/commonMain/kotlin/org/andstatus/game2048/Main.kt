package org.andstatus.game2048

import com.soywiz.korge.Korge
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.SizeInt
import org.andstatus.game2048.view.ColorThemeEnum
import org.andstatus.game2048.view.GameView
import org.andstatus.game2048.view.gameDefaultBackgroundColor
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.properties.Delegates

val defaultPortraitGameWindowSize = SizeInt(720, 1280)
val defaultDesktopGameWindowSize get() = SizeInt(defaultPortraitGameWindowSize.width / 2,
    defaultPortraitGameWindowSize.height / 2)
const val defaultPortraitTextSize = 64.0
var defaultTextSize: Double by Delegates.notNull()

suspend fun main() = main(null)

suspend fun main(colorThemeEnum: ColorThemeEnum?) {
    val windowSize: SizeInt = coroutineContext.gameWindowSize
    val virtualHeight: Int = defaultPortraitGameWindowSize.height
    val windowProportionalWidth = virtualHeight * windowSize.width / windowSize.height
    // We will set the width depending on orientation, when we have landscape layout...
    val virtualWidth = max( windowProportionalWidth, defaultPortraitGameWindowSize.width)
    defaultTextSize = if (virtualHeight == defaultPortraitGameWindowSize.height) defaultPortraitTextSize
        else defaultPortraitTextSize * virtualHeight / defaultPortraitGameWindowSize.height
    val color = colorThemeEnum?.let {
        when (it) {
            ColorThemeEnum.DEVICE_DEFAULT -> null
            ColorThemeEnum.DARK -> Colors.BLACK
            ColorThemeEnum.LIGHT -> gameDefaultBackgroundColor
        }
    } ?: if (coroutineContext.isDarkThemeOn) Colors.BLACK else gameDefaultBackgroundColor
    Korge(width = windowSize.width, height = windowSize.height,
            virtualWidth = virtualWidth, virtualHeight = virtualHeight,
            bgcolor = color,
            gameId = "org.andstatus.game2048") {
        GameView.initialize(this, loadSettings(stage), true)
    }
}