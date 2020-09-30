package org.andstatus.game2048

import com.soywiz.korge.view.*
import com.soywiz.korma.geom.vector.roundRect

class Block(val piece: Piece) : Container() {

    init {
        graphics {
            fill(piece.color) {
                roundRect(0.0, 0.0, cellSize, cellSize, 5.0)
            }
            text(piece.text, piece.textSize(cellSize), piece.textColor(), font).apply {
                centerBetween(0.0, 0.0, cellSize, cellSize)
            }
        }
    }

    fun addTo(parent: Container, square: Square) = addTo(parent)
            .position(square.positionX(), square.positionY())
}
