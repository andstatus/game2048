package org.andstatus.game2048.model

import org.andstatus.game2048.Settings

class Board(settings: Settings, boardWidth: Int = settings.boardWidth) {
    val allowUsersMoveWithoutBlockMoves = settings.allowUsersMoveWithoutBlockMoves
    val allowResultingTileToMerge = settings.allowResultingTileToMerge
    val width = boardWidth
    val height = boardWidth
    val size = width * height
    val array: Array<Square> = Array(size) { ind ->
        Square(ind % width, (ind - ind % width) / width)
    }
    val emptyPosition = PlyAndPosition(Ply.emptyPly, GamePosition(this))

    init {
        for (ind in (0..size - 1)) {
            val square = array[ind]
            square.ind = ind

            square.nextToIterate[0] = square._nextToIterate(Direction.LEFT)
            square.nextToIterate[1] = square._nextToIterate(Direction.RIGHT)
            square.nextToIterate[2] = square._nextToIterate(Direction.UP)
            square.nextToIterate[3] = square._nextToIterate(Direction.DOWN)

            square.nextInDirections[0] = square._nextInThe(Direction.LEFT)
            square.nextInDirections[1] = square._nextInThe(Direction.RIGHT)
            square.nextInDirections[2] = square._nextInThe(Direction.UP)
            square.nextInDirections[3] = square._nextInThe(Direction.DOWN)
        }
    }

    fun firstSquareToIterate(direction: Direction) = when (direction) {
        Direction.LEFT, Direction.UP -> toSquare(width - 1, height - 1)
        Direction.RIGHT, Direction.DOWN -> toSquare(0, 0)
    }

    private fun Square._nextToIterate(direction: Direction): Square? =
        when (direction) {
            Direction.LEFT -> when {
                x > 0 -> toSquare(x - 1, y)
                y > 0 -> toSquare(width - 1, y - 1)
                else -> null
            }

            Direction.RIGHT -> when {
                x < width - 1 -> toSquare(x + 1, y)
                y < height - 1 -> toSquare(0, y + 1)
                else -> null
            }

            Direction.UP -> when {
                y > 0 -> toSquare(x, y - 1)
                x > 0 -> toSquare(x - 1, height - 1)
                else -> null
            }

            Direction.DOWN -> when {
                y < height - 1 -> toSquare(x, y + 1)
                x < width - 1 -> toSquare(x + 1, 0)
                else -> null
            }
        }

    private fun Square._nextInThe(direction: Direction): Square? =
        when (direction) {
            Direction.LEFT -> if (x > 0) toSquare(x - 1, y) else null
            Direction.RIGHT -> if (x < width - 1) toSquare(x + 1, y) else null
            Direction.UP -> if (y > 0) toSquare(x, y - 1) else null
            Direction.DOWN -> if (y < height - 1) toSquare(x, y + 1) else null
        }

    fun toSquare(x: Int, y: Int): Square = array[x + y * width]
}