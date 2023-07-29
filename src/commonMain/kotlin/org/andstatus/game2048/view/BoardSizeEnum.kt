package org.andstatus.game2048.view

import korlibs.math.squared

enum class BoardSizeEnum(val labelKey: String, val size: Int, val sizeSquared: Int = size.squared()) {
    SIZE3("3x3", 3),
    SIZE4("4x4", 4),
    SIZE5("5x5", 5),
    SIZE6("6x6", 6),
    SIZE7("7x7", 7),
    SIZE8("8x8", 8);

    companion object {
        val BOARD_SIZE_DEFAULT = SIZE4
        private val minSize = values()[0].size
        private val maxSize = values()[values().size - 1].size

        fun load(value: Int?): BoardSizeEnum = values().firstOrNull { it.size == value } ?: BOARD_SIZE_DEFAULT

        fun fixBoardSize(size: Int?): Int {
            return size?.let {
                if (it < minSize) minSize
                else {
                    if (it > maxSize) maxSize
                    else it
                }
            } ?: BOARD_SIZE_DEFAULT.size
        }


        /** To avoid Square root calculation */
        fun squaredToSize(sq: Int): Int = values().firstOrNull { it.sizeSquared == sq }
            ?.size
            ?: 0

        fun isValidBoardSize(size: Int) = size >= minSize && size <= maxSize
    }
}