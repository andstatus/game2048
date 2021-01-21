package org.andstatus.game2048.model

import org.andstatus.game2048.Settings
import kotlin.random.Random

/** @author yvolk@yurivolkov.com */
class GameModel(val settings: Settings, val onMoveHandler: (PlayerMove, Board) -> Unit) {
    var board: Board = Board(settings)

    val usersMoveNumber: Int get() = board.usersMoveNumber
    val gameClock get() = board.gameClock
    val score get() = board.score

    fun composerMove(board: Board, isRedo: Boolean = false) =
            listOf(PlayerMove.composerMove(board)).play(isRedo)

    fun randomComputerMove(): List<PlayerMove> {
        return calcPlacedRandomBlock()?.let { computerMove(it) } ?: emptyList()
    }

    fun computerMove(placedPiece: PlacedPiece): List<PlayerMove> {
        return placedPiece.let { listOf(PlayerMove.computerMove(it, gameClock.playedSeconds)).play() }
    }

    private fun calcPlacedRandomBlock(): PlacedPiece?  =
            board.getRandomFreeSquare()?.let {square ->
                val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
                PlacedPiece(piece, square)
            }

    fun userMove(playerMoveEnum: PlayerMoveEnum): List<PlayerMove> {
        return calcMove(playerMoveEnum).let {
            if (it.moves.isNotEmpty() || settings.allowUsersMoveWithoutBlockMoves) {
                gameClock.start()
                listOf(it).play()
            } else {
                emptyList()
            }
        }
    }

    private fun calcMove(playerMoveEnum: PlayerMoveEnum): PlayerMove {
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

    fun List<PlayerMove>.play(isRedo: Boolean = false): List<PlayerMove> {
        forEach { playerMove ->
            board = play(playerMove, isRedo, board).also { newBoard ->
                if (!isRedo) onMoveHandler(playerMove, newBoard)
            }
        }
        return this
    }

    private fun play(playerMove: PlayerMove, isRedo: Boolean = false, oldBoard: Board): Board {
        var board = if (isRedo) oldBoard.forAutoPlaying(playerMove.seconds, true) else oldBoard.forNextMove()
        playerMove.moves.forEach { move ->
            board.score += move.points()
            when(move) {
                is MovePlace -> {
                    board[move.first.square] = move.first.piece
                }
                is MoveLoad -> {
                    board = move.board.copy()
                }
                is MoveOne -> {
                    board[move.first.square] = null
                    board[move.destination] = move.first.piece
                }
                is MoveMerge -> {
                    board[move.first.square] = null
                    board[move.second.square] = null
                    board[move.merged.square] = move.merged.piece
                }
                is MoveDelay -> Unit
            }
        }
        return board
    }

    fun playReversed(moves: List<PlayerMove>): List<PlayerMove> {
        moves.asReversed().forEach { board = playReversed(it, board) }
        return moves
    }

    private fun playReversed(playerMove: PlayerMove, oldBoard: Board): Board {
        var board = oldBoard.forAutoPlaying(playerMove.seconds, false)
        playerMove.moves.asReversed().forEach { move ->
            board.score -= move.points()
            when(move) {
                is MovePlace -> {
                    board[move.first.square] = null
                }
                is MoveLoad -> {
                    board = move.board
                }
                is MoveOne -> {
                    board[move.destination] = null
                    board[move.first.square] = move.first.piece
                }
                is MoveMerge -> {
                    board[move.merged.square] = null
                    board[move.second.square] = move.second.piece
                    board[move.first.square] = move.first.piece
                }
                is MoveDelay -> Unit
            }
        }
        return board
    }

    fun noMoreMoves() = board.noMoreMoves()

}