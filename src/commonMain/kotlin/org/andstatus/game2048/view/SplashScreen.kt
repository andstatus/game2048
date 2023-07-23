package org.andstatus.game2048.view

import korlibs.korge.view.Container
import korlibs.korge.view.Stage
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.size
import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs
import kotlin.math.min

suspend fun Stage.splashScreen(quick: ViewDataQuick, colorThemeEnum: ColorThemeEnum): Container = container {
    val suffix = if (colorThemeEnum == ColorThemeEnum.DARK) "dark" else "light"
    image(resourcesVfs["assets/splash_$suffix.png"].readBitmap()) {
        val size1 = min (quick.gameViewWidth, quick.gameViewHeight) * 0.4
        size(size1, size1)
        position((views.virtualWidth - size1) / 2, (views.virtualHeight - size1) / 2)
    }
}

