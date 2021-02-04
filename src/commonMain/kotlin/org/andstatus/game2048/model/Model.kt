package org.andstatus.game2048.model

import org.andstatus.game2048.Settings

/** @author yvolk@yurivolkov.com */
class Model(val history: History) {
    val settings: Settings = history.settings
    var gameModel = GameModel(settings)
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

    fun composerMove(board: Board, isRedo: Boolean = false) = gameModel.composerMove(board, isRedo).update(isRedo)

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
        gameModel.playReversed(it).update(true)
    } ?: emptyList()

    fun undoToStart(): List<PlayerMove> {
        history.historyIndex = 0
        return composerMove(Board(settings), true)
    }

    fun canRedo(): Boolean {
        return history.canRedo()
    }

    fun redo(): List<PlayerMove> = history.redo()?.let {
        MoveAndModel(it, gameModel.play(it, true)).update(true)
    } ?: emptyList()

    fun redoToCurrent(): List<PlayerMove> {
        history.historyIndex = -1
        return composerMove(history.currentGame.shortRecord.finalBoard, true)
    }

    fun randomComputerMove() = gameModel.randomComputerMove().update()

    fun computerMove(placedPiece: PlacedPiece) = gameModel.computerMove(placedPiece).update()

    fun userMove(playerMoveEnum: PlayerMoveEnum): List<PlayerMove> = gameModel.userMove(playerMoveEnum).let {
        if (it.move.isNotEmpty() || settings.allowUsersMoveWithoutBlockMoves) {
            it.update(false)
        } else {
            emptyList()
        }
    }

    private fun MoveAndModel.update(isRedo: Boolean = false): List<PlayerMove> {
        if (!isRedo) {
            history.add(move, model.board)
        }
        gameModel = model
        return if(move.isEmpty()) emptyList() else listOf(move)
    }

    fun noMoreMoves() = gameModel.noMoreMoves()

}