package org.andstatus.game2048

import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.math.geom.Size
import korlibs.math.geom.SizeInt
import korlibs.math.geom.toFloat
import org.andstatus.game2048.view.gameDefaultBackgroundColor
import org.andstatus.game2048.view.viewData
import kotlin.coroutines.coroutineContext

val defaultPortraitGameWindowSize = SizeInt(720, 1440)
val defaultPortraitRatio: Double = defaultPortraitGameWindowSize.width.toDouble() / defaultPortraitGameWindowSize.height
val defaultDesktopGameWindowSize
    get() = SizeInt(
        defaultPortraitGameWindowSize.width / 2,
        defaultPortraitGameWindowSize.height / 2
    )
const val defaultPortraitTextSize = 64.0f

suspend fun main() {
    val windowSize: SizeInt = coroutineContext.gameWindowSize
    val virtualWidth: Int
    val virtualHeight: Int
    if (windowSize.width < windowSize.height) {
        val windowRatio = windowSize.width.toDouble() / windowSize.height
        if (windowRatio >= defaultPortraitRatio) {
            virtualHeight = defaultPortraitGameWindowSize.height
            virtualWidth = (virtualHeight * windowRatio).toInt()
        } else {
            virtualWidth = defaultPortraitGameWindowSize.width
            virtualHeight = (virtualWidth / windowRatio).toInt()
        }
    } else {
        val windowRatio = windowSize.height.toDouble() / windowSize.width
        if (windowRatio >= defaultPortraitRatio) {
            virtualHeight = defaultPortraitGameWindowSize.width
            virtualWidth = (virtualHeight / windowRatio).toInt()
        } else {
            virtualWidth = defaultPortraitGameWindowSize.height
            virtualHeight = (virtualWidth * windowRatio).toInt()
        }
    }
    val color = if (coroutineContext.isDarkThemeOn) Colors.BLACK else gameDefaultBackgroundColor
    Korge(
        windowSize = windowSize.toFloat(),
        virtualSize = Size(virtualWidth, virtualHeight),
        bgcolor = color,
        gameId = "org.andstatus.game2048"
    ) {
        myLog("Stage is ready")
        viewData(this, true)
    }
}
