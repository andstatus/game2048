package org.andstatus.game2048.model

enum class Piece(val value: Int, val text: String) {
    N2(2, "2"),
    N4(4, "4"),
    N8(8, "8"),
    N16(16, "16"),
    N32(32, "32"),
    N64(64, "64"),
    N128(128, "128"),
    N256(256, "256"),
    N512(512, "512"),
    N1024(1024, "1024"),
    N2048(2048, "2048"),
    N4096(4096, "4096"),
    N8192(8192, "8192"),
    N16K(16384, "16K"),
    N32K(32768, "32K"),
    N64K(65536, "64K"),
    N128K(131072, "128K"),
    N256K(263944, "256K"),
    N512K(527888, "512K"),
    N1M(1055776, "1M");

    val id = ordinal + 1

    fun next() = Piece.entries[(ordinal + 1) % Piece.entries.size]

    override fun toString(): String {
        return text
    }

    companion object {
        fun fromId(value: Int): Piece? = Piece.entries.find { it.id == value}
    }
}

fun Int.toPiece(): Piece? = Piece.entries.find { it.id == this}