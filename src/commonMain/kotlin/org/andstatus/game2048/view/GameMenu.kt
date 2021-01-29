package org.andstatus.game2048.view

import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.position

fun ViewData.showGameMenu(aiEnabled: Boolean) = myWindow("game_actions") {
    var yInd: Int = 0

    suspend fun button(buttonEnum: GameMenuButtonsEnum, yInd: Int, handler: () -> Unit) =
        wideButton(buttonEnum.icon, buttonEnum.labelKey) {
            handler()
            window.removeFromParent()
        }.apply {
            position(buttonXs[0], buttonYs[yInd])
            addTo(window)
        }

    if (aiEnabled) {
        button(GameMenuButtonsEnum.SELECT_AI_ALGORITHM, ++yInd, { selectAiAlgorithm(settings) })
    }
    button(GameMenuButtonsEnum.BOOKMARKS, ++yInd, presenter::onBookmarksClick)
    button(GameMenuButtonsEnum.RESTORE, ++yInd, presenter::onRestoreClick)
    button(GameMenuButtonsEnum.RESTART, ++yInd, presenter::onRestartClick)
    button(GameMenuButtonsEnum.SHARE, ++yInd, presenter::onShareClick)
    button(GameMenuButtonsEnum.LOAD, ++yInd, presenter::onLoadClick)
    button(GameMenuButtonsEnum.HELP, ++yInd, presenter::onHelpClick)
    button(GameMenuButtonsEnum.DELETE, ++yInd, presenter::onDeleteGameClick)
    if (yInd < 8) {
        button(GameMenuButtonsEnum.SELECT_THEME, ++yInd, { selectTheme(settings) })
    }
}
