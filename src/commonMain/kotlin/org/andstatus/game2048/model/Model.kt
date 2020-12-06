package org.andstatus.game2048.model

import org.andstatus.game2048.settings
import kotlin.random.Random

class Model {
    val history: History = History()
    var board: Board = Board()

    val usersMoveNumber: Int get() = board.usersMoveNumber
    val isBookmarked get() = history.currentGame.shortRecord.bookmarks.any { it.moveNumber == board.moveNumber }
    val gameClock get() = board.gameClock
    val bestScore get() = history.bestScore
    val score get() = board.score

    fun onAppEntry(): List<PlayerMove> {
        return if (history.currentGame.score == 0)
            restart()
        else
            composerMove(history.currentGame.shortRecord.finalBoard, true)
    }

    fun gotoBookmark(board: Board): List<PlayerMove> {
        history.gotoBookmark(board)
        return composerMove(board, isRedo = true)
    }

    fun composerMove(board: Board, isRedo: Boolean = false) =
            listOf(PlayerMove.composerMove(board)).play(isRedo)

    fun createBookmark() {
        history.createBookmark()
    }

    fun deleteBookmark() {
        history.deleteBookmark()
    }

    fun restart(): List<PlayerMove> {
        return composerMove(Board()) + PlayerMove.delay() + computerMove()
    }

    fun restoreGame(id: Int): List<PlayerMove> {
        return history.restoreGame(id)?.let { redoToCurrent() } ?: emptyList()
    }

    fun saveCurrent() = history.saveCurrent()

    fun canUndo(): Boolean {
        return history.canUndo()
    }

    fun undo(): List<PlayerMove> = history.undo()?.let { listOf(it).playReversed() } ?: emptyList()

    fun undoToStart(): List<PlayerMove> {
        history.historyIndex = 0
        return composerMove(Board(), true)
    }

    fun canRedo(): Boolean {
        return history.canRedo()
    }

    fun redo(): List<PlayerMove> = history.redo()?.let { listOf(it).play(true) } ?: emptyList()

    fun redoToCurrent(): List<PlayerMove> {
        history.historyIndex = -1
        return composerMove(history.currentGame.shortRecord.finalBoard, true)
    }

    fun computerMove(): List<PlayerMove> {
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

    private fun List<PlayerMove>.play(isRedo: Boolean = false): List<PlayerMove> {
        forEach { playerMove ->
            board = play(playerMove, isRedo, board).also { newBoard ->
                if (!isRedo) history.add(playerMove, newBoard)
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

    private fun List<PlayerMove>.playReversed(): List<PlayerMove> {
        asReversed().forEach { board = playReversed(it, board) }
        return this
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