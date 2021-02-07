package org.andstatus.game2048.model

import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies
import kotlin.random.Random

/** @author yvolk@yurivolkov.com */
class GamePosition(val settings: Settings, val prevPly: Ply, val data: PositionData) {
    val gameClock get() = data.gameClock
    val score get() = data.score

    constructor(settings: Settings) : this(settings, Ply.emptyPly, PositionData(settings))

    fun Ply.nextPosition(positionData: PositionData) = when {
        this.isNotEmpty() && (pieceMoves.isNotEmpty() || settings.allowUsersMoveWithoutBlockMoves) -> this
        else -> Ply.emptyPly
    }.let { GamePosition(settings, it, positionData) }

    fun composerPly(positionData: PositionData, isRedo: Boolean = false): GamePosition {
        val ply = Ply.composerPly(positionData)
        return play(ply, isRedo)
    }

    fun randomComputerPly(): GamePosition {
        return calcPlacedRandomBlock()?.let { computerPly(it) } ?: nextEmpty()
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
        if (!UserPlies.contains(plyEnum)) return nextEmpty()

        val newData = this.data.forNextPly()
        val pieceMoves = mutableListOf<PieceMove>()
        val direction = plyEnum.reverseDirection()
        var square: Square? = settings.squares.firstSquareToIterate(direction)
        while (square != null) {
            val found = settings.squares.nextPlacedPieceInThe(square, direction, newData)
            if (found == null) {
                square = square.nextToIterate(direction)
            } else {
                newData[found.square] = null
                val next = settings.squares.nextPlacedPieceInThe(found.square, direction, newData)
                if (next != null && found.piece == next.piece) {
                    // merge equal blocks
                    val merged = found.piece.next()
                    newData[square] = merged
                    newData[next.square] = null
                    pieceMoves += PieceMoveMerge(found, next, PlacedPiece(merged, square)).also {
                        newData.score += it.points()
                    }
                    if (!settings.allowResultingTileToMerge) {
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
        return Ply.userPly(plyEnum, gameClock.playedSeconds, pieceMoves).nextPosition(newData)
    }

    fun nextEmpty() = Ply.emptyPly.nextPosition(data)

    fun play(ply: Ply, isRedo: Boolean = false): GamePosition {
        var newData = if (isRedo) data.forAutoPlaying(ply.seconds, true) else data.forNextPly()
        ply.pieceMoves.forEach { move ->
            newData.score += move.points()
            when (move) {
                is PieceMovePlace -> {
                    newData[move.first.square] = move.first.piece
                }
                is PieceMoveLoad -> {
                    newData = move.positionData.copy()
                }
                is PieceMoveOne -> {
                    newData[move.first.square] = null
                    newData[move.destination] = move.first.piece
                }
                is PieceMoveMerge -> {
                    newData[move.first.square] = null
                    newData[move.second.square] = null
                    newData[move.merged.square] = move.merged.piece
                }
                is PieceMoveDelay -> Unit
            }
        }
        return ply.nextPosition(newData)
    }

    fun playReversed(ply: Ply): GamePosition {
        var newData = data.forAutoPlaying(ply.seconds, false)
        ply.pieceMoves.asReversed().forEach { move ->
            newData.score -= move.points()
            when (move) {
                is PieceMovePlace -> {
                    newData[move.first.square] = null
                }
                is PieceMoveLoad -> {
                    newData = move.positionData
                }
                is PieceMoveOne -> {
                    newData[move.destination] = null
                    newData[move.first.square] = move.first.piece
                }
                is PieceMoveMerge -> {
                    newData[move.merged.square] = null
                    newData[move.second.square] = move.second.piece
                    newData[move.first.square] = move.first.piece
                }
                is PieceMoveDelay -> Unit
            }
        }
        return ply.nextPosition(newData)
    }

    fun noMoreMoves() = data.noMoreMoves()

}