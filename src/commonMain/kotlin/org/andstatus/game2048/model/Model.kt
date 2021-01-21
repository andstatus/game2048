package org.andstatus.game2048.model

import org.andstatus.game2048.Settings

/** @author yvolk@yurivolkov.com */
class Model(val history: History) {
    val settings: Settings = history.settings
    val gameModel = GameModel(settings) {
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
        return if (history.currentGame.score == 0)
            restart()
        else
            composerMove(history.currentGame.shortRecord.finalBoard, true)
    }

    fun gotoBookmark(board: Board): List<PlayerMove> {
        gameMode.modeEnum = GameModeEnum.STOP
        history.gotoBookmark(board)
        return composerMove(board, true)
    }

    val composerMove = gameModel::composerMove

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

    fun saveCurrent() = history.saveCurrent()

    fun canUndo(): Boolean {
        return history.canUndo()
    }

    fun undo(): List<PlayerMove> = history.undo()?.let {
        gameModel.playReversed(listOf(it))
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

    val randomComputerMove = gameModel::randomComputerMove
    val computerMove = gameModel::computerMove
    val userMove = gameModel::userMove

    private fun List<PlayerMove>.play(isRedo: Boolean = false): List<PlayerMove> {
        with(gameModel) {
            return play(isRedo)
        }
    }

    val noMoreMoves = gameModel::noMoreMoves

}