package org.andstatus.game2048.model

import kotlinx.coroutines.CoroutineScope
import org.andstatus.game2048.Settings

/** @author yvolk@yurivolkov.com */
class Model(private val coroutineScope: CoroutineScope, val history: History) {
    val settings: Settings = history.settings
    var gamePosition = GamePosition.newEmpty(settings.defaultBoard)

    val moveNumber: Int get() = gamePosition.moveNumber
    val isBookmarked get() = history.currentGame.shortRecord.bookmarks.any {
        it.plyNumber == gamePosition.plyNumber
    }
    val gameClock get() = gamePosition.gameClock
    val bestScore get() = history.bestScore
    val score get() = gamePosition.score

    val gameMode: GameMode get() = history.gameMode

    fun onAppEntry(): List<Ply> {
        return if (history.currentGame.id == 0)
            restart()
        else
            composerMove(history.currentGame.shortRecord.finalPosition, true)
    }

    fun gotoBookmark(position: GamePosition): List<Ply> {
        gameMode.modeEnum = GameModeEnum.STOP
        history.gotoBookmark(position)
        return composerMove(position, true)
    }

    fun composerMove(position: GamePosition, isRedo: Boolean = false) =
        gamePosition.composerPly(position, isRedo).update(isRedo)

    fun createBookmark() {
        history.createBookmark(gamePosition)
        saveCurrent()
    }

    fun deleteBookmark() {
        history.deleteBookmark(gamePosition)
        saveCurrent()
    }

    fun restart(): List<Ply> {
        gameMode.modeEnum = GameModeEnum.PLAY
        return composerMove(gamePosition.newEmpty(), false) + Ply.delay() + randomComputerMove()
    }

    fun restoreGame(id: Int): List<Ply> {
        return history.restoreGame(id)?.let {
            saveCurrent()
            redoToCurrent()
        } ?: emptyList()
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
        gamePosition.playReversed(it).update(true)
    } ?: emptyList()

    fun undoToStart(): List<Ply> {
        history.historyIndex = 0
        return composerMove(gamePosition.newEmpty(), true)
    }

    fun canRedo(): Boolean {
        return history.canRedo()
    }

    fun redo(): List<Ply> = history.redo()?.let {
        gamePosition.play(it, true).update(true)
    } ?: emptyList()

    fun redoToCurrent(): List<Ply> {
        history.historyIndex = -1
        return composerMove(history.currentGame.shortRecord.finalPosition, true)
    }

    fun randomComputerMove() = gamePosition.randomComputerPly().update()

    fun computerMove(placedPiece: PlacedPiece) = gamePosition.computerPly(placedPiece).update()

    fun userMove(plyEnum: PlyEnum): List<Ply> = gamePosition.userPly(plyEnum).update()

    private fun GamePosition.update(isRedo: Boolean = false): List<Ply> {
        if (!isRedo && prevPly.isNotEmpty()) {
            history.add(this)
        }
        gamePosition = this
        return if (prevPly.isEmpty()) emptyList() else listOf(prevPly)
    }

    fun noMoreMoves() = gamePosition.noMoreMoves()

}