package org.andstatus.game2048.view

import korlibs.time.milliseconds
import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.korge.view.position
import korlibs.io.async.delay
import korlibs.io.async.launch
import org.andstatus.game2048.Settings

fun ViewData.selectBoardSize(settings: Settings) = myWindow("select_board_size") {
    var selected = settings.boardWidth
    var buttons: List<Container> = emptyList()

    suspend fun button(buttonEnum: BoardSizeEnum, yInd: Int, handler: (BoardSizeEnum) -> Unit): Container =
        wideButton(if (buttonEnum.size == selected) "radio_button_checked" else "radio_button_unchecked", buttonEnum.labelKey) {
            handler(buttonEnum)
        }.apply {
            position(buttonXs[0], buttonYs[yInd])
            addTo(window)
        }


    suspend fun showOptions( handler: (BoardSizeEnum) -> Unit) {
        val oldButtons = buttons
        buttons = listOf(
            button(BoardSizeEnum.SIZE3, 1, handler),
            button(BoardSizeEnum.SIZE4, 2, handler),
            button(BoardSizeEnum.SIZE5, 3, handler),
            button(BoardSizeEnum.SIZE6, 4, handler),
            button(BoardSizeEnum.SIZE7, 5, handler),
            button(BoardSizeEnum.SIZE8, 6, handler),
        )
        oldButtons.forEach{ b -> b.removeFromParent() }
    }

    fun onSelected(boardSizeEnum: BoardSizeEnum) {
        selected = boardSizeEnum.size
        korgeCoroutineScope.launch {
            showOptions {}
            delay(100.milliseconds)
            presenter.onSelectBoardSize(selected)
            window.removeFromParent()
        }
    }

    showOptions(::onSelected)
}
