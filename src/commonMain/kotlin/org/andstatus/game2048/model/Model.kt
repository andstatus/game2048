package org.andstatus.game2048.model

import korlibs.io.concurrent.atomic.KorAtomicRef
import korlibs.io.concurrent.atomic.korAtomic
import org.andstatus.game2048.MyContext

/** @author yvolk@yurivolkov.com */
class Model(val history: History) {
    val myContext: MyContext = history.myContext
    private val gamePositionRef = korAtomic(GamePosition(myContext.defaultBoard))
    val gamePosition get() = gamePositionRef.value

    val moveNumber: Int get() = gamePosition.moveNumber
    val isBookmarked
        get() = history.currentGame.shortRecord.bookmarks.any {
            it.plyNumber == gamePosition.plyNumber
        }
    val gameClock get() = gamePosition.gameClock
    val bestScore get() = history.bestScore
    val score get() = gamePosition.score

    val gameMode: GameMode get() = history.gameMode
    val nextComputerPlacedPeace: KorAtomicRef<PlacedPiece?> = korAtomic(null)

    fun gotoBookmark(position: GamePosition): List<Ply> {
        gameMode.modeEnum = GameModeEnum.STOP
        history.gotoBookmark(position)
        return composerPly(position, true)
    }

    fun composerPly(position: GamePosition, isRedo: Boolean = false) =
        gamePosition.composerPly(position, isRedo).update(isRedo)

    fun createBookmark() {
        history.createBookmark(gamePosition)
        saveCurrent()
    }

    fun deleteBookmark() {
        history.deleteBookmark(gamePosition)
        saveCurrent()
    }

    fun tryAgain(): List<Ply> = history.openNewGame().let {
        composerPly(it.shortRecord.finalPosition, false) + Ply.delay() + randomComputerMove()
    }

    fun openGame(id: Int): List<Ply> {
        return history.openGame(id)?.let {
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

    private fun saveCurrent() = history.saveCurrent(history.myContext.multithreadedScope)

    fun canUndo(): Boolean {
        return history.canUndo()
    }

    fun undo(): List<Ply> = history.undo()?.let {
        gamePosition.playReversed(it).update(true)
    } ?: emptyList()

    fun undoToStart(): List<Ply> {
        history.redoPlyPointer = 1
        return composerPly(gamePosition.newEmpty(), true)
    }

    fun canRedo(): Boolean {
        return history.canRedo()
    }

    fun redo(): List<Ply> = history.redo()?.let {
        gamePosition.play(it, true).update(true)
    } ?: emptyList()

    fun redoToCurrent(): List<Ply> = history.currentGame.let { game ->
        history.redoPlyPointer = 0
        return composerPly(game.shortRecord.finalPosition, true)
    }

    fun randomComputerMove() = (nextComputerPlacedPeace.value?.let {
        nextComputerPlacedPeace.value = null
        gamePosition.computerPly(it)
    } ?: gamePosition.randomComputerPly()).update()

    fun computerMove(placedPiece: PlacedPiece) = gamePosition.computerPly(placedPiece).update()

    fun userMove(plyEnum: PlyEnum): List<Ply> = gamePosition.userPly(plyEnum, history.plyToRedo).update()

    private fun PlyAndPosition.update(isRedo: Boolean = false): List<Ply> {
        if (ply.isEmpty()) return emptyList()

        if (!isRedo) {
            history.add(this)
        }
        if (gameMode.isPlaying) {
            gameClock.start()
        }
        gamePositionRef.value = this.position
        return listOf(ply)
    }

    fun noMoreMoves() = gamePosition.noMoreMoves()

}
