package org.andstatus.game2048.view

enum class BoardSizeEnum(val labelKey: String, val width: Int) {
    SIZE3("3x3", 3),
    SIZE4("4x4", 4),
    SIZE5("5x5", 5),
    SIZE6("6x6", 6),
    SIZE7("7x7", 7),
    SIZE8("8x8", 8);

    val height = width
    val size: Int = width * height
    val keyBest: String = when (width) {
        4 -> "best"
        else -> "best${this.width}"
    }

    companion object {
        val BOARD_SIZE_DEFAULT = SIZE4

        fun fromIntWidth(value: Int?): BoardSizeEnum = entries.firstOrNull { it.width == value } ?: BOARD_SIZE_DEFAULT

        /** To avoid Square root calculation */
        fun fromIntSizeOrNull(sq: Int): BoardSizeEnum? = entries.firstOrNull { it.size == sq }
    }

    override fun toString(): String = labelKey
}