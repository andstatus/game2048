package org.andstatus.game2048.presenter

import korlibs.image.text.TextAlignment
import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.korge.view.graphics
import korlibs.korge.view.position
import korlibs.korge.view.text
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.view.ViewData

class Block(val piece: Piece, val viewData: ViewData, blockSize: Float = viewData.cellSize) : Container() {

    init {
        graphics {
            fill(viewData.gameColors.pieceBackground(piece)) {
                roundRect(0f, 0f, blockSize, blockSize, viewData.buttonRadius, viewData.buttonRadius)
            }
            val textSize: Float = when (piece.text.length) {
                1, 2, 3 -> if (viewData.boardSize.width < 5) blockSize / 2 else blockSize * 0.7f
                else -> blockSize * 1.8f / piece.text.length
            }
            text(
                piece.text, textSize, viewData.gameColors.pieceText(piece),
                viewData.font, TextAlignment.MIDDLE_CENTER
            ) {
                position(blockSize / 2, blockSize / 2)
            }
        }
    }

    fun addTo(parent: Container, square: Square) = addTo(parent).apply {
        with(viewData) {
            position(square)
        }
    }

}
