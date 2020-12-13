package org.andstatus.game2048.view

import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.position
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launch
import org.andstatus.game2048.Settings

fun GameView.selectTheme(settings: Settings) = myWindow("select_theme") {
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
