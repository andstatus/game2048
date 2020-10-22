package org.andstatus.game2048

import com.soywiz.klock.DateTimeTz
import com.soywiz.klogger.Console
import com.soywiz.klogger.log

class History() {
    private val keyBest = "best"
    private val keyCurrentGame = "current"

    // 1. Info on previous games
    var bestScore: Int = 0

    // 2. This game, see for the inspiration https://en.wikipedia.org/wiki/Portable_Game_Notation
    var historyIndex = -1
    var currentGame: GameRecord

    init {
        bestScore = settings.storage.getOrNull(keyBest)?.toInt() ?: 0
        Console.log("Best score: ${bestScore}")
        currentGame = settings.storage.getOrNull(keyCurrentGame)
                ?.let { GameRecord.fromJson(it)}
                ?: GameRecord(DateTimeTz.nowLocal(), emptyList(), Board())
    }

    private fun onUpdate() {
        settings.storage[keyBest] = bestScore.toString()
        settings.storage[keyCurrentGame] = currentGame.toJson()
    }

    val currentPlayerMove: PlayerMove?
        get() = when {
            historyIndex < 0 || historyIndex >= currentGame.playerMoves.size ->
                PlayerMove.composerMove(currentGame.finalBoard)
            else -> currentGame.playerMoves[historyIndex]
        }

    fun add(playerMove: PlayerMove, board: Board) {
        currentGame = when (playerMove.playerMoveEnum ) {
            PlayerMoveEnum.LOAD -> GameRecord(DateTimeTz.nowLocal(), emptyList(), board)
            else -> GameRecord(currentGame.start, currentGame.playerMoves + playerMove, board)
        }
        onUpdate()
    }

    fun canUndo(): Boolean {
        return settings.allowUndo && currentGame.playerMoves.size > 1 && historyIndex != 0
    }

    fun undo(): PlayerMove? {
        if (canUndo()) {
            if (historyIndex < 0) historyIndex = currentGame.playerMoves.size - 2 else historyIndex--
        }
        return currentPlayerMove
    }

    fun canRedo(): Boolean {
        return historyIndex >= 0 && historyIndex < currentGame.playerMoves.size - 1
    }

    fun redo(): PlayerMove? {
        if (canRedo()) historyIndex++
        return currentPlayerMove
    }
}