package org.andstatus.game2048

import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA

enum class Piece(val value: Int, val color: RGBA) {
    N2(2, RGBA(240, 228, 218)),
    N4(4, RGBA(236, 224, 201)),
    N8(8, RGBA(255, 178, 120)),
    N16(16, RGBA(254, 150, 92)),
    N32(32, RGBA(247, 123, 97)),
    N64(64, RGBA(235, 88, 55)),
    N128(128, RGBA(236, 220, 146)),
    N256(256, RGBA(240, 212, 121)),
    N512(512, RGBA(244, 206, 96)),
    N1024(1024, RGBA(248, 200, 71)),
    N2038(2048, RGBA(256, 194, 46)),
    N4096(4096, RGBA(104, 130, 249)),
    N8192(8192, RGBA(51, 85, 247)),
    N16384(16384, RGBA(10, 47, 222)),
    N32768(32768, RGBA(9, 43, 202)),
    N65536(65536, RGBA(181, 37, 188)),
    N131972(131072, RGBA(166, 34, 172));

    val id = ordinal + 1
    val text = value.toString()

    fun textSize(cellSize: Double): Double = when (text.length) {
        1, 2, 3 -> cellSize / 2
        else -> cellSize * 1.8 / text.length
    }

    fun textColor() = when (this) {
        N2, N4 -> Colors.BLACK
        else -> Colors.WHITE
    }

    fun next() = values()[(ordinal + 1) % values().size]

}

fun Int.toPiece(): Piece? = Piece.values().find { it.id == this}