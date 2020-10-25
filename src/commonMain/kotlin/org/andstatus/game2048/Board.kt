package org.andstatus.game2048

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import kotlin.random.Random

private const val keyPieces = "pieces"
private const val keyScore = "score"
private const val keyTime = "time"

class Board(val width: Int = settings.boardWidth,
            val height: Int = settings.boardHeight,
            val array: Array<Piece?> = Array(width * height) { null },
            var score: Int = 0,
            val time: DateTimeTz = DateTimeTz.nowLocal()) {
    private val size = width * height

    fun firstSquareToIterate(direction: Direction) = when (direction) {
        Direction.LEFT, Direction.UP -> Square(width - 1, height - 1)
        Direction.RIGHT, Direction.DOWN -> Square(0, 0)
    }

    fun getRandomFreeSquare(): Square? =
        (size - count { true }).let {
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

    fun pieces(): List<PlacedPiece> = fold(ArrayList(0)) { list, placedPiece ->
        list.apply { add(placedPiece) }
    }

    private inline fun <R> fold(initial: R, operation: (R, PlacedPiece) -> R): R {
        var acc: R = initial
        array.forEachIndexed { ind, nullableBlock ->
            ind.toSquare()?.let { square ->
                nullableBlock?.let { piece ->
                    acc = operation(acc, PlacedPiece(piece, square))
                }
            }
        }
        return acc
    }

    private inline fun count(predicate: (PlacedPiece) -> Boolean): Int =
        fold(0) { acc, p: PlacedPiece -> if(predicate(p)) acc + 1 else acc }

    private fun Int.toSquare(): Square? {
        if (this < 0 || this >= size) return null
        val x: Int = this % width
        return Square(x, (this - x) / width)
    }

    fun noMoreMoves(): Boolean {
        array.forEachIndexed { ind, nullableBlock ->
            ind.toSquare()?.let { square ->
                nullableBlock?.let { block ->
                    if (PlacedPiece(block, square).hasMove()) return false
                } ?: return false
            }
        }
        return true
    }

    private fun PlacedPiece.hasMove(): Boolean {
        return hasMoveInThe(Direction.LEFT) ||
                hasMoveInThe(Direction.RIGHT) ||
                hasMoveInThe(Direction.UP) ||
                hasMoveInThe(Direction.DOWN)
    }

    private fun PlacedPiece.hasMoveInThe(direction: Direction): Boolean {
        return square.nextInThe(direction, this@Board)
                ?.let { square ->
                    get(square)?.let {it == piece} ?: true
                }
                ?: false
    }

    operator fun get(square: Square): Piece? = square.toInd()?.let { array[it] }

    operator fun set(square: Square, value: Piece?) {
        square.toInd()?.let { array[it] = value }
    }

    private fun Square.toInd(): Int? {
        return if (x < 0 || y < 0 || x >= width || y >= height)
            null
        else x + y * width
    }

    fun save() = IntArray(size) { array[it]?.id ?: 0 }

    fun toJson(): Map<String, Any> = mapOf(
            keyPieces to array.map { it?.id ?: 0 },
            keyScore to score,
            keyTime to time.format(DateFormat.FORMAT1)
    )

    override fun toString(): String = "pieces:" + array.mapIndexed { ind, piece ->
        ind.toString() + ":" + (piece ?: "-")
    } + ", score:$score, time:${time.format(DateFormat.FORMAT1)}"

    fun copy() = Board(width, height, array.copyOf(), score, time)

    fun isEmpty(): Boolean = score == 0 && array.find { it != null } == null

    companion object {

        fun fromJson(json: Any): Board? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val pieces: Array<Piece?>? = aMap[keyPieces]?.asJsonArray()
                    ?.map { Piece.fromId(it as Int) }?.toTypedArray()
            val score: Int? = aMap[keyScore] as Int?
            val time: DateTimeTz? = aMap[keyTime]?.let { DateTime.parse(it as String)}
            return if (pieces != null && score != null && time != null)
                Board(settings.boardWidth, settings.boardHeight, pieces, score, time)
            else null
        }

    }
}