package org.andstatus.game2048

import com.soywiz.klock.DateTimeTz
import com.soywiz.klogger.Console
import com.soywiz.klogger.log

class History() {
    private val keyBest = "best"
    private val keyCurrentGame = "current"
    private val keyGame = "game"
    private val gameIdsRange = 1..200

    // 1. Info on previous games
    var bestScore: Int = 0
    var prevGames: List<GameSummary> = emptyList()

    // 2. This game, see for the inspiration https://en.wikipedia.org/wiki/Portable_Game_Notation
    var historyIndex = -1
    var currentGame: GameRecord

    init {
        bestScore = settings.storage.getOrNull(keyBest)?.toInt() ?: 0
        Console.log("Best score: ${bestScore}")
        currentGame = settings.storage.getOrNull(keyCurrentGame)
                ?.let { GameRecord.fromJson(it)}
                ?: GameRecord(DateTimeTz.nowLocal(), emptyList(), Board())
        loadPrevGames()
    }

    private fun loadPrevGames() {
        prevGames = gameIdsRange.fold(emptyList(), { acc, ind ->
            settings.storage.getOrNull(keyGame + ind)
                    ?.let { GameSummary.fromJson(it)}
                    ?.let { acc + it } ?: acc
        })
    }

    fun restoreGameByIndex(historyIndex: Int): GameRecord? =
        prevGames.getOrNull(historyIndex)
            ?.let { settings.storage.getOrNull(keyGame + it.id) }
            ?.let { GameRecord.fromJson(it) }
            ?.let {
                Console.log("Restored game ${it.start}")
                currentGame = it
                it
            }

    fun onUpdate(): History {
        if (bestScore < currentGame.finalBoard.score) {
            bestScore = currentGame.finalBoard.score
        }
        settings.storage[keyBest] = bestScore.toString()
        settings.storage[keyCurrentGame] = currentGame.toJson()
        return this
    }

    fun saveCurrentToHistory() {
        val idToStore = if (currentGame.id > 0){
            currentGame.id
        } else {
            (gameIdsRange.find { id -> prevGames.none { it.id == id }}
                    ?: prevGames.minByOrNull { it.finalBoard.time }?.id
                    ?: gameIdsRange.first)
                .also {
                    currentGame.id = it
                }
        }
        settings.storage[keyGame + idToStore] = currentGame.toJson()
        Console.log("Game $idToStore saved ${currentGame.start}")
        loadPrevGames()
    }

    fun deleteCurrent() {
        if (currentGame.id == 0) return

        settings.storage.remove(keyGame + currentGame.id)
        loadPrevGames()
    }

    val currentPlayerMove: PlayerMove?
        get() = when {
            historyIndex < 0 || historyIndex >= currentGame.playerMoves.size -> null
            else -> currentGame.playerMoves[historyIndex]
        }

    fun add(playerMove: PlayerMove, board: Board) {
        currentGame = when (playerMove.playerMoveEnum ) {
            PlayerMoveEnum.LOAD -> {
                GameRecord(DateTimeTz.nowLocal(), emptyList(), board)
            }
            else -> {
                val playerMoves = when {
                    historyIndex < 0 -> {
                        currentGame.playerMoves
                    }
                    historyIndex == 0 -> {
                        emptyList()
                    }
                    else -> {
                        currentGame.playerMoves.take(historyIndex)
                    }
                }
                GameRecord(currentGame.start, playerMoves + playerMove, board)
            }
        }
        historyIndex = -1
        onUpdate()
    }

    fun canUndo(): Boolean = settings.allowUndo &&
            historyIndex != 0 && historyIndex != 1 &&
            currentGame.playerMoves.size > 1 &&
            currentGame.playerMoves.lastOrNull()?.player == PlayerEnum.COMPUTER

    fun undo(): PlayerMove? {
        if (historyIndex < 0 && currentGame.playerMoves.size > 0) {
            historyIndex = currentGame.playerMoves.size - 1
        } else if (historyIndex > 0 && historyIndex < currentGame.playerMoves.size)
            historyIndex--
        else {
            return null
        }
        return currentPlayerMove
    }

    fun canRedo(): Boolean {
        return historyIndex >= 0 && historyIndex < currentGame.playerMoves.size
    }

    fun redo(): PlayerMove? {
        if (canRedo()) {
            return currentPlayerMove?.also {
                if (historyIndex < currentGame.playerMoves.size - 1)
                    historyIndex++
                else {
                    historyIndex = -1
                }
            }
        }
        historyIndex = -1
        return null
    }
}