package org.andstatus.game2048

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA

val gameDefaultBackgroundColor = Colors["#fdf7f0"]

val gameColors = ColorTheme()

class ColorTheme {
    val stageBackground = gameDefaultBackgroundColor
    val buttonBackground = Colors["#b9aea0"]
    val buttonLabelText = RGBA(239, 226, 210)
    val buttonText = Colors.WHITE
    val cellBackground = Colors["#cec0b2"]

    val labelText = Colors.BLACK
    val labelTextOver = RGBA(90, 90, 90)
    val labelTextDown = RGBA(120, 120, 120)
    val gameOverBackground = RGBA(255, 255, 255, 51)

    val myWindowBackground = Colors.WHITE
    val myWindowBorder = Colors.BLACK

    val transparent = Colors.TRANSPARENT_WHITE

    fun pieceBackground(piece: Piece): RGBA = when (piece) {
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
        Piece.N2048 -> RGBA(256, 194, 46)
        Piece.N4096 -> RGBA(104, 130, 249)
        Piece.N8192 -> RGBA(51, 85, 247)
        Piece.N16384 -> RGBA(10, 47, 222)
        Piece.N32768 -> RGBA(9, 43, 202)
        Piece.N65536 -> RGBA(181, 37, 188)
        Piece.N131972 -> RGBA(166, 34, 172)
    }

    fun pieceText(piece: Piece) = when (piece) {
        Piece.N2, Piece.N4 -> Colors.BLACK
        else -> Colors.WHITE
    }
}