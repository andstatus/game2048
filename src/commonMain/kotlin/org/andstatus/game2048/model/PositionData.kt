package org.andstatus.game2048.model

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.kmem.isOdd
import kotlin.random.Random

private const val keyPlyNumber = "moveNumber"
private const val keyPieces = "pieces"
private const val keyScore = "score"
private const val keyDateTime = "time"
private const val keyPlayedSeconds = "playedSeconds"

private val SUMMARY_FORMAT = DateFormat("yyyy-MM-dd HH:mm")

class PositionData(
    val board: Board,
    val array: Array<Piece?> = Array(board.size) { null },
    var score: Int = 0,
    val dateTime: DateTimeTz = DateTimeTz.nowLocal(),
    val gameClock: GameClock = GameClock(),
    var plyNumber: Int = 0
) {
    val timeString get() = dateTime.format(SUMMARY_FORMAT)

    val moveNumber: Int get() {
        if (plyNumber < 2 ) return 1
        return (if (plyNumber.isOdd) (plyNumber + 1) else (plyNumber + 2)) / 2
    }

    fun getRandomFreeSquare(): Square? = freeCount().let {
        if (it == 0) null else findFreeSquare(Random.nextInt(it))
    }

    private fun findFreeSquare(freeIndToFind: Int): Square? {
        var freeInd = -1
        board.array.forEach { square ->
            if (array[square.ind] == null ) {
                freeInd++
                if (freeInd == freeIndToFind) return square
            }
        }
        return null
    }

    fun pieces(): List<PlacedPiece> = board.array.mapNotNull { square ->
        array[square.ind]?.let { piece -> PlacedPiece(piece, square) }
    }

    fun freeCount(): Int = array.count { it == null }

    fun noMoreMoves(): Boolean {
        board.array.forEach { square ->
            array[square.ind]?.let { piece ->
                if (square.hasMove(piece)) return false
            } ?: return false
        }
        return true
    }

    private fun Square.hasMove(piece: Piece): Boolean {
        return hasMoveInThe(piece, Direction.LEFT) ||
                hasMoveInThe(piece, Direction.RIGHT) ||
                hasMoveInThe(piece, Direction.UP) ||
                hasMoveInThe(piece, Direction.DOWN)
    }

    private fun Square.hasMoveInThe(piece: Piece, direction: Direction): Boolean = nextInThe(direction)
        ?.let { square -> get(square)?.let { it == piece } ?: true }
        ?: false

    operator fun get(square: Square): Piece? = array[square.ind]

    operator fun set(square: Square, value: Piece?) {
        array[square.ind] = value
    }

    fun toMap(): Map<String, Any> = mapOf(
            keyPlyNumber to plyNumber,
            keyPieces to array.map { it?.id ?: 0 },
            keyScore to score,
            keyDateTime to dateTime.format(DateFormat.FORMAT1),
            keyPlayedSeconds to gameClock.playedSeconds
    )

    override fun toString(): String = "$plyNumber. pieces:" + array.mapIndexed { ind, piece ->
        ind.toString() + ":" + (piece ?: "-")
    } + ", score:$score, time:${dateTime.format(DateFormat.FORMAT1)}"

    fun copy() = PositionData(board, array.copyOf(), score, dateTime, gameClock.copy(), plyNumber)
    fun forAutoPlaying(seconds: Int, isForward: Boolean) = PositionData(
        board, array.copyOf(), score,
        dateTime, if (seconds == 0) gameClock.copy() else GameClock(seconds), plyNumber + (if (isForward) 1 else -1)
    )
    fun forNextPly() = PositionData(board, array.copyOf(), score, DateTimeTz.nowLocal(), gameClock,
        plyNumber + 1
    )

    companion object {

        fun fromJson(board: Board, json: Any): PositionData? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val pieces: Array<Piece?>? = aMap[keyPieces]?.asJsonArray()
                    ?.map { Piece.fromId(it as Int) }?.toTypedArray()
            val size: Int = pieces?.size ?: 0
            val score: Int? = aMap[keyScore] as Int?
            val dateTime: DateTimeTz? = aMap[keyDateTime]?.let { DateTime.parse(it as String)}
            val playedSeconds: Int = aMap[keyPlayedSeconds] as Int? ?: 0
            val plyNumber: Int = aMap[keyPlyNumber] as Int? ?: 0
            return if (pieces != null && score != null && dateTime != null && size == board.size)
                PositionData(board, pieces, score, dateTime, GameClock(playedSeconds), plyNumber)
            else null
        }

    }
}