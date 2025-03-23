package org.andstatus.game2048.view

import korlibs.io.async.launch
import korlibs.korge.time.delay
import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.korge.view.position
import korlibs.time.milliseconds

fun ViewData.selectBoardSize() = myWindow("select_board_size") {
    var buttons: List<Container?> = emptyList()
    var selected = boardSize
    var xInd = 0
    var yInd = 0

    suspend fun button(buttonEnum: BoardSizeEnum, handler: (BoardSizeEnum) -> Unit): Container? {
        if (isPortrait) {
            yInd++
            if (yInd > 9) return null
        } else {
            yInd++
            if (yInd > 4 && xInd == 0) {
                xInd = 5
                yInd = 1
            }
            if (yInd > 4) return null
        }

        return wideButton(
            if (buttonEnum == selected) "radio_button_checked" else "radio_button_unchecked",
            buttonEnum.labelKey
        ) {
            handler(buttonEnum)
        }.apply {
            position(buttonXs[xInd], buttonYs[yInd])
            addTo(window)
        }
    }

    suspend fun showOptions(handler: (BoardSizeEnum) -> Unit) {
        val oldButtons = buttons
        xInd = 0
        yInd = 0
        buttons = listOf(
            button(BoardSizeEnum.SIZE3, handler),
            button(BoardSizeEnum.SIZE4, handler),
            button(BoardSizeEnum.SIZE5, handler),
            button(BoardSizeEnum.SIZE6, handler),
            button(BoardSizeEnum.SIZE7, handler),
            button(BoardSizeEnum.SIZE8, handler),
        )
        oldButtons.forEach { b -> b?.removeFromParent() }
    }

    fun onSelected(boardSizeEnum: BoardSizeEnum) {
        selected = boardSizeEnum
        korgeCoroutineScope.launch {
            showOptions {}
            delay(100.milliseconds)
            presenter.onSelectBoardSize(selected)
            window.removeFromParent()
        }
    }

    showOptions(::onSelected)
}
