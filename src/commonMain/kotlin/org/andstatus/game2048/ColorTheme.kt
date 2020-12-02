package org.andstatus.game2048

import com.soywiz.korge.view.Stage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA

val gameDefaultBackgroundColor = Colors["#fdf7f0"]

sealed class ColorTheme {
    open val stageBackground = gameDefaultBackgroundColor
    open val buttonBackground = Colors["#b9aea0"]
    open val buttonLabelText = Colors["#efe2d2"]
    open val buttonText = Colors.WHITE
    open val cellBackground = Colors["#cec0b2"]

    open val labelText = Colors.BLACK
    open val labelTextOver = Colors["#5a5a5a"]
    open val labelTextDown = Colors["#787878"]
    open val gameOverBackground = Colors["#ffffff33"]

    open val myWindowBackground = Colors.WHITE
    open val myWindowBorder = Colors.BLACK

    open val transparent = Colors.TRANSPARENT_WHITE

    open fun pieceBackground(piece: Piece): RGBA = when (piece) {
        Piece.N2 -> Colors["#f0e4da"]
        Piece.N4 -> Colors["#ece0c9"]
        Piece.N8 -> Colors["#ffb278"]
        Piece.N16 -> Colors["#fe965c"]
        Piece.N32 -> Colors["#f77b61"]
        Piece.N64 -> Colors["#eb5837"]
        Piece.N128 -> Colors["#ecdc92"]
        Piece.N256 -> Colors["#f0d479"]
        Piece.N512 -> Colors["#f4ce60"]
        Piece.N1024 -> Colors["#f8c847"]
        Piece.N2048 -> Colors["#ffc22e"]
        Piece.N4096 -> Colors["#6882f9"]
        Piece.N8192 -> Colors["#3355f7"]
        Piece.N16384 -> Colors["#0a2f16"]
        Piece.N32768 -> Colors["#092bca"]
        Piece.N65536 -> Colors["#b525bc"]
        Piece.N131972 -> Colors["#a62248"]
    }

    open fun pieceText(piece: Piece) = when (piece) {
        Piece.N2, Piece.N4 -> Colors.BLACK
        else -> Colors.WHITE
    }

    companion object {
        fun load(stage: Stage): ColorTheme = if (stage.coroutineContext.isDarkThemeOn) DarkTheme() else LightTheme()
    }
}

class LightTheme: ColorTheme()

class DarkTheme: ColorTheme() {
    override val stageBackground = Colors.BLACK
    override val buttonBackground = Colors["#212121"]
    override val buttonLabelText = Colors["#efe2d2"]
    override val buttonText = Colors.WHITE
    override val cellBackground = Colors["#424242"]

    override val labelText = Colors.WHITE
    override val labelTextOver = Colors["#cacaca"]
    override val labelTextDown = Colors["#dedede"]
    override val gameOverBackground = Colors["#00000033"]

    override val myWindowBackground = Colors.BLACK
    override val myWindowBorder = Colors.DARKGRAY

    override val transparent = Colors.TRANSPARENT_WHITE

    override fun pieceBackground(piece: Piece): RGBA = when (piece) {
        Piece.N2 -> Colors["#a3abc2"]
        Piece.N4 -> Colors["#5f70a6"]
        Piece.N8 -> Colors["#395094"]
        Piece.N16 -> Colors["#273d7e"]
        Piece.N32 -> Colors["#a3bac2"]
        Piece.N64 -> Colors["#678a95"]
        Piece.N128 -> Colors["#426772"]
        Piece.N256 -> Colors["#274149"]
        Piece.N512 -> Colors["#98c5bb"]
        Piece.N1024 -> Colors["#679c8f"]
        Piece.N2048 -> Colors["#00604c"]
        Piece.N4096 -> Colors["#003f2c"]
        Piece.N8192 -> Colors["#c2aba3"]
        Piece.N16384 -> Colors["#937063"]
        Piece.N32768 -> Colors["#734e41"]
        Piece.N65536 -> Colors["#54352f"]
        Piece.N131972 -> Colors["#442824"]
    }

    override fun pieceText(piece: Piece) = when (piece) {
        Piece.N2, Piece.N4, Piece.N32, Piece.N64, Piece.N512, Piece.N1024,
        Piece.N8192, Piece.N16384, Piece.N131972 -> Colors.BLACK
        else -> Colors.WHITE
    }
}