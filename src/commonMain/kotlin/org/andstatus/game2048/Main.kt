package org.andstatus.game2048

import com.soywiz.korge.Korge
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.SizeInt
import org.andstatus.game2048.view.ColorThemeEnum
import org.andstatus.game2048.view.gameDefaultBackgroundColor
import org.andstatus.game2048.view.viewData
import kotlin.coroutines.coroutineContext

val defaultPortraitGameWindowSize = SizeInt(720, 1280)
val defaultPortraitRatio : Double = defaultPortraitGameWindowSize.width.toDouble() / defaultPortraitGameWindowSize.height
val defaultDesktopGameWindowSize get() = SizeInt(defaultPortraitGameWindowSize.width / 2,
    defaultPortraitGameWindowSize.height / 2)
const val defaultPortraitTextSize = 64.0

suspend fun main() = main(null)

suspend fun main(colorThemeEnum: ColorThemeEnum?) {
    val windowSize: SizeInt = coroutineContext.gameWindowSize
    val windowRatio = windowSize.width.toDouble() /  windowSize.height
    val virtualWidth: Int
    val virtualHeight: Int
    // TODO: We will set the width depending on orientation, when we have landscape layout...
    if (windowRatio >= defaultPortraitRatio) {
        virtualHeight = defaultPortraitGameWindowSize.height
        virtualWidth = (virtualHeight * windowRatio).toInt()
    } else {
        virtualWidth = defaultPortraitGameWindowSize.width
        virtualHeight = (virtualWidth / windowRatio).toInt()
    }
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
        myLog("Stage is ready")
        viewData(this, true)
    }
}
