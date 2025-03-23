package org.andstatus.game2048.view

import korlibs.korge.view.addTo
import korlibs.korge.view.position
import org.andstatus.game2048.model.GameMode

fun ViewData.showGameMenu(gameMode: GameMode, numberOfRecentGames: Int) = myWindow("game_actions") {
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

    val aiShown = gameMode.isPlaying && gameMode.aiEnabled
    if (aiShown) {
        button(GameMenuButtonsEnum.SELECT_AI_ALGORITHM) { selectAiAlgorithm(myContext) }
    }
    button(GameMenuButtonsEnum.BOOKMARKS, presenter::onBookmarksClick)
    button(GameMenuButtonsEnum.RECENT, presenter::onRecentClick)
    button(GameMenuButtonsEnum.TRY_AGAIN, presenter::onTryAgainClick)
    button(GameMenuButtonsEnum.SHARE, presenter::onShareClick)
    if (!aiShown || isPortrait) {
        button(GameMenuButtonsEnum.LOAD, presenter::onLoadClick)
    }
    button(GameMenuButtonsEnum.SELECT_THEME) { selectTheme(myContext) }
    button(GameMenuButtonsEnum.SELECT_BOARD_SIZE) { selectBoardSize() }
    button(GameMenuButtonsEnum.EXIT, presenter::onExitAppClick)
    if (numberOfRecentGames > 5) {
        button(GameMenuButtonsEnum.DELETE, presenter::onDeleteGameClick)
    }
    button(GameMenuButtonsEnum.HELP, presenter::onHelpClick)
}
