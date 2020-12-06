package org.andstatus.game2048.view

import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.container
import com.soywiz.korge.view.position
import com.soywiz.korge.view.text
import com.soywiz.korim.text.TextAlignment
import org.andstatus.game2048.defaultTextSize

fun GameView.showGameMenu() = myWindow("game_actions") {

    suspend fun button(buttonEnum: GameMenuButtonsEnum, yInd: Int, handler: () -> Unit) =
        barButton(buttonEnum.icon) {
            handler()
            window.removeFromParent()
        }.apply {
            position(buttonXs[0], buttonYs[yInd])
            addTo(window)
            window.container {
                text(stringResources.text(buttonEnum.labelKey), defaultTextSize, gameColors.labelText,
                    font, TextAlignment.MIDDLE_LEFT
                ) {
                    position(buttonXs[1], buttonYs[yInd] + buttonSize / 2)
                    customOnClick {
                        handler()
                        window.removeFromParent()
                    }
                }
            }
        }

    button(GameMenuButtonsEnum.BOOKMARKS, 1, presenter::onBookmarksClick)
    button(GameMenuButtonsEnum.RESTORE, 2, presenter::onRestoreClick)
    button(GameMenuButtonsEnum.RESTART, 3, presenter::onRestartClick)
    button(GameMenuButtonsEnum.SHARE, 4, presenter::onShareClick)
    button(GameMenuButtonsEnum.LOAD, 5, presenter::onLoadClick)
    button(GameMenuButtonsEnum.HELP, 6, presenter::onHelpClick)
    button(GameMenuButtonsEnum.DELETE, 7, presenter::onDeleteGameClick)
}
