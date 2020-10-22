package org.andstatus.game2048

import kotlin.random.Random

class Model {
    val history: History = History()
    var board: Board = Board()

    val bestScore get() = history.bestScore
    val score get() = board.score

    fun onAppEntry(): List<PlayerMove> {
        return if (history.currentGame.finalBoard.isEmpty())
            restart()
        else
            composerMove(history.currentGame.finalBoard)
    }

    fun composerMove(board: Board) = listOf(PlayerMove.composerMove(board)).playMoves()

    fun restart(): List<PlayerMove> {
        return composerMove(Board()).appendAll(computerMove())
    }

    fun canUndo(): Boolean {
        return history.canUndo()
    }

    fun canRedo(): Boolean {
        return history.canRedo()
    }

    fun undo(): List<PlayerMove> = history.undo()?.let { listOf(it).playMoves() } ?: emptyList()

    fun redo(): List<PlayerMove> = history.redo()?.let { listOf(it).playMoves() } ?: emptyList()

    fun computerMove(): List<PlayerMove> {
        return calcPlacedRandomBlock()?.let { computerMove(it) } ?: emptyList()
    }

    fun computerMove(placedPiece: PlacedPiece): List<PlayerMove> {
        return placedPiece.let { listOf(PlayerMove.computerMove(it)).playMoves() }
    }

    fun calcPlacedRandomBlock(): PlacedPiece?  =
            board.getRandomFreeSquare()?.let {square ->
                val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
                PlacedPiece(piece, square)
            }

    fun userMove(playerMoveEnum: PlayerMoveEnum): List<PlayerMove> {
        return calcMove(playerMoveEnum).let {
            if (it.moves.isNotEmpty() || settings.allowUsersMoveWithoutBlockMoves) {
                listOf(it).playMoves()
            } else {
                emptyList()
            }
        }
    }

    fun calcMove(playerMoveEnum: PlayerMoveEnum): PlayerMove {
        val board = this.board.copy()
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
        return PlayerMove.userMove(playerMoveEnum, moves)
    }

    private fun List<PlayerMove>.playMoves(): List<PlayerMove> {
        forEach {
            val newBoard = playMove(it, board)
            history.add(it, newBoard)
            board = newBoard
        }
        return this
    }

    fun playMove(playerMove: PlayerMove, oldBoard: Board): Board {
        var board = oldBoard.copy()
        for (move in playerMove.moves) {
            board.score += move.points()
            when(move) {
                is MovePlace -> {
                    board[move.first.square] = move.first.piece
                }
                is MoveLoad -> {
                    board = move.board
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
            }
        }
        return board
    }

    fun noMoreMoves() = board.noMoreMoves()

}