package org.andstatus.game2048.model

import com.soywiz.klock.DateTimeTz
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies
import kotlin.random.Random

/** @author yvolk@yurivolkov.com */
class GamePosition(val board: Board, val prevPly: Ply = Ply.emptyPly,
                   val data: PositionData = PositionData(board)) {
    val gameClock get() = data.gameClock
    val score get() = data.score

    constructor(board: Board, prevPly: Ply = Ply.emptyPly,
                array: Array<Piece?> = Array(board.size) { null },
                score: Int = 0,
                dateTime: DateTimeTz = DateTimeTz.nowLocal(),
                gameClock: GameClock = GameClock(),
                plyNumber: Int = 0
                ) : this(board, prevPly,
        PositionData(board, array, score, dateTime, gameClock, plyNumber)
    )

    companion object {
        fun newEmpty(board: Board) = GamePosition(board, Ply.emptyPly)

        fun fromJson(board: Board, json: Any): GamePosition? =
            PositionData.fromJson(board, json)?.let {
                GamePosition(board, Ply.emptyPly, it)
            }

    }

    fun copy(): GamePosition = GamePosition(board, prevPly.copy(), data.copy())

    fun forNextPly() = GamePosition(board, Ply.emptyPly, data.forNextPly())

    fun Ply.nextPosition(position: GamePosition) = when {
        this.isNotEmpty() && (pieceMoves.isNotEmpty() || board.allowUsersMoveWithoutBlockMoves) -> this
        else -> Ply.emptyPly
    }.let { GamePosition(board, it, position.data) }

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
        data.getRandomFreeSquare()?.let { square ->
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
        val newData = newPosition.data
        val pieceMoves = mutableListOf<PieceMove>()
        val direction = plyEnum.reverseDirection()
        var square: Square? = board.firstSquareToIterate(direction)
        while (square != null) {
            val found = board.nextPlacedPieceInThe(square, direction, newPosition)
            if (found == null) {
                square = square.nextToIterate(direction)
            } else {
                newData[found.square] = null
                val next = board.nextPlacedPieceInThe(found.square, direction, newPosition)
                if (next != null && found.piece == next.piece) {
                    // merge equal blocks
                    val merged = found.piece.next()
                    newData[square] = merged
                    newData[next.square] = null
                    pieceMoves += PieceMoveMerge(found, next, PlacedPiece(merged, square)).also {
                        newData.score += it.points()
                    }
                    if (!board.allowResultingTileToMerge) {
                        square = square.nextToIterate(direction)
                    }
                } else {
                    if (found.square != square) {
                        pieceMoves += PieceMoveOne(found, square).also {
                            newData.score += it.points()
                        }
                    }
                    newData[square] = found.piece
                    square = square.nextToIterate(direction)
                }
            }
        }
        return Ply.userPly(plyEnum, gameClock.playedSeconds, pieceMoves).nextPosition(newPosition)
    }

    fun nextNoMove() = Ply.emptyPly.nextPosition(this)

    fun newEmpty() = GamePosition(board)

    fun play(ply: Ply, isRedo: Boolean = false): GamePosition {

        var newPosition = GamePosition(board,
            Ply.emptyPly,
            if (isRedo) data.forAutoPlaying(ply.seconds, true) else data.forNextPly()
        )

        ply.pieceMoves.forEach { move ->
            newPosition.data.score += move.points()
            when (move) {
                is PieceMovePlace -> {
                    newPosition.data[move.first.square] = move.first.piece
                }
                is PieceMoveLoad -> {
                    newPosition = move.position.copy()
                }
                is PieceMoveOne -> {
                    newPosition.data[move.first.square] = null
                    newPosition.data[move.destination] = move.first.piece
                }
                is PieceMoveMerge -> {
                    newPosition.data[move.first.square] = null
                    newPosition.data[move.second.square] = null
                    newPosition.data[move.merged.square] = move.merged.piece
                }
                is PieceMoveDelay -> Unit
            }
        }
        return ply.nextPosition(newPosition)
    }

    fun playReversed(ply: Ply): GamePosition {
        var newPosition = GamePosition(board, Ply.emptyPly, data.forAutoPlaying(ply.seconds, false))

        ply.pieceMoves.asReversed().forEach { move ->
            newPosition.data.score -= move.points()
            when (move) {
                is PieceMovePlace -> {
                    newPosition.data[move.first.square] = null
                }
                is PieceMoveLoad -> {
                    newPosition = move.position.copy()
                }
                is PieceMoveOne -> {
                    newPosition.data[move.destination] = null
                    newPosition.data[move.first.square] = move.first.piece
                }
                is PieceMoveMerge -> {
                    newPosition.data[move.merged.square] = null
                    newPosition.data[move.second.square] = move.second.piece
                    newPosition.data[move.first.square] = move.first.piece
                }
                is PieceMoveDelay -> Unit
            }
        }
        return ply.nextPosition(newPosition)
    }

    fun noMoreMoves() = data.noMoreMoves()

}