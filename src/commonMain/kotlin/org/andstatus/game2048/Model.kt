package org.andstatus.game2048

import kotlin.random.Random

class Model {
    private val history: History = History()
    private var board: Board = Board()

    val bestScore get() = history.bestScore
    val score get() = board.score

    fun firstMove() : List<Move> =
        history.currentElement?.let {
            restoreState(it)
        } ?: placeRandomBlock()

    private fun restoreState(historyElement: History.Element): List<Move> {
        board = Board.load(historyElement)
        return listOf(MoveLoad(board))
    }

    fun restart(): List<Move> {
        board = Board()
        history.clear()
        return mutableListOf<Move>(MoveLoad(board)).apply { addAll(placeRandomBlock()) }
    }

    fun placeRandomBlock(): List<Move> {
        val newBoard = board.copy()
        val square = newBoard.getRandomFreeSquare() ?: return emptyList()

        val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
        newBoard[square] = piece
        board = newBoard
        history.add(board)
        return listOf(MovePlace(PlacedPiece(piece, square)))
    }

    fun canUndo(): Boolean {
        return history.canUndo()
    }

    fun canRedo(): Boolean {
        return history.canRedo()
    }

    fun undo() = history.undo()?.let { restoreState(it) } ?: emptyList()

    fun redo() = history.redo()?.let { restoreState(it) } ?: emptyList()

    fun moveBlocksTo(moveDirection: Direction): List<Move> {
        val board = this.board.copy()
        val moves = mutableListOf<Move>()
        val direction = moveDirection.reverse()
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
        val points = moves.fold(0, {acc, move -> acc + move.points()})
        board.score = (this.board.score + points)
        if (board.score > history.bestScore) {
            history.bestScore = board.score
        }
        this.board = board
        if (moves.isNotEmpty() || settings.allowUsersMoveWithoutBlockMoves) {
            moves += placeRandomBlock()
        }
        return moves
    }

    fun noMoreMoves() = board.noMoreMoves()

}