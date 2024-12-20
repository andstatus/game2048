package org.andstatus.game2048.view

import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.korge.view.positionX
import korlibs.korge.view.positionY
import org.andstatus.game2048.view.AppBarButtonsEnum.AI_FORWARD
import org.andstatus.game2048.view.AppBarButtonsEnum.AI_OFF
import org.andstatus.game2048.view.AppBarButtonsEnum.AI_ON
import org.andstatus.game2048.view.AppBarButtonsEnum.AI_START
import org.andstatus.game2048.view.AppBarButtonsEnum.AI_STOP
import org.andstatus.game2048.view.AppBarButtonsEnum.BACKWARDS
import org.andstatus.game2048.view.AppBarButtonsEnum.BOOKMARK
import org.andstatus.game2048.view.AppBarButtonsEnum.BOOKMARKED
import org.andstatus.game2048.view.AppBarButtonsEnum.BOOKMARK_PLACEHOLDER
import org.andstatus.game2048.view.AppBarButtonsEnum.FORWARD
import org.andstatus.game2048.view.AppBarButtonsEnum.FORWARD_PLACEHOLDER
import org.andstatus.game2048.view.AppBarButtonsEnum.GAME_MENU
import org.andstatus.game2048.view.AppBarButtonsEnum.PAUSE
import org.andstatus.game2048.view.AppBarButtonsEnum.PLAY
import org.andstatus.game2048.view.AppBarButtonsEnum.REDO
import org.andstatus.game2048.view.AppBarButtonsEnum.REDO_PLACEHOLDER
import org.andstatus.game2048.view.AppBarButtonsEnum.STOP
import org.andstatus.game2048.view.AppBarButtonsEnum.STOP_PLACEHOLDER
import org.andstatus.game2048.view.AppBarButtonsEnum.TO_CURRENT
import org.andstatus.game2048.view.AppBarButtonsEnum.TO_START
import org.andstatus.game2048.view.AppBarButtonsEnum.TRY_AGAIN
import org.andstatus.game2048.view.AppBarButtonsEnum.UNDO
import org.andstatus.game2048.view.AppBarButtonsEnum.WATCH

class EButton(val enum: AppBarButtonsEnum, val container: Container = Container())

class AppBar private constructor(val viewData: ViewData, private val appBarButtons: List<EButton>) {

    fun show(parent: Container, appBarButtonsToShow: List<AppBarButtonsEnum>) {
        val (toShowAll, toRemove) = appBarButtons.partition { appBarButtonsToShow.contains(it.enum) }
        toRemove.forEach { it.container.removeFromParent() }

        (0..1).forEach { row ->
            val toShow = toShowAll.filter { eButton -> eButton.enum.row == row }
            val remainingPos = viewData.buttonXs.take(5).toMutableList()

            // Left aligned buttons
            toShow.filter { it.enum.sortOrder < 0 }
                .sortedBy { it.enum.sortOrder }
                .forEach { eb ->
                    remainingPos.firstOrNull()?.let {
                        eb.container.positionX(it)
                            .addTo(parent)
                        remainingPos.removeAt(0)
                    }
                }
            // Others are Right-aligned
            toShow.filter { it.enum.sortOrder >= 0 }
                .sortedByDescending { it.enum.sortOrder }
                .forEach { eb ->
                    remainingPos.lastOrNull()?.let {
                        eb.container.positionX(it)
                            .addTo(parent)
                        remainingPos.removeAt(remainingPos.size - 1)
                    }
                }
        }
    }

    companion object {
        suspend fun ViewData.setupAppBar(): AppBar {

            suspend fun AppBarButtonsEnum.button(handler: () -> Unit): EButton =
                EButton(this, this@setupAppBar.barButton(this.icon, handler))

            fun AppBarButtonsEnum.button(): EButton = EButton(this)

            val buttons: List<EButton> = listOf(
                rotatingLogo(this, buttonSize),
                AI_OFF.button(presenter::onNoMagicClick),
                AI_ON.button(presenter::onMagicClick),

                TRY_AGAIN.button(presenter::onTryAgainClick),
                GAME_MENU.button(presenter::onGameMenuClick),

                PLAY.button(presenter::onPlayClick),
                BOOKMARK.button(presenter::onBookmarkClick),
                BOOKMARKED.button(presenter::onBookmarkedClick),
                BOOKMARK_PLACEHOLDER.button(),
                PAUSE.button(presenter::onPauseClick),
                AI_STOP.button(presenter::onAiStopClick),
                AI_START.button(presenter::onAiStartClick),
                AI_FORWARD.button(presenter::onAiForwardClick),
                UNDO.button(presenter::onUndoClick),
                REDO.button(presenter::onRedoClick),
                REDO_PLACEHOLDER.button(),

                WATCH.button(presenter::onWatchClick),
                TO_START.button(presenter::onToStartClick),
                BACKWARDS.button(presenter::onBackwardsClick),
                STOP.button(presenter::onStopClick),
                STOP_PLACEHOLDER.button(),
                FORWARD.button(presenter::onForwardClick),
                FORWARD_PLACEHOLDER.button(),
                TO_CURRENT.button(presenter::onToCurrentClick),
            )
            buttons.forEach {
                it.container.positionY(buttonYs[it.enum.row])
            }

            return AppBar(this, buttons)
        }
    }
}
