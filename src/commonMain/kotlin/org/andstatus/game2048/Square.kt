package org.andstatus.game2048

data class Square(val x: Int, val y: Int) {

    fun nextToIterate(direction: Direction, board: Board): Square? =
            when (direction) {
                Direction.LEFT -> when {
                    x > 0 -> Square(x - 1, y)
                    y > 0 -> Square(board.width - 1, y - 1)
                    else -> null
                }
                Direction.RIGHT -> when {
                    x < board.width - 1 -> Square(x + 1, y)
                    y < board.height - 1 -> Square(0, y + 1)
                    else -> null
                }
                Direction.UP -> when {
                    y > 0 -> Square(x, y - 1)
                    x > 0 -> Square(x - 1, board.height - 1)
                    else -> null
                }
                Direction.DOWN -> when {
                    y < board.height - 1 -> Square(x, y + 1)
                    x < board.width - 1 -> Square(x + 1, 0)
                    else -> null
                }
            }

    fun nextInThe(direction: Direction, board: Board) : Square? =
            when (direction) {
                Direction.LEFT -> if (x > 0) Square(x - 1, y) else null
                Direction.RIGHT -> if (x < board.width - 1) Square(x + 1, y) else null
                Direction.UP -> if (y > 0) Square(x, y - 1) else null
                Direction.DOWN -> if (y < board.height - 1) Square(x, y + 1) else null
            }

    /** Starting from the square, search for a block in the direction */
    fun nextPlacedBlockInThe(direction: Direction, board: Board): PlacedBlock? {
        when (direction) {
            Direction.LEFT -> for (x1 in x downTo 0) board[Square(x1, y)]?.let { return PlacedBlock(it, Square(x1, y)) }
            Direction.RIGHT -> for (x1 in x until board.width) board[Square(x1, y)]?.let { return PlacedBlock(it, Square(x1, y)) }
            Direction.UP -> for (y1 in y downTo 0) board[Square(x, y1)]?.let { return PlacedBlock(it, Square(x, y1)) }
            Direction.DOWN -> for (y1 in y until board.height) board[Square(x, y1)]?.let { return PlacedBlock(it, Square(x, y1)) }
        }
        return null
    }

}