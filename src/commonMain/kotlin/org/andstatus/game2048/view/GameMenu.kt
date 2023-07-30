package org.andstatus.game2048.view

import korlibs.korge.view.addTo
import korlibs.korge.view.position

fun ViewData.showGameMenu(aiEnabled: Boolean, numberOfRecentGames: Int) = myWindow("game_actions") {
    var xInd = 0
    var yInd = 0

    suspend fun button(buttonEnum: GameMenuButtonsEnum, handler: () -> Unit) {
        if (isPortrait) {
            yInd++
            if (yInd > 9) return
        } else {
            yInd++
            if (yInd > 4 && xInd == 0) {
                xInd = 5
                yInd = 1
            }
            if (yInd > 4) return
        }

        wideButton(buttonEnum.icon, buttonEnum.labelKey) {
            handler()
            window.removeFromParent()
        }.apply {
            position(buttonXs[xInd], buttonYs[yInd])
            addTo(window)
        }
    }

    if (aiEnabled) {
        button(GameMenuButtonsEnum.SELECT_AI_ALGORITHM) { selectAiAlgorithm(settings) }
    }
    button(GameMenuButtonsEnum.BOOKMARKS, presenter::onBookmarksClick)
    button(GameMenuButtonsEnum.RECENT, presenter::onRecentClick)
    button(GameMenuButtonsEnum.TRY_AGAIN, presenter::onTryAgainClick)
    button(GameMenuButtonsEnum.SHARE, presenter::onShareClick)
    if (aiEnabled && !isPortrait) {
        button(GameMenuButtonsEnum.LOAD, presenter::onLoadClick)
    }
    button(GameMenuButtonsEnum.SELECT_THEME) { selectTheme(settings) }
    button(GameMenuButtonsEnum.SELECT_BOARD_SIZE) { selectBoardSize(settings) }
    button(GameMenuButtonsEnum.EXIT, presenter::onExitAppClick)
    if (numberOfRecentGames > 5) {
        button(GameMenuButtonsEnum.DELETE, presenter::onDeleteGameClick)
    }
    button(GameMenuButtonsEnum.HELP, presenter::onHelpClick)
}
