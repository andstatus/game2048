package org.andstatus.game2048.view

import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.position

fun ViewData.showGameMenu(aiEnabled: Boolean) = myWindow("game_actions") {
    var yInd: Int = 0

    suspend fun button(buttonEnum: GameMenuButtonsEnum, handler: () -> Unit) {
        yInd++
        if (yInd > 8) return

        wideButton(buttonEnum.icon, buttonEnum.labelKey) {
            handler()
            window.removeFromParent()
        }.apply {
            position(buttonXs[0], buttonYs[yInd])
            addTo(window)
        }
    }

    if (aiEnabled) {
        button(GameMenuButtonsEnum.SELECT_AI_ALGORITHM) { selectAiAlgorithm(settings) }
    }
    button(GameMenuButtonsEnum.BOOKMARKS, presenter::onBookmarksClick)
    button(GameMenuButtonsEnum.RECENT, presenter::onRecentClick)
    button(GameMenuButtonsEnum.RESTART, presenter::onRestartClick)
    button(GameMenuButtonsEnum.DELETE, presenter::onDeleteGameClick)
    button(GameMenuButtonsEnum.SHARE, presenter::onShareClick)
    button(GameMenuButtonsEnum.LOAD, presenter::onLoadClick)
    button(GameMenuButtonsEnum.SELECT_THEME) { selectTheme(settings) }
    button(GameMenuButtonsEnum.HELP, presenter::onHelpClick)
}
