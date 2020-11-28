package org.andstatus.game2048

import com.soywiz.korge.view.*
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korma.geom.vector.roundRect

class Block(val piece: Piece, gameView: GameView, size: Double = cellSize) : Container() {

    init {
        graphics {
            fill(gameView.gameColors.pieceBackground(piece)) {
                roundRect(0.0, 0.0, size, size, buttonRadius)
            }
            text(piece.text, piece.textSize(), gameView.gameColors.pieceText(piece),
                    gameView.font, TextAlignment.MIDDLE_CENTER) {
                position(size / 2, size / 2)
            }
        }
    }

    fun addTo(parent: Container, square: Square) = addTo(parent)
            .position(square.positionX(), square.positionY())

    private fun Piece.textSize(): Double = when (text.length) {
        1, 2, 3 -> width / 2
        else -> width * 1.8 / text.length
    }

}
