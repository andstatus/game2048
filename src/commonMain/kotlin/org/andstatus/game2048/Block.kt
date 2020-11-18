package org.andstatus.game2048

import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korma.geom.vector.roundRect

class Block(val piece: Piece, font: Font) : Container() {

    init {
        graphics {
            fill(piece.color()) {
                roundRect(0.0, 0.0, cellSize, cellSize, buttonRadius)
            }
            text(piece.text, piece.textSize(), piece.textColor(), font, TextAlignment.MIDDLE_CENTER) {
                position(cellSize / 2, cellSize / 2)
            }
        }
    }

    fun addTo(parent: Container, square: Square) = addTo(parent)
            .position(square.positionX(), square.positionY())

    private fun Piece.color(): RGBA = when (this) {
        Piece.N2 -> RGBA(240, 228, 218)
        Piece.N4 -> RGBA(236, 224, 201)
        Piece.N8 -> RGBA(255, 178, 120)
        Piece.N16 -> RGBA(254, 150, 92)
        Piece.N32 -> RGBA(247, 123, 97)
        Piece.N64 -> RGBA(235, 88, 55)
        Piece.N128 -> RGBA(236, 220, 146)
        Piece.N256 -> RGBA(240, 212, 121)
        Piece.N512 -> RGBA(244, 206, 96)
        Piece.N1024 -> RGBA(248, 200, 71)
        Piece.N2038 -> RGBA(256, 194, 46)
        Piece.N4096 -> RGBA(104, 130, 249)
        Piece.N8192 -> RGBA(51, 85, 247)
        Piece.N16384 -> RGBA(10, 47, 222)
        Piece.N32768 -> RGBA(9, 43, 202)
        Piece.N65536 -> RGBA(181, 37, 188)
        Piece.N131972 -> RGBA(166, 34, 172)
    }

    private fun Piece.textColor() = when (this) {
        Piece.N2, Piece.N4 -> Colors.BLACK
        else -> Colors.WHITE
    }

    private fun Piece.textSize(): Double = when (text.length) {
        1, 2, 3 -> cellSize / 2
        else -> cellSize * 1.8 / text.length
    }

}
