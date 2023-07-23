package org.andstatus.game2048.view

import korlibs.time.milliseconds
import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.korge.view.position
import korlibs.io.async.delay
import korlibs.io.async.launch
import org.andstatus.game2048.Settings

fun ViewData.selectTheme(settings: Settings) = myWindow("select_theme") {
    var selected = settings.colorThemeEnum
    var buttons: List<Container> = emptyList()

    suspend fun button(buttonEnum: ColorThemeEnum, yInd: Int, handler: (ColorThemeEnum) -> Unit): Container =
        wideButton(if (buttonEnum == selected) "radio_button_checked" else "radio_button_unchecked", buttonEnum.labelKey) {
            handler(buttonEnum)
        }.apply {
            position(buttonXs[0], buttonYs[yInd])
            addTo(window)
        }


    suspend fun showOptions( handler: (ColorThemeEnum) -> Unit) {
        val oldButtons = buttons
        buttons = listOf(
            button(ColorThemeEnum.DEVICE_DEFAULT, 1, handler),
            button(ColorThemeEnum.DARK, 2, handler),
            button(ColorThemeEnum.LIGHT, 3, handler)
        )
        oldButtons.forEach{ b -> b.removeFromParent() }
    }

    fun onSelected(colorTheme: ColorThemeEnum) {
        selected = colorTheme
        gameStage.launch {
            showOptions {}
            delay(100.milliseconds)
            presenter.onSelectColorTheme(selected)
            window.removeFromParent()
        }
    }

    showOptions(::onSelected)
}
