package org.andstatus.game2048.model

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.kmem.isOdd
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies
import kotlin.random.Random

private const val keyPlyNumber = "moveNumber"
private const val keyPieces = "pieces"
private const val keyScore = "score"
private const val keyDateTime = "time"
private const val keyPlayedSeconds = "playedSeconds"

private val SUMMARY_FORMAT = DateFormat("yyyy-MM-dd HH:mm")

/** @author yvolk@yurivolkov.com */
class GamePosition(val board: Board, val prevPly: Ply = Ply.emptyPly,
                   val array: Array<Piece?> = Array(board.size) { null },
                   var score: Int = 0,
                   val dateTime: DateTimeTz = DateTimeTz.nowLocal(),
                   val gameClock: GameClock = GameClock(),
                   var plyNumber: Int = 0) {

    companion object {
        fun newEmpty(board: Board) = GamePosition(board, Ply.emptyPly)

        fun Ply.nextPosition(position: GamePosition) = when {
            isNotEmpty() && (pieceMoves.isNotEmpty() || position.board.allowUsersMoveWithoutBlockMoves) -> this
            else -> Ply.emptyPly
        }.let { ply ->
            with(position) {
                GamePosition(board, ply, array, score, dateTime, gameClock, plyNumber)
            }
        }

        fun fromJson(board: Board, json: Any): GamePosition? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val pieces: Array<Piece?>? = aMap[keyPieces]?.asJsonArray()
                ?.map { Piece.fromId(it as Int) }?.toTypedArray()
            val size: Int = pieces?.size ?: 0
            val score: Int? = aMap[keyScore] as Int?
            val dateTime: DateTimeTz? = aMap[keyDateTime]?.let { DateTime.parse(it as String)}
            val playedSeconds: Int = aMap[keyPlayedSeconds] as Int? ?: 0
            val plyNumber: Int = aMap[keyPlyNumber] as Int? ?: 0
            return if (pieces != null && score != null && dateTime != null && size == board.size)
                GamePosition(board, Ply.emptyPly, pieces, score, dateTime, GameClock(playedSeconds), plyNumber)
            else null
        }
    }

    fun copy(): GamePosition = GamePosition(board, prevPly.copy(),
        array.copyOf(), score, dateTime, gameClock.copy(), plyNumber)

    fun forAutoPlaying(seconds: Int, isForward: Boolean) = GamePosition(
        board, prevPly.copy(),
        array.copyOf(), score,
        dateTime, if (seconds == 0) gameClock.copy() else GameClock(seconds), plyNumber + (if (isForward) 1 else -1)
    )

    fun forNextPly() = GamePosition(board, Ply.emptyPly, array.copyOf(), score, DateTimeTz.nowLocal(), gameClock,
        plyNumber + 1)

    fun composerPly(position: GamePosition, isRedo: Boolean = false): GamePosition {
        val ply = Ply.composerPly(position)
        return play(ply, isRedo)
    }

    fun randomComputerPly(): GamePosition {
        return calcPlacedRandomBlock()?.let { computerPly(it) } ?: nextNoMove()
    }

    fun computerPly(placedPiece: PlacedPiece): GamePosition {
        return placedPiece.let {
            val ply = Ply.computerPly(it, gameClock.playedSeconds)
            play(ply, false)
        }
    }

    private fun calcPlacedRandomBlock(): PlacedPiece? =
        getRandomFreeSquare()?.let { square ->
            val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
            PlacedPiece(piece, square)
        }

    fun userPly(plyEnum: PlyEnum): GamePosition {
        return calcUserPly(plyEnum).also {
            if (it.prevPly.isNotEmpty()) gameClock.start()
        }
    }

    fun calcUserPly(plyEnum: PlyEnum): GamePosition {
        if (!UserPlies.contains(plyEnum)) return nextNoMove()

        val newPosition = forNextPly()
        val pieceMoves = mutableListOf<PieceMove>()
        val direction = plyEnum.reverseDirection()
        var square: Square? = board.firstSquareToIterate(direction)
        while (square != null) {
            val found = board.nextPlacedPieceInThe(square, direction, newPosition)
            if (found == null) {
                square = square.nextToIterate(direction)
            } else {
                newPosition[found.square] = null
                val next = board.nextPlacedPieceInThe(found.square, direction, newPosition)
                if (next != null && found.piece == next.piece) {
                    // merge equal blocks
                    val merged = found.piece.next()
                    newPosition[square] = merged
                    newPosition[next.square] = null
                    pieceMoves += PieceMoveMerge(found, next, PlacedPiece(merged, square)).also {
                        newPosition.score += it.points()
                    }
                    if (!board.allowResultingTileToMerge) {
                        square = square.nextToIterate(direction)
                    }
                } else {
                    if (found.square != square) {
                        pieceMoves += PieceMoveOne(found, square).also {
                            newPosition.score += it.points()
                        }
                    }
                    newPosition[square] = found.piece
                    square = square.nextToIterate(direction)
                }
            }
        }
        return Ply.userPly(plyEnum, gameClock.playedSeconds, pieceMoves).nextPosition(newPosition)
    }

    fun nextNoMove() = Ply.emptyPly.nextPosition(this)

    fun newEmpty() = GamePosition(board)

    fun play(ply: Ply, isRedo: Boolean = false): GamePosition {
        var newPosition = if (isRedo) forAutoPlaying(ply.seconds, true) else forNextPly()

        ply.pieceMoves.forEach { move ->
            newPosition.score += move.points()
            when (move) {
                is PieceMovePlace -> {
                    newPosition[move.first.square] = move.first.piece
                }
                is PieceMoveLoad -> {
                    newPosition = move.position.copy()
                }
                is PieceMoveOne -> {
                    newPosition[move.first.square] = null
                    newPosition[move.destination] = move.first.piece
                }
                is PieceMoveMerge -> {
                    newPosition[move.first.square] = null
                    newPosition[move.second.square] = null
                    newPosition[move.merged.square] = move.merged.piece
                }
                is PieceMoveDelay -> Unit
            }
        }
        return ply.nextPosition(newPosition)
    }

    fun playReversed(ply: Ply): GamePosition {
        var newPosition = forAutoPlaying(ply.seconds, false)

        ply.pieceMoves.asReversed().forEach { move ->
            newPosition.score -= move.points()
            when (move) {
                is PieceMovePlace -> {
                    newPosition[move.first.square] = null
                }
                is PieceMoveLoad -> {
                    newPosition = move.position.copy()
                }
                is PieceMoveOne -> {
                    newPosition[move.destination] = null
                    newPosition[move.first.square] = move.first.piece
                }
                is PieceMoveMerge -> {
                    newPosition[move.merged.square] = null
                    newPosition[move.second.square] = move.second.piece
                    newPosition[move.first.square] = move.first.piece
                }
                is PieceMoveDelay -> Unit
            }
        }
        return ply.nextPosition(newPosition)
    }

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
}