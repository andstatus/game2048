package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.positionX
import com.soywiz.korge.view.positionY
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
import org.andstatus.game2048.view.AppBarButtonsEnum.RESTART
import org.andstatus.game2048.view.AppBarButtonsEnum.STOP
import org.andstatus.game2048.view.AppBarButtonsEnum.STOP_PLACEHOLDER
import org.andstatus.game2048.view.AppBarButtonsEnum.TO_CURRENT
import org.andstatus.game2048.view.AppBarButtonsEnum.TO_START
import org.andstatus.game2048.view.AppBarButtonsEnum.UNDO
import org.andstatus.game2048.view.AppBarButtonsEnum.WATCH

class EButton(val enum: AppBarButtonsEnum, val container: Container = Container())

private suspend fun ViewData.eButton(enum: AppBarButtonsEnum, icon: String, handler: () -> Unit): EButton =
    EButton(enum, this.barButton(icon, handler))

class AppBar private constructor(val viewData: ViewData, private val appBarButtons: List<EButton>) {

    fun show(parent: Container, appBarButtonsToShow: List<AppBarButtonsEnum>) {
        val (toShowAll, toRemove) = appBarButtons.partition { appBarButtonsToShow.contains(it.enum) }
        toRemove.forEach { it.container.removeFromParent() }

        (0 .. 1).forEach { row ->
            val toShow = toShowAll.filter { eButton -> eButton.enum.row == row }
            val remainingPos = viewData.buttonXs.toMutableList()

            // Left aligned buttons
            toShow.filter { it.enum.sortOrder < 0 }
                .sortedBy { it.enum.sortOrder }
                .forEach { eb ->
                    remainingPos.firstOrNull()?.let {
                        eb.container.positionX(it)
                            .addTo(parent)
                        remainingPos.removeFirst()
                    }
                }
            // Others are Right-aligned
            toShow.filter { it.enum.sortOrder >= 0 }
                .sortedByDescending { it.enum.sortOrder }
                .forEach { eb ->
                    remainingPos.lastOrNull()?.let {
                        eb.container.positionX(it)
                            .addTo(parent)
                        remainingPos.removeLast()
                    }
                }
        }
    }

    companion object {
        suspend fun ViewData.setupAppBar(): AppBar {

            suspend fun AppBarButtonsEnum.button(icon: String, handler: () -> Unit): EButton =
                EButton(this, this@setupAppBar.barButton(icon, handler))
            fun AppBarButtonsEnum.button(): EButton = EButton(this)

            val buttons: List<EButton> = listOf(
                rotatingLogo(this, buttonSize),
                GAME_MENU.button("menu", presenter::onGameMenuClick),

                PLAY.button("play", presenter::onPlayClick),
                TO_START.button("skip_previous", presenter::onToStartClick),
                BACKWARDS.button("backwards", presenter::onBackwardsClick),
                STOP.button("stop", presenter::onStopClick),
                STOP_PLACEHOLDER.button(),
                FORWARD.button("forward", presenter::onForwardClick),
                FORWARD_PLACEHOLDER.button(),
                TO_CURRENT.button("skip_next", presenter::onToCurrentClick),

                WATCH.button("watch", presenter::onWatchClick),
                BOOKMARK.button("bookmark_border", presenter::onBookmarkClick),
                BOOKMARKED.button("bookmark", presenter::onBookmarkedClick),
                BOOKMARK_PLACEHOLDER.button(),
                PAUSE.button("pause", presenter::onPauseClick),
                RESTART.button("restart", presenter::onRestartClick),
                UNDO.button("undo", presenter::onUndoClick),
                REDO.button("redo", presenter::onRedoClick),
                REDO_PLACEHOLDER.button(),
            )
            buttons.forEach {
                it.container.positionY(buttonYs[it.enum.row])
            }

            return AppBar(this, buttons)
        }
    }
}