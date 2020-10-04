package org.andstatus.game2048

import kotlin.random.Random

class Model {
    private var board: Board = Board()
    private val history: History

    init {
        val keyHistory = "history"
        history = History(settings.storage.getOrNull(keyHistory)) { settings.storage[keyHistory] = it.toString() }
    }

    fun firstMove() : List<Move> =
        history.currentElement?.let {
            restoreState(it)
        } ?: placeRandomBlock()

    private fun restoreState(history: History.Element): List<Move> {
        val newBoard = Board()
        newBoard.load(history.pieceIds)
        board = newBoard
        return listOf(MoveLoad(board, history.score))
    }

    fun restart(): List<Move> {
        board = Board()
        history.clear()
        return mutableListOf<Move>(MoveLoad(board, 0)).apply { addAll(placeRandomBlock()) }
    }

    fun placeRandomBlock(): List<Move> {
        val newBoard = board.copy()
        val square = newBoard.getRandomFreeSquare() ?: return emptyList<Move>()

        val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
        newBoard[square] = piece
        board = newBoard
        history.add(board.save(), score.value)
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

    fun moveBlocksOnTheBoard(moveDirection: Direction): List<Move> {
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
        this.board = board
        if (moves.isNotEmpty() || settings.allowUsersMoveWithoutBlockMoves) {
            moves += placeRandomBlock()
        }
        val points = moves.fold(0, {acc, move -> acc + move.points()})
        score.update(score.value + points)
        return moves
    }

    fun noMoreMoves() = board.noMoreMoves()
}