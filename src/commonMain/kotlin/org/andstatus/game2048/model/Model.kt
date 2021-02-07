package org.andstatus.game2048.model

import kotlinx.coroutines.CoroutineScope
import org.andstatus.game2048.Settings

/** @author yvolk@yurivolkov.com */
class Model(private val coroutineScope: CoroutineScope, val history: History) {
    val settings: Settings = history.settings
    var gameModel = GameModel(settings)
    val board: Board get() = gameModel.board

    val moveNumber: Int get() = board.moveNumber
    val isBookmarked get() = history.currentGame.shortRecord.bookmarks.any { it.plyNumber == board.plyNumber }
    val gameClock get() = board.gameClock
    val bestScore get() = history.bestScore
    val score get() = board.score

    val gameMode: GameMode get() = history.gameMode

    fun onAppEntry(): List<Ply> {
        return if (history.currentGame.id == 0)
            restart()
        else
            composerMove(history.currentGame.shortRecord.finalBoard, true)
    }

    fun gotoBookmark(board: Board): List<Ply> {
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

    fun restart(): List<Ply> {
        gameMode.modeEnum = GameModeEnum.PLAY
        return composerMove(Board(settings), false) + Ply.delay() + randomComputerMove()
    }

    fun restoreGame(id: Int): List<Ply> {
        return history.restoreGame(id)?.let { redoToCurrent() } ?: emptyList()
    }

    fun pauseGame() {
        if (!gameClock.started) return

        gameClock.stop()
        gameMode.modeEnum = when (gameMode.modeEnum) {
            GameModeEnum.PLAY, GameModeEnum.AI_PLAY -> GameModeEnum.PLAY
            else -> GameModeEnum.STOP
        }
        saveCurrent()
    }

    fun saveCurrent() = history.saveCurrent(coroutineScope)

    fun canUndo(): Boolean {
        return history.canUndo()
    }

    fun undo(): List<Ply> = history.undo()?.let {
        gameModel.playReversed(it).update(true)
    } ?: emptyList()

    fun undoToStart(): List<Ply> {
        history.historyIndex = 0
        return composerMove(Board(settings), true)
    }

    fun canRedo(): Boolean {
        return history.canRedo()
    }

    fun redo(): List<Ply> = history.redo()?.let {
        gameModel.play(it, true).update(true)
    } ?: emptyList()

    fun redoToCurrent(): List<Ply> {
        history.historyIndex = -1
        return composerMove(history.currentGame.shortRecord.finalBoard, true)
    }

    fun randomComputerMove() = gameModel.randomComputerMove().update()

    fun computerMove(placedPiece: PlacedPiece) = gameModel.computerMove(placedPiece).update()

    fun userMove(plyEnum: PlyEnum): List<Ply> = gameModel.userMove(plyEnum).update()

    private fun GameModel.update(isRedo: Boolean = false): List<Ply> {
        if (!isRedo && prevMove.isNotEmpty()) {
            history.add(prevMove, board)
        }
        gameModel = this
        return if (prevMove.isEmpty()) emptyList() else listOf(prevMove)
    }

    fun noMoreMoves() = gameModel.noMoreMoves()

}