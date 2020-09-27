package org.andstatus.game2048

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage
import kotlin.random.Random

class PlacedBlock(val block: Block, val square: Square)

class Board(val width: Int = 4, val height: Int = 4,
            private val array: Array<Block?> = Array(width * height) { null }) {
    val size = width * height
    var gameOver: Container? = null

    fun firstSquareToIterate(direction: Direction) = when (direction) {
        Direction.LEFT, Direction.UP -> Square(width - 1, height - 1)
        Direction.RIGHT, Direction.DOWN -> Square(0, 0)
    }

    fun getRandomFreeSquare(): Square? =
        count { it == null }.let {
            if (it == 0) null else findFreeSquare(Random.nextInt(it))
        }

    private fun findFreeSquare(freeIndToFind: Int): Square? {
        var freeInd = -1
        for (ind in 0..size) {
            if (array[ind] == null) {
                freeInd++
                if (freeInd == freeIndToFind) return ind.toSquare()
            }
        }
        return null
    }

    private inline fun count(predicate: (Block?) -> Boolean): Int =
            fold(0) { acc, b -> if (predicate(b)) acc + 1 else acc }

    private inline fun <R> fold(initial: R, operation: (R, Block?) -> R): R =
            array.fold(initial) { acc1, b -> operation(acc1, b) }

    private fun Int.toSquare(): Square? {
        if (this < 0 || this >= size) return null
        val x: Int = this % width
        return Square(x, (this - x) / width)
    }

    fun noMoreMoves(): Boolean {
        array.forEachIndexed { ind, nullableBlock ->
            ind.toSquare()?.let { square ->
                nullableBlock?.let { block ->
                    if (PlacedBlock(block, square).hasMove()) return false
                }
            }
        }
        return true
    }

    private fun PlacedBlock.hasMove(): Boolean {
        return hasMoveInThe(Direction.LEFT) ||
                hasMoveInThe(Direction.RIGHT) ||
                hasMoveInThe(Direction.UP) ||
                hasMoveInThe(Direction.DOWN)
    }

    private fun PlacedBlock.hasMoveInThe(direction: Direction): Boolean {
        return square.nextInThe(direction, this@Board)
                ?.let { square ->
                    get(square)?.let {it.piece == block.piece} ?: true
                }
                ?: false
    }

    operator fun get(square: Square): Block? = square.toInd()?.let { array[it] }

    operator fun set(square: Square, value: Block?) {
        square.toInd()?.let { array[it] = value }
    }

    private fun Square.toInd(): Int? {
        return if (x < 0 || y < 0 || x >= width || y >= height)
            null
        else x + y * width
    }

    fun save() = IntArray(size) { array[it]?.piece?.id ?: 0 }

    fun load(stage: Stage, ids: IntArray) {
        ids.forEachIndexed { ind, id ->
            ind.toSquare()?.let { square ->
                id.toPiece()?.let { piece ->
                    set(square, Block(piece).addTo(stage, square))
                }
            }
        }
    }

    fun removeFromParent() {
        gameOver?.removeFromParent()
        array.forEach { block -> block?.removeFromParent() }
    }

    fun copy() = Board(width, height, array.copyOf()).apply {
        gameOver = this@Board.gameOver
    }
}
