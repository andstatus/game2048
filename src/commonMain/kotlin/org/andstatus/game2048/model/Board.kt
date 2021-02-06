package org.andstatus.game2048.model

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.kmem.isOdd
import org.andstatus.game2048.Settings
import kotlin.random.Random

private const val keyMoveNumber = "moveNumber"
private const val keyPieces = "pieces"
private const val keyScore = "score"
private const val keyDateTime = "time"
private const val keyPlayedSeconds = "playedSeconds"

private val SUMMARY_FORMAT = DateFormat("yyyy-MM-dd HH:mm")

class Board(
    val settings: Settings,
    val array: Array<Piece?> = Array(settings.squares.size) { null },
    var score: Int = 0,
    val dateTime: DateTimeTz = DateTimeTz.nowLocal(),
    val gameClock: GameClock = GameClock(),
    var moveNumber: Int = 0
) {
    val timeString get() = dateTime.format(SUMMARY_FORMAT)

    val usersMoveNumber: Int get() {
        if (moveNumber < 2 ) return 1
        return (if (moveNumber.isOdd) (moveNumber + 1) else (moveNumber + 2)) / 2
    }

    fun getRandomFreeSquare(): Square? = freeCount().let {
        if (it == 0) null else findFreeSquare(Random.nextInt(it))
    }

    private fun findFreeSquare(freeIndToFind: Int): Square? {
        var freeInd = -1
        settings.squares.array.forEach { square ->
            if (array[square.ind] == null ) {
                freeInd++
                if (freeInd == freeIndToFind) return square
            }
        }
        return null
    }

    fun pieces(): List<PlacedPiece> = settings.squares.array.mapNotNull { square ->
        array[square.ind]?.let { piece -> PlacedPiece(piece, square) }
    }

    fun freeCount(): Int = array.count { it == null }

    fun noMoreMoves(): Boolean {
        settings.squares.array.forEach { square ->
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
            keyMoveNumber to moveNumber,
            keyPieces to array.map { it?.id ?: 0 },
            keyScore to score,
            keyDateTime to dateTime.format(DateFormat.FORMAT1),
            keyPlayedSeconds to gameClock.playedSeconds
    )

    override fun toString(): String = "$moveNumber. pieces:" + array.mapIndexed { ind, piece ->
        ind.toString() + ":" + (piece ?: "-")
    } + ", score:$score, time:${dateTime.format(DateFormat.FORMAT1)}"

    fun copy() = Board(settings, array.copyOf(), score, dateTime, gameClock.copy(), moveNumber)
    fun forAutoPlaying(seconds: Int, isForward: Boolean) = Board(
        settings, array.copyOf(), score,
        dateTime, if (seconds == 0) gameClock.copy() else GameClock(seconds), moveNumber + (if (isForward) 1 else -1)
    )
    fun forNextMove() = Board(settings, array.copyOf(), score, DateTimeTz.nowLocal(), gameClock,
        moveNumber + 1
    )

    companion object {

        fun fromJson(settings: Settings, json: Any): Board? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val pieces: Array<Piece?>? = aMap[keyPieces]?.asJsonArray()
                    ?.map { Piece.fromId(it as Int) }?.toTypedArray()
            val size: Int = pieces?.size ?: 0
            val score: Int? = aMap[keyScore] as Int?
            val dateTime: DateTimeTz? = aMap[keyDateTime]?.let { DateTime.parse(it as String)}
            val playedSeconds: Int = aMap[keyPlayedSeconds] as Int? ?: 0
            val moveNumber: Int = aMap[keyMoveNumber] as Int? ?: 0
            return if (pieces != null && score != null && dateTime != null && size == settings.squares.size)
                Board(settings, pieces, score, dateTime, GameClock(playedSeconds), moveNumber)
            else null
        }

    }
}