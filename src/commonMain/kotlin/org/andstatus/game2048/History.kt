package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korio.serialization.json.toJson

class History() {
    private val keyBest = "best"
    private val keyCurrentGame = "current"
    private val keyGame = "game"
    private val gameIdsRange = 1..200

    // 1. Info on previous games
    var bestScore: Int = 0
    var prevGames: List<GameRecord.ShortRecord> = emptyList()

    // 2. This game, see for the inspiration https://en.wikipedia.org/wiki/Portable_Game_Notation
    var historyIndex = -1
    var currentGame: GameRecord

    init {
        bestScore = settings.storage.getOrNull(keyBest)?.toInt() ?: 0
        Console.log("Best score: ${bestScore}")
        currentGame = settings.storage.getOrNull(keyCurrentGame)
                ?.let { GameRecord.fromJson(it)}
                ?: GameRecord.newWithBoardAndMoves(Board(), emptyList())
        loadPrevGames()
    }

    private fun loadPrevGames() {
        prevGames = gameIdsRange.fold(emptyList(), { acc, ind ->
            settings.storage.getOrNull(keyGame + ind)
                    ?.let { GameRecord.ShortRecord.fromJson(it)}
                    ?.let { acc + it } ?: acc
        })
    }

    fun restoreGame(id: Int): GameRecord? =
            settings.storage.getOrNull(keyGame + id)
                ?.let {
                    Console.log("gameId:$id, json:$it")
                    GameRecord.fromJson(it)
                }
                ?.also {
                    if (it.id == id) {
                        Console.log("Restored game $it")
                    } else {
                        Console.log("Fixed id $id for Restored game $it")
                        it.id = id
                    }
                    currentGame = it
                    onUpdate()
                }

    fun onUpdate(): History {
        if (bestScore < currentGame.score) {
            bestScore = currentGame.score
        }
        settings.storage[keyBest] = bestScore.toString()
        settings.storage[keyCurrentGame] = currentGame.toMap().toJson()
        return this
    }

    fun saveCurrentToHistory() {
        val newGame = currentGame.id <= 0
        val idToStore = if (newGame){
            (gameIdsRange.find { id -> prevGames.none { it.id == id }}
                    ?: prevGames.minByOrNull { it.finalBoard.dateTime }?.id
                    ?: gameIdsRange.first)
                    .also {
                        currentGame.id = it
                        onUpdate()
                    }
        } else {
            currentGame.id
        }
        settings.storage[keyGame + idToStore] = currentGame.toMap().toJson()
        Console.log((if (newGame) "New" else "Old") + " game saved $currentGame")
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
                GameRecord.newWithBoardAndMoves(board, emptyList())
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
                GameRecord(GameRecord.ShortRecord(currentGame.id, currentGame.shortRecord.start, board), playerMoves + playerMove)
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