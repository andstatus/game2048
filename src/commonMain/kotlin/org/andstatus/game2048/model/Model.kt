package org.andstatus.game2048.model

import org.andstatus.game2048.Settings

/** @author yvolk@yurivolkov.com */
class Model(val history: History) {
    val settings: Settings = history.settings
    var gameModel = GameModel(settings) {
            playerMove, newBoard -> history.add(playerMove, newBoard)
    }
    val board: Board get() = gameModel.board

    val usersMoveNumber: Int get() = board.usersMoveNumber
    val isBookmarked get() = history.currentGame.shortRecord.bookmarks.any { it.moveNumber == board.moveNumber }
    val gameClock get() = board.gameClock
    val bestScore get() = history.bestScore
    val score get() = board.score

    val gameMode: GameMode get() = history.gameMode

    fun onAppEntry(): List<PlayerMove> {
        return if (history.currentGame.id == 0)
            restart()
        else
            composerMove(history.currentGame.shortRecord.finalBoard, true)
    }

    fun gotoBookmark(board: Board): List<PlayerMove> {
        gameMode.modeEnum = GameModeEnum.STOP
        history.gotoBookmark(board)
        return composerMove(board, true)
    }

    fun composerMove(board: Board, isRedo: Boolean = false) = gameModel.composerMove(board, isRedo).toMoves()

    fun createBookmark() {
        history.createBookmark()
    }

    fun deleteBookmark() {
        history.deleteBookmark()
    }

    fun restart(): List<PlayerMove> {
        gameMode.modeEnum = GameModeEnum.PLAY
        return composerMove(Board(settings), false) + PlayerMove.delay() + randomComputerMove()
    }

    fun restoreGame(id: Int): List<PlayerMove> {
        return history.restoreGame(id)?.let { redoToCurrent() } ?: emptyList()
    }

    fun pauseGame() {
        gameClock.stop()
        gameMode.modeEnum = when(gameMode.modeEnum) {
            GameModeEnum.PLAY, GameModeEnum.AI_PLAY -> GameModeEnum.PLAY
            else -> GameModeEnum.STOP
        }
        saveCurrent()
    }

    fun saveCurrent() = history.saveCurrent()

    fun canUndo(): Boolean {
        return history.canUndo()
    }

    fun undo(): List<PlayerMove> = history.undo()?.let {
        gameModel.playReversed(listOf(it)).toMoves()
    } ?: emptyList()

    fun undoToStart(): List<PlayerMove> {
        history.historyIndex = 0
        return composerMove(Board(settings), true)
    }

    fun canRedo(): Boolean {
        return history.canRedo()
    }

    fun redo(): List<PlayerMove> = history.redo()?.let { listOf(it).play(true) } ?: emptyList()

    fun redoToCurrent(): List<PlayerMove> {
        history.historyIndex = -1
        return composerMove(history.currentGame.shortRecord.finalBoard, true)
    }

    fun randomComputerMove() = gameModel.randomComputerMove().toMoves()
    fun computerMove(placedPiece: PlacedPiece) = gameModel.computerMove(placedPiece).toMoves()
    fun userMove(playerMoveEnum: PlayerMoveEnum) = gameModel.userMove(playerMoveEnum).toMoves()

    private fun List<PlayerMove>.play(isRedo: Boolean = false): List<PlayerMove> {
        with(gameModel) {
            return play(isRedo).toMoves()
        }
    }

    fun MovesAndModel.toMoves(): List<PlayerMove> {
        gameModel = model
        return moves
    }

    fun noMoreMoves() = gameModel.noMoreMoves()

}