package org.andstatus.game2048

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.kmem.isOdd
import kotlin.random.Random

private const val keyMoveNumber = "moveNumber"
private const val keyPieces = "pieces"
private const val keyScore = "score"
private const val keyDateTime = "time"
private const val keyPlayedSeconds = "playedSeconds"

class Board(val width: Int = settings.boardWidth,
            val height: Int = settings.boardHeight,
            val array: Array<Piece?> = Array(width * height) { null },
            var score: Int = 0,
            val dateTime: DateTimeTz = DateTimeTz.nowLocal(),
            val gameClock: GameClock = GameClock(),
            val moveNumber: Int = 0) {
    private val size = width * height

    val usersMoveNumber: Int get() {
        if (moveNumber < 2 ) return 1
        return (if (moveNumber.isOdd) (moveNumber + 1) else (moveNumber + 2)) / 2
    }

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

    fun toMap(): Map<String, Any> = mapOf(
            keyMoveNumber to moveNumber,
            keyPieces to array.map { it?.id ?: 0 },
            keyScore to score,
            keyDateTime to dateTime.format(DateFormat.FORMAT1),
            keyPlayedSeconds to gameClock.playedSeconds
    )

    override fun toString(): String = "$moveNumber. pieces:" + array.mapIndexed { ind, piece ->
        ind.toString() + ":" + (piece ?: "-")
    } + ", score:$score, time:${dateTime.format(DateFormat.FORMAT1)}"

    fun copy() = Board(width, height, array.copyOf(), score, dateTime, gameClock, moveNumber)
    fun forPreviousMove(seconds: Int) = Board(width, height, array.copyOf(), score, dateTime,
            if (seconds == 0) gameClock else GameClock(seconds), moveNumber - 1)
    fun forNextMove() = Board(width, height, array.copyOf(), score, DateTimeTz.nowLocal(), gameClock,
            moveNumber + 1)

    companion object {

        fun fromJson(json: Any): Board? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val pieces: Array<Piece?>? = aMap[keyPieces]?.asJsonArray()
                    ?.map { Piece.fromId(it as Int) }?.toTypedArray()
            val score: Int? = aMap[keyScore] as Int?
            val dateTime: DateTimeTz? = aMap[keyDateTime]?.let { DateTime.parse(it as String)}
            val playedSeconds: Int = aMap[keyPlayedSeconds] as Int? ?: 0
            val moveNumber: Int = aMap[keyMoveNumber] as Int? ?: 0
            return if (pieces != null && score != null && dateTime != null)
                Board(settings.boardWidth, settings.boardHeight, pieces, score, dateTime, GameClock(playedSeconds), moveNumber)
            else null
        }

    }
}