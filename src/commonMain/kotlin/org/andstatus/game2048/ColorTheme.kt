package org.andstatus.game2048

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA

val gameDefaultBackgroundColor = Colors["#fdf7f0"]

sealed class ColorTheme {
    open val stageBackground = gameDefaultBackgroundColor
    open val buttonBackground = Colors["#b9aea0"]
    open val buttonLabelText = RGBA(239, 226, 210)
    open val buttonText = Colors.WHITE
    open val cellBackground = Colors["#cec0b2"]

    open val labelText = Colors.BLACK
    open val labelTextOver = RGBA(90, 90, 90)
    open val labelTextDown = RGBA(120, 120, 120)
    open val gameOverBackground = RGBA(255, 255, 255, 51)

    open val myWindowBackground = Colors.WHITE
    open val myWindowBorder = Colors.BLACK

    open val transparent = Colors.TRANSPARENT_WHITE

    open fun pieceBackground(piece: Piece): RGBA = when (piece) {
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

    open fun pieceText(piece: Piece) = when (piece) {
        Piece.N2, Piece.N4 -> Colors.BLACK
        else -> Colors.WHITE
    }

    companion object {
        fun load(): ColorTheme = if (isDarkThemeOn) DarkTheme() else LightTheme()
    }
}

class LightTheme: ColorTheme()

class DarkTheme: ColorTheme() {
    override val stageBackground = gameDefaultBackgroundColor
    override val buttonBackground = Colors["#b9aea0"]
    override val buttonLabelText = RGBA(239, 226, 210)
    override val buttonText = Colors.WHITE
    override val cellBackground = Colors["#cec0b2"]

    override val labelText = Colors.BLACK
    override val labelTextOver = RGBA(90, 90, 90)
    override val labelTextDown = RGBA(120, 120, 120)
    override val gameOverBackground = RGBA(255, 255, 255, 51)

    override val myWindowBackground = Colors.WHITE
    override val myWindowBorder = Colors.BLACK

    override val transparent = Colors.TRANSPARENT_WHITE

    override fun pieceBackground(piece: Piece): RGBA = when (piece) {
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

    override fun pieceText(piece: Piece) = when (piece) {
        Piece.N2, Piece.N4 -> Colors.BLACK
        else -> Colors.WHITE
    }
}