package org.andstatus.game2048.view

import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.position

fun GameView.showGameMenu() = myWindow("game_actions") {

    suspend fun button(buttonEnum: GameMenuButtonsEnum, yInd: Int, handler: () -> Unit) =
        wideButton(buttonEnum.icon, buttonEnum.labelKey) {
            handler()
            window.removeFromParent()
        }.apply {
            position(buttonXs[0], buttonYs[yInd])
            addTo(window)
        }

    button(GameMenuButtonsEnum.BOOKMARKS, 1, presenter::onBookmarksClick)
    button(GameMenuButtonsEnum.RESTORE, 2, presenter::onRestoreClick)
    button(GameMenuButtonsEnum.RESTART, 3, presenter::onRestartClick)
    button(GameMenuButtonsEnum.SHARE, 4, presenter::onShareClick)
    button(GameMenuButtonsEnum.LOAD, 5, presenter::onLoadClick)
    button(GameMenuButtonsEnum.HELP, 6, presenter::onHelpClick)
    button(GameMenuButtonsEnum.DELETE, 7, presenter::onDeleteGameClick)
    button(GameMenuButtonsEnum.SELECT_THEME, 8, { selectTheme(settings) })
}
