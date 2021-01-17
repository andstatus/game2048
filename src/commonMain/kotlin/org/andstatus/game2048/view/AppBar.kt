package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.position
import com.soywiz.korge.view.positionX
import com.soywiz.korge.view.positionY

private class EButton(val enum: AppBarButtonsEnum, val container: Container)
private infix fun AppBarButtonsEnum.to(container: Container): EButton = EButton(this, container)

class AppBar private constructor(val viewData: ViewData, val logo: RotatingLogo, private val appBarButtons: List<EButton>) {

    fun show(parent: Container, appBarButtonsToShow: List<AppBarButtonsEnum>) {
        logo.addTo(parent)

        appBarButtons.filter { !appBarButtonsToShow.contains(it.enum) }
            .forEach { it.container.removeFromParent() }

        val toShow = appBarButtons.filter { appBarButtonsToShow.contains(it.enum) }.let { list ->
            list.firstOrNull{ eButton ->  eButton.enum == AppBarButtonsEnum.GAME_MENU }?.let{
                it.container.position(viewData.buttonXs[4], viewData.buttonYs[0]).addTo(parent)
                list.filter { it.enum != AppBarButtonsEnum.GAME_MENU }
            } ?: list
        }
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

    companion object {
        suspend fun ViewData.setupAppBar(): AppBar {
            val logo =  RotatingLogo(this, buttonSize).apply { position(buttonXs[0], buttonYs[0]) }

            val appBarTop = buttonYs[1]

            suspend fun button(icon: String, handler: () -> Unit): Container =
                barButton(icon, handler).apply {
                    positionY(appBarTop)
                }

            val appBarButtons: List<EButton> = listOf(
                AppBarButtonsEnum.PLAY to button("play", presenter::onPlayClick),
                AppBarButtonsEnum.TO_START to button("skip_previous", presenter::onToStartClick),
                AppBarButtonsEnum.BACKWARDS to button("backwards", presenter::onBackwardsClick),
                AppBarButtonsEnum.STOP to button("stop", presenter::onStopClick),
                AppBarButtonsEnum.STOP_PLACEHOLDER to Container(),
                AppBarButtonsEnum.FORWARD to button("forward", presenter::onForwardClick),
                AppBarButtonsEnum.FORWARD_PLACEHOLDER to Container(),
                AppBarButtonsEnum.TO_CURRENT to button("skip_next", presenter::onToCurrentClick),

                AppBarButtonsEnum.WATCH to button("watch", presenter::onWatchClick),
                AppBarButtonsEnum.BOOKMARK to button("bookmark_border", presenter::onBookmarkClick),
                AppBarButtonsEnum.BOOKMARKED to button("bookmark", presenter::onBookmarkedClick),
                AppBarButtonsEnum.PAUSE to button("pause", presenter::onPauseClick),
                AppBarButtonsEnum.RESTART to button("restart", presenter::onRestartClick),
                AppBarButtonsEnum.UNDO to button("undo", presenter::onUndoClick),
                AppBarButtonsEnum.REDO to button("redo", presenter::onRedoClick),
                AppBarButtonsEnum.REDO_PLACEHOLDER to Container(),
                AppBarButtonsEnum.GAME_MENU to button("menu", presenter::onGameMenuClick),
            )

            return AppBar(this, logo, appBarButtons)
        }
    }
}