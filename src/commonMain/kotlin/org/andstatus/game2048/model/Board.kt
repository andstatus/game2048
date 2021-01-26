package org.andstatus.game2048.model

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.kmem.isOdd
import org.andstatus.game2048.*
import kotlin.math.sqrt
import kotlin.random.Random

private const val keyMoveNumber = "moveNumber"
private const val keyPieces = "pieces"
private const val keyScore = "score"
private const val keyDateTime = "time"
private const val keyPlayedSeconds = "playedSeconds"

private val SUMMARY_FORMAT = DateFormat("yyyy-MM-dd HH:mm")

class Board(val width: Int,
            val height: Int,
            val array: Array<Piece?> = Array(width * height) { null },
            var score: Int = 0,
            val dateTime: DateTimeTz = DateTimeTz.nowLocal(),
            val gameClock: GameClock = GameClock(),
            var moveNumber: Int = 0) {
    private val size = width * height
    val timeString get() = dateTime.format(SUMMARY_FORMAT)

    constructor(settings: Settings): this(settings.boardWidth, settings.boardHeight)

    val usersMoveNumber: Int get() {
        if (moveNumber < 2 ) return 1
        return (if (moveNumber.isOdd) (moveNumber + 1) else (moveNumber + 2)) / 2
    }

    fun firstSquareToIterate(direction: Direction) = when (direction) {
        Direction.LEFT, Direction.UP -> Square(width - 1, height - 1)
        Direction.RIGHT, Direction.DOWN -> Square(0, 0)
    }

    fun getRandomFreeSquare(): Square? =
        (size - piecesCount()).let {
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

    fun piecesCount(): Int = count { _ -> true}

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

    fun copy() = Board(width, height, array.copyOf(), score, dateTime, gameClock.copy(), moveNumber)
    fun forAutoPlaying(seconds: Int, isForward: Boolean) = Board(width, height, array.copyOf(), score, dateTime,
            if (seconds == 0) gameClock.copy() else GameClock(seconds), moveNumber + (if (isForward) 1 else -1))
    fun forNextMove() = Board(width, height, array.copyOf(), score, DateTimeTz.nowLocal(), gameClock,
            moveNumber + 1)

    companion object {

        fun fromJson(json: Any): Board? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val pieces: Array<Piece?>? = aMap[keyPieces]?.asJsonArray()
                    ?.map { Piece.fromId(it as Int) }?.toTypedArray()
            val boardWidth: Int = pieces?.let { sqrt(it.size.toDouble()).toInt() } ?: 0
            val score: Int? = aMap[keyScore] as Int?
            val dateTime: DateTimeTz? = aMap[keyDateTime]?.let { DateTime.parse(it as String)}
            val playedSeconds: Int = aMap[keyPlayedSeconds] as Int? ?: 0
            val moveNumber: Int = aMap[keyMoveNumber] as Int? ?: 0
            return if (pieces != null && score != null && dateTime != null)
                Board(boardWidth, boardWidth, pieces, score, dateTime, GameClock(playedSeconds), moveNumber)
            else null
        }

    }
}