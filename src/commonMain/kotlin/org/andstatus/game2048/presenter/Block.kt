package org.andstatus.game2048.presenter

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.position
import com.soywiz.korge.view.text
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korma.geom.vector.roundRect
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.view.ViewData

class Block(val piece: Piece, val viewData: ViewData, size: Double = viewData.cellSize) : Container() {

    init {
        graphics {
            fill(viewData.gameColors.pieceBackground(piece)) {
                roundRect(0.0, 0.0, size, size, viewData.buttonRadius)
            }
            text(piece.text, piece.textSize(), viewData.gameColors.pieceText(piece),
                    viewData.font, TextAlignment.MIDDLE_CENTER) {
                position(size / 2, size / 2)
            }
        }
    }

    fun addTo(parent: Container, square: Square) = addTo(parent).apply {
        with(viewData) {
            position(square)
        }
    }

    private fun Piece.textSize(): Double = when (text.length) {
        1, 2, 3 -> width / 2
        else -> width * 1.8 / text.length
    }

}
