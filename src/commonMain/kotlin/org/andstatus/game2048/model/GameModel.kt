package org.andstatus.game2048.model

import org.andstatus.game2048.Settings
import kotlin.random.Random

/** @author yvolk@yurivolkov.com */
class GameModel(val settings: Settings, val board: Board, val onMoveHandler: (PlayerMove, Board) -> Unit) {
    val usersMoveNumber: Int get() = board.usersMoveNumber
    val gameClock get() = board.gameClock
    val score get() = board.score

    constructor(settings: Settings, onMoveHandler: (PlayerMove, Board) -> Unit) :
            this(settings, Board(settings), onMoveHandler)

    fun Board.nextModel() = GameModel(settings, this, onMoveHandler)

    fun composerMove(board: Board, isRedo: Boolean = false) =
            listOf(PlayerMove.composerMove(board)).play(isRedo)

    fun randomComputerMove(): MovesAndModel {
        return calcPlacedRandomBlock()?.let { computerMove(it) } ?: MovesAndModel(emptyList(), this)
    }

    fun computerMove(placedPiece: PlacedPiece): MovesAndModel {
        return placedPiece.let { listOf(PlayerMove.computerMove(it, gameClock.playedSeconds)).play() }
    }

    private fun calcPlacedRandomBlock(): PlacedPiece?  =
            board.getRandomFreeSquare()?.let {square ->
                val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
                PlacedPiece(piece, square)
            }

    fun userMove(playerMoveEnum: PlayerMoveEnum): MovesAndModel {
        return calcMove(playerMoveEnum).let {
            if (it.moves.isNotEmpty() || settings.allowUsersMoveWithoutBlockMoves) {
                gameClock.start()
                listOf(it).play()
            } else {
                MovesAndModel(emptyList(), this)
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

    fun play(move: PlayerMove): MovesAndModel = listOf(move).play(false)

    fun List<PlayerMove>.play(isRedo: Boolean = false): MovesAndModel {
        var newBoard = board
        forEach { playerMove ->
            newBoard = play(playerMove, isRedo, newBoard)
            if (!isRedo) onMoveHandler(playerMove, newBoard)
        }
        return MovesAndModel(this, GameModel(settings, newBoard, onMoveHandler))
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

    fun playReversed(moves: List<PlayerMove>): MovesAndModel {
        var newBoard = board
        moves.asReversed().forEach {
            newBoard = playReversed(it, newBoard)
        }
        return MovesAndModel(moves, newBoard.nextModel())
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