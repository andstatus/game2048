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
            composerMove(history.currentGame.finalBoard, true)
    }

    fun composerMove(board: Board, isRedo: Boolean = false) = listOf(PlayerMove.composerMove(board)).play(isRedo)

    fun restart(): List<PlayerMove> {
        return composerMove(Board()) + PlayerMove.delay() + computerMove()
    }

    fun canUndo(): Boolean {
        return history.canUndo()
    }

    fun canRedo(): Boolean {
        return history.canRedo()
    }

    fun undo(): List<PlayerMove> = history.undo()?.let { listOf(it).playReversed() } ?: emptyList()

    fun redo(): List<PlayerMove> = history.redo()?.let { listOf(it).play(true) } ?: emptyList()

    fun computerMove(): List<PlayerMove> {
        return calcPlacedRandomBlock()?.let { computerMove(it) } ?: emptyList()
    }

    fun computerMove(placedPiece: PlacedPiece): List<PlayerMove> {
        return placedPiece.let { listOf(PlayerMove.computerMove(it)).play() }
    }

    fun calcPlacedRandomBlock(): PlacedPiece?  =
            board.getRandomFreeSquare()?.let {square ->
                val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
                PlacedPiece(piece, square)
            }

    fun userMove(playerMoveEnum: PlayerMoveEnum): List<PlayerMove> {
        return calcMove(playerMoveEnum).let {
            if (it.moves.isNotEmpty() || settings.allowUsersMoveWithoutBlockMoves) {
                listOf(it).play()
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

    private fun List<PlayerMove>.play(isRedo: Boolean = false): List<PlayerMove> {
        forEach { playerMove ->
            board = play(playerMove, board).also { newBoard ->
                if (!isRedo) history.add(playerMove, newBoard)
            }
        }
        return this
    }

    private fun play(playerMove: PlayerMove, oldBoard: Board): Board {
        var board = oldBoard.copy()
        playerMove.moves.forEach { move ->
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

    private fun List<PlayerMove>.playReversed(): List<PlayerMove> {
        asReversed().forEach { board = playReversed(it, board) }
        return this
    }

    private fun playReversed(playerMove: PlayerMove, oldBoard: Board): Board {
        var board = oldBoard.copy()
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
            }
        }
        return board
    }

    fun noMoreMoves() = board.noMoreMoves()

}