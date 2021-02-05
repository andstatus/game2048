package org.andstatus.game2048.model

import org.andstatus.game2048.Settings
import kotlin.random.Random

/** @author yvolk@yurivolkov.com */
class GameModel(val settings: Settings, val prevMove: PlayerMove, val board: Board) {
    val usersMoveNumber: Int get() = board.usersMoveNumber
    val gameClock get() = board.gameClock
    val score get() = board.score

    constructor(settings: Settings) : this(settings, PlayerMove.emptyMove, Board(settings))

    fun PlayerMove.nextModel(board: Board) = GameModel(settings, this, board)

    fun composerMove(board: Board, isRedo: Boolean = false): GameModel {
        val move = PlayerMove.composerMove(board)
        return play(move, isRedo)
    }

    fun randomComputerMove(): GameModel {
        return calcPlacedRandomBlock()?.let { computerMove(it) } ?: PlayerMove.emptyMove.nextModel(board)
    }

    fun computerMove(placedPiece: PlacedPiece): GameModel {
        return placedPiece.let {
            val move = PlayerMove.computerMove(it, gameClock.playedSeconds)
            play(move, false)
        }
    }

    private fun calcPlacedRandomBlock(): PlacedPiece? =
        board.getRandomFreeSquare()?.let { square ->
            val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
            PlacedPiece(piece, square)
        }

    fun userMove(playerMoveEnum: PlayerMoveEnum): GameModel {
        return calcMove(playerMoveEnum).let {
            if (it.isNotEmpty() || settings.allowUsersMoveWithoutBlockMoves) {
                gameClock.start()
                play(it, false)
            } else {
                PlayerMove.emptyMove.nextModel(board)
            }
        }
    }

    fun calcMove(playerMoveEnum: PlayerMoveEnum): PlayerMove {
        val board = this.board.forNextMove()
        val moves = mutableListOf<Move>()
        val direction = playerMoveEnum.reverseDirection()
        var square: Square? = board.firstSquareToIterate(direction)
        while (square != null) {
            val found = square.nextPlacedPieceInThe(direction, board)
            if (found == null) {
                square = square.nextToIterate(direction, board)
            } else {
                board[found.square] = null
                val next = found.square.nextInThe(direction, board)?.nextPlacedPieceInThe(direction, board)
                if (next != null && found.piece == next.piece) {
                    // merge equal blocks
                    val merged = found.piece.next()
                    board[square] = merged
                    board[next.square] = null
                    moves += MoveMerge(found, next, PlacedPiece(merged, square))
                    if (!settings.allowResultingTileToMerge) {
                        square = square.nextToIterate(direction, board)
                    }
                } else {
                    if (found.square != square) {
                        moves += MoveOne(found, square)
                    }
                    board[square] = found.piece
                    square = square.nextToIterate(direction, board)
                }
            }
        }
        return PlayerMove.userMove(playerMoveEnum, gameClock.playedSeconds, moves)
    }

    fun play(playerMove: PlayerMove, isRedo: Boolean = false): GameModel {
        var newBoard = if (isRedo) board.forAutoPlaying(playerMove.seconds, true) else board.forNextMove()
        playerMove.moves.forEach { move ->
            newBoard.score += move.points()
            when (move) {
                is MovePlace -> {
                    newBoard[move.first.square] = move.first.piece
                }
                is MoveLoad -> {
                    newBoard = move.board.copy()
                }
                is MoveOne -> {
                    newBoard[move.first.square] = null
                    newBoard[move.destination] = move.first.piece
                }
                is MoveMerge -> {
                    newBoard[move.first.square] = null
                    newBoard[move.second.square] = null
                    newBoard[move.merged.square] = move.merged.piece
                }
                is MoveDelay -> Unit
            }
        }
        return playerMove.nextModel(newBoard)
    }

    fun playReversed(playerMove: PlayerMove): GameModel {
        var newBoard = board.forAutoPlaying(playerMove.seconds, false)
        playerMove.moves.asReversed().forEach { move ->
            newBoard.score -= move.points()
            when (move) {
                is MovePlace -> {
                    newBoard[move.first.square] = null
                }
                is MoveLoad -> {
                    newBoard = move.board
                }
                is MoveOne -> {
                    newBoard[move.destination] = null
                    newBoard[move.first.square] = move.first.piece
                }
                is MoveMerge -> {
                    newBoard[move.merged.square] = null
                    newBoard[move.second.square] = move.second.piece
                    newBoard[move.first.square] = move.first.piece
                }
                is MoveDelay -> Unit
            }
        }
        return playerMove.nextModel(newBoard)
    }

    fun noMoreMoves() = board.noMoreMoves()

}