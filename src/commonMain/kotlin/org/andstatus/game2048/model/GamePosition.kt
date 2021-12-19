package org.andstatus.game2048.model

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.kmem.isOdd
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies
import kotlin.random.Random

private const val keyPlyNumber = "plyNumber"
private const val keyPlyNumberV1 = "moveNumber"
private const val keyPieces = "pieces"
private const val keyScore = "score"
private const val keyDateTime = "time"
private const val keyPlayedSeconds = "playedSeconds"
private const val keyRetries = "retries"

private val SUMMARY_FORMAT = DateFormat("yyyy-MM-dd HH:mm")

/** @author yvolk@yurivolkov.com */
class GamePosition(
    val board: Board,
    var ply: Ply = Ply.emptyPly,
    val pieces: Array<Piece?> = Array(board.size) { null },
    var score: Int = 0,
    val startingDateTime: DateTimeTz = DateTimeTz.nowLocal(),
    val gameClock: GameClock = GameClock(),
    var retries: Int = 0,
    var plyNumber: Int = 0
) {

    val startingDateTimeString get() = startingDateTime.format(SUMMARY_FORMAT)

    val moveNumber: Int
        get() {
            if (plyNumber < 2) return 1
            return (if (plyNumber.isOdd) (plyNumber + 1) else (plyNumber + 2)) / 2
        }

    companion object {

        fun fromJson(board: Board, json: Any): GamePosition? {
            val aMap: Map<String, Any> = json.parseJsonMap()
            val pieces: Array<Piece?>? = aMap[keyPieces]?.parseJsonArray()
                ?.map { Piece.fromId(it as Int) }?.toTypedArray()
            val size: Int = pieces?.size ?: 0
            val score: Int? = aMap[keyScore] as Int?
            val dateTime: DateTimeTz? = aMap[keyDateTime]?.let { DateTime.parse(it as String) }
            val playedSeconds: Int = aMap[keyPlayedSeconds] as Int? ?: 0
            val retries: Int = aMap[keyRetries] as Int? ?: 0
            val plyNumber: Int = aMap[keyPlyNumber] as Int? ?: aMap[keyPlyNumberV1] as Int? ?: 0
            return if (pieces != null && score != null && dateTime != null && size == board.size)
                GamePosition(
                    board,
                    Ply.emptyPly,
                    pieces,
                    score,
                    dateTime,
                    GameClock(playedSeconds),
                    retries,
                    plyNumber
                )
            else null
        }
    }

    fun copy(plyNew: Ply = ply.copy()): GamePosition = GamePosition(
        board, plyNew, pieces.copyOf(), score, startingDateTime, gameClock.copy(), retries, plyNumber
    )

    fun newEmpty() = GamePosition(board)

    private fun forAutoPlaying(seconds: Int, isForward: Boolean) = GamePosition(
        board,
        ply.copy(),
        pieces.copyOf(),
        score,
        startingDateTime,
        if (seconds == 0) gameClock.copy() else GameClock(seconds),
        retries,
        plyNumber + (if (isForward) 1 else -1)
    )

    private fun forNextPly() = GamePosition(
        board, Ply.emptyPly,
        pieces.copyOf(), score, DateTimeTz.nowLocal(), gameClock, retries, plyNumber + 1
    )

    fun composerPly(position: GamePosition, isRedo: Boolean = false): GamePosition {
        val ply = Ply.composerPly(position)
        return play(ply, isRedo)
    }

    fun randomComputerPly(piece: Piece = randomComputerPiece()): GamePosition {
        return getRandomFreeSquare()?.let { square ->
            PlacedPiece(piece, square)
        }?.let { computerPly(it) } ?: board.emptyPosition
    }

    private fun randomComputerPiece() = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4

    fun computerPly(placedPiece: PlacedPiece): GamePosition {
        return placedPiece.let {
            val ply = Ply.computerPly(it, gameClock.playedSeconds)
            play(ply, false)
        }
    }

    fun userPly(plyEnum: PlyEnum, prevAttempt: Ply? = null): GamePosition = if (!UserPlies.contains(plyEnum)) {
        board.emptyPosition
    } else with(forNextPly()) {
        val pieceMoves = mutableListOf<PieceMove>()
        val direction = plyEnum.reverseDirection()
        var square: Square? = board.firstSquareToIterate(direction)
        while (square != null) {
            val found = nextPlacedPieceInThe(square, direction)
            if (found == null) {
                square = square.nextToIterate(direction)
            } else {
                set(found.square, null)
                val next = nextPlacedPieceInThe(found.square, direction)
                if (next != null && found.piece == next.piece) { // merge equal blocks
                    val merged = found.piece.next()
                    set(square, merged)
                    set(next.square, null)
                    pieceMoves += PieceMoveMerge(found, next, PlacedPiece(merged, square)).also {
                        score += it.points()
                    }
                    if (!board.allowResultingTileToMerge) {
                        square = square.nextToIterate(direction)
                    }
                } else {
                    if (found.square != square) {
                        pieceMoves += PieceMoveOne(found, square).also {
                            score += it.points()
                        }
                    }
                    set(square, found.piece)
                    square = square.nextToIterate(direction)
                }
            }
        }
        val plyRetries = if (prevAttempt == null) 0 else prevAttempt.retries + 1
        ply = Ply.userPly(plyEnum, gameClock.playedSeconds, plyRetries, pieceMoves)
        retries += ply.retries
        if (ply.isValid(board)) {
            gameClock.start()
            this
        } else board.emptyPosition
    }

    /** Starting from the square, search for a block in the direction */
    private fun nextPlacedPieceInThe(square: Square, direction: Direction): PlacedPiece? {
        var square1: Square? = square
        while (square1 != null) {
            val square2: Square = square1
            get(square2)?.let {
                return PlacedPiece(it, square2)
            }
            square1 = square2.nextInThe(direction)
        }
        return null
    }

    fun play(ply: Ply, isRedo: Boolean = false): GamePosition =
        (if (isRedo) forAutoPlaying(ply.seconds, true) else forNextPly()).apply {
            ply.pieceMoves.forEach { move ->
                score += move.points()
                when (move) {
                    is PieceMovePlace -> {
                        set(move.first.square, move.first.piece)
                    }
                    is PieceMoveLoad -> {
                        return move.position.copy(ply)
                    }
                    is PieceMoveOne -> {
                        set(move.first.square, null)
                        set(move.destination, move.first.piece)
                    }
                    is PieceMoveMerge -> {
                        set(move.first.square, null)
                        set(move.second.square, null)
                        set(move.merged.square, move.merged.piece)
                    }
                    is PieceMoveDelay -> Unit
                }
            }
            retries += ply.retries
            this.ply = ply
        }

    fun playReversed(ply: Ply): GamePosition {
        var newPosition = forAutoPlaying(ply.seconds, false)

        newPosition.retries -= ply.retries
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
        return with(newPosition) {
            GamePosition(board, ply, pieces, score, startingDateTime, gameClock, retries, plyNumber)
        }
    }

    private fun getRandomFreeSquare(): Square? = freeCount().let {
        if (it == 0) null else findFreeSquare(Random.nextInt(it))
    }

    private fun findFreeSquare(freeIndToFind: Int): Square? {
        var freeInd = -1
        board.array.forEach { square ->
            if (pieces[square.ind] == null) {
                freeInd++
                if (freeInd == freeIndToFind) return square
            }
        }
        return null
    }

    fun placedPieces(): List<PlacedPiece> = board.array.mapNotNull { square ->
        pieces[square.ind]?.let { piece -> PlacedPiece(piece, square) }
    }

    fun freeCount(): Int = pieces.count { it == null }

    fun noMoreMoves(): Boolean {
        board.array.forEach { square ->
            pieces[square.ind]?.let { piece ->
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

    operator fun get(square: Square): Piece? = pieces[square.ind]

    operator fun set(square: Square, value: Piece?) {
        pieces[square.ind] = value
    }

    fun toMap(): Map<String, Any> = mapOf(
        keyPlyNumber to plyNumber,
        keyPieces to pieces.map { it?.id ?: 0 },
        keyScore to score,
        keyDateTime to startingDateTime.format(DateFormat.FORMAT1),
        keyPlayedSeconds to gameClock.playedSeconds
    ).let {
        if (retries == 0) it else it + (keyRetries to retries)
    }

    override fun equals(other: Any?): Boolean {
        return other is GamePosition && ply.plyEnum == other.ply.plyEnum && plyNumber == other.plyNumber
    }

    override fun hashCode(): Int {
        var result = ply.plyEnum.hashCode()
        result = 31 * result + plyNumber
        return result
    }

    override fun toString(): String = "$plyNumber. pieces:" + pieces.mapIndexed { ind, piece ->
        ind.toString() + ":" + (piece ?: "-")
    } + ", score:$score, time:${startingDateTime.format(DateFormat.FORMAT1)}" +
        if (ply.isEmpty()) "" else ", ${ply.plyEnum.id}"
}