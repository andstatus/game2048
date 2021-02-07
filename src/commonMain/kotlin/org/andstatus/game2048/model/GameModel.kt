package org.andstatus.game2048.model

import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies
import kotlin.random.Random

/** @author yvolk@yurivolkov.com */
class GameModel(val settings: Settings, val prevMove: Ply, val board: Board) {
    val gameClock get() = board.gameClock
    val score get() = board.score

    constructor(settings: Settings) : this(settings, Ply.emptyPly, Board(settings))

    fun Ply.nextModel(board: Board) = when {
        this.isNotEmpty() && (pieceMoves.isNotEmpty() || settings.allowUsersMoveWithoutBlockMoves) -> this
        else -> Ply.emptyPly
    }.let { GameModel(settings, it, board) }

    fun composerMove(board: Board, isRedo: Boolean = false): GameModel {
        val move = Ply.composerMove(board)
        return play(move, isRedo)
    }

    fun randomComputerMove(): GameModel {
        return calcPlacedRandomBlock()?.let { computerMove(it) } ?: nextEmpty()
    }

    fun computerMove(placedPiece: PlacedPiece): GameModel {
        return placedPiece.let {
            val move = Ply.computerMove(it, gameClock.playedSeconds)
            play(move, false)
        }
    }

    private fun calcPlacedRandomBlock(): PlacedPiece? =
        board.getRandomFreeSquare()?.let { square ->
            val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
            PlacedPiece(piece, square)
        }

    fun userMove(plyEnum: PlyEnum): GameModel {
        return calcUserMove(plyEnum).also {
            if (it.prevMove.isNotEmpty()) gameClock.start()
        }
    }

    fun calcUserMove(plyEnum: PlyEnum): GameModel {
        if (!UserPlies.contains(plyEnum)) return nextEmpty()

        val newBoard = this.board.forNextMove()
        val moves = mutableListOf<PieceMove>()
        val direction = plyEnum.reverseDirection()
        var square: Square? = settings.squares.firstSquareToIterate(direction)
        while (square != null) {
            val found = settings.squares.nextPlacedPieceInThe(square, direction, newBoard)
            if (found == null) {
                square = square.nextToIterate(direction)
            } else {
                newBoard[found.square] = null
                val next = settings.squares.nextPlacedPieceInThe(found.square, direction, newBoard)
                if (next != null && found.piece == next.piece) {
                    // merge equal blocks
                    val merged = found.piece.next()
                    newBoard[square] = merged
                    newBoard[next.square] = null
                    moves += PieceMoveMerge(found, next, PlacedPiece(merged, square)).also {
                        newBoard.score += it.points()
                    }
                    if (!settings.allowResultingTileToMerge) {
                        square = square.nextToIterate(direction)
                    }
                } else {
                    if (found.square != square) {
                        moves += PieceMoveOne(found, square).also {
                            newBoard.score += it.points()
                        }
                    }
                    newBoard[square] = found.piece
                    square = square.nextToIterate(direction)
                }
            }
        }
        return Ply.userMove(plyEnum, gameClock.playedSeconds, moves).nextModel(newBoard)
    }

    fun nextEmpty() = Ply.emptyPly.nextModel(board)

    fun play(ply: Ply, isRedo: Boolean = false): GameModel {
        var newBoard = if (isRedo) board.forAutoPlaying(ply.seconds, true) else board.forNextMove()
        ply.pieceMoves.forEach { move ->
            newBoard.score += move.points()
            when (move) {
                is PieceMovePlace -> {
                    newBoard[move.first.square] = move.first.piece
                }
                is PieceMoveLoad -> {
                    newBoard = move.board.copy()
                }
                is PieceMoveOne -> {
                    newBoard[move.first.square] = null
                    newBoard[move.destination] = move.first.piece
                }
                is PieceMoveMerge -> {
                    newBoard[move.first.square] = null
                    newBoard[move.second.square] = null
                    newBoard[move.merged.square] = move.merged.piece
                }
                is PieceMoveDelay -> Unit
            }
        }
        return ply.nextModel(newBoard)
    }

    fun playReversed(ply: Ply): GameModel {
        var newBoard = board.forAutoPlaying(ply.seconds, false)
        ply.pieceMoves.asReversed().forEach { move ->
            newBoard.score -= move.points()
            when (move) {
                is PieceMovePlace -> {
                    newBoard[move.first.square] = null
                }
                is PieceMoveLoad -> {
                    newBoard = move.board
                }
                is PieceMoveOne -> {
                    newBoard[move.destination] = null
                    newBoard[move.first.square] = move.first.piece
                }
                is PieceMoveMerge -> {
                    newBoard[move.merged.square] = null
                    newBoard[move.second.square] = move.second.piece
                    newBoard[move.first.square] = move.first.piece
                }
                is PieceMoveDelay -> Unit
            }
        }
        return ply.nextModel(newBoard)
    }

    fun noMoreMoves() = board.noMoreMoves()

}