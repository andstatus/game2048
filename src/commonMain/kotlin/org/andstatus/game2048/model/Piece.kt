package org.andstatus.game2048.model

enum class Piece(val value: Int) {
    N2(2),
    N4(4),
    N8(8),
    N16(16),
    N32(32),
    N64(64),
    N128(128),
    N256(256),
    N512(512),
    N1024(1024),
    N2048(2048),
    N4096(4096),
    N8192(8192),
    N16384(16384),
    N32768(32768),
    N65536(65536),
    N131972(131072);

    val id = ordinal + 1
    val text = value.toString()

    fun next() = values()[(ordinal + 1) % values().size]

    override fun toString(): String {
        return text
    }

    companion object {
        fun fromId(value: Int): Piece? = values().find { it.id == value}
    }
}

fun Int.toPiece(): Piece? = Piece.values().find { it.id == this}