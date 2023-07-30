package org.andstatus.game2048.view

import korlibs.math.squared

enum class BoardSizeEnum(val labelKey: String, val width: Int, val size: Int = width.squared()) {
    SIZE3("3x3", 3),
    SIZE4("4x4", 4),
    SIZE5("5x5", 5),
    SIZE6("6x6", 6),
    SIZE7("7x7", 7),
    SIZE8("8x8", 8);

    companion object {
        val BOARD_SIZE_DEFAULT = SIZE4
        private val minWidth = values()[0].width
        private val maxWidth = values()[values().size - 1].width

        fun load(value: Int?): BoardSizeEnum = values().firstOrNull { it.width == value } ?: BOARD_SIZE_DEFAULT

        fun fixBoardWidth(width: Int?): Int {
            return width?.let {
                if (it < minWidth) minWidth
                else {
                    if (it > maxWidth) maxWidth
                    else it
                }
            } ?: BOARD_SIZE_DEFAULT.width
        }


        /** To avoid Square root calculation */
        fun sizeToWidth(sq: Int): Int = values().firstOrNull { it.size == sq }
            ?.width
            ?: 0

        fun isValidBoardWidth(width: Int) = width >= minWidth && width <= maxWidth
    }
}