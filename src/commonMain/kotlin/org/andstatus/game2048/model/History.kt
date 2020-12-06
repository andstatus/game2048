package org.andstatus.game2048.model

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.weeks
import com.soywiz.korio.serialization.json.toJson
import org.andstatus.game2048.myLog
import org.andstatus.game2048.myMeasured
import org.andstatus.game2048.myMeasuredIt
import org.andstatus.game2048.settings

class History() {
    private val keyBest = "best"
    private val keyCurrentGame = "current"
    private val keyGame = "game"

    private val gameIdsRange = 1..60
    private val maxOlderGames = 30

    // 1. Info on previous games
    var bestScore: Int = 0
    var prevGames: List<GameRecord.ShortRecord> = emptyList()

    // 2. This game, see for the inspiration https://en.wikipedia.org/wiki/Portable_Game_Notation
    var historyIndex = -1
    var currentGame: GameRecord

    init {
        bestScore = settings.storage.getOrNull(keyBest)?.toInt() ?: 0
        myLog("Best score: ${bestScore}")
        currentGame = settings.storage.getOrNull(keyCurrentGame)
            ?.let { GameRecord.fromJson(it) }
            ?: GameRecord.newWithBoardAndMoves(Board(), emptyList(), emptyList())
        loadPrevGames()
    }

    private fun loadPrevGames(): History = myMeasured("PrevGames loaded") {
        prevGames = gameIdsRange.fold(emptyList(), { acc, ind ->
            settings.storage.getOrNull(keyGame + ind)
                ?.let { GameRecord.ShortRecord.fromJson(it) }
                ?.let { acc + it } ?: acc
        })
        this
    }

    fun restoreGame(id: Int): GameRecord? =
        settings.storage.getOrNull(keyGame + id)
            ?.let {
                myLog("On restore gameId:$id, json.length:${it.length} ${it.substring(0..200)}...")
                GameRecord.fromJson(it)
            }
            ?.also {
                if (it.id == id) {
                    myLog("Restored game $it")
                } else {
                    myLog("Fixed id $id for Restored game $it")
                    it.id = id
                }
                currentGame = it
                saveCurrent()
            }

    fun saveCurrent(): History {
        if (currentGame.score < 1) return this

        val isNew = currentGame.id <= 0
        myMeasuredIt((if (isNew) "New" else "Old") + " game saved") {
            val idToStore = if (isNew) idForNewGame().also {
                currentGame.id = it
            } else currentGame.id
            updateBestScore()
            settings.storage[keyCurrentGame] = currentGame.toMap().toJson()
            settings.storage[keyGame + idToStore] = currentGame.toMap().toJson()
            currentGame
        }
        return loadPrevGames()
    }

    private fun updateBestScore() {
        if (bestScore < currentGame.score) {
            bestScore = currentGame.score
            settings.storage[keyBest] = bestScore.toString()
        }
    }

    private fun idForNewGame(): Int {
        val maxGames = gameIdsRange.last
        if (prevGames.size > maxOlderGames) {
            val keepAfter = DateTimeTz.nowLocal().minus(1.weeks)
            val olderGames = prevGames.filter { it.finalBoard.dateTime < keepAfter }
            val id = when {
                olderGames.size > 20 -> olderGames.minByOrNull { it.finalBoard.score }?.id
                prevGames.size >= maxGames -> prevGames.minByOrNull { it.finalBoard.score }?.id
                else -> null
            }
            if (id != null) return id
        }
        return (gameIdsRange.find { id -> prevGames.none { it.id == id } }
            ?: prevGames.minByOrNull { it.finalBoard.dateTime }?.id
            ?: gameIdsRange.first)
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
        currentGame = when (playerMove.playerMoveEnum) {
            PlayerMoveEnum.LOAD -> {
                GameRecord.newWithBoardAndMoves(board, emptyList(), emptyList())
            }
            else -> {
                val bookmarksNew = when {
                    historyIndex < 0 -> {
                        currentGame.shortRecord.bookmarks
                    }
                    historyIndex == 0 -> {
                        emptyList()
                    }
                    else -> {
                        currentGame.shortRecord.bookmarks.filterNot { it.moveNumber > historyIndex }
                    }
                }
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
                } + playerMove
                with(currentGame.shortRecord) {
                    GameRecord(GameRecord.ShortRecord(note, id, start, board, bookmarksNew), playerMoves)
                }
            }
        }
        updateBestScore()
        historyIndex = -1
    }

    fun createBookmark() {
        currentGame = with(currentGame.shortRecord) {
            GameRecord(
                GameRecord.ShortRecord(note, id, start, finalBoard, bookmarks + finalBoard.copy()),
                currentGame.playerMoves
            )
        }
        saveCurrent()
    }

    fun deleteBookmark() {
        currentGame = with(currentGame.shortRecord) {
            GameRecord(
                GameRecord.ShortRecord(note, id, start, finalBoard, bookmarks
                    .filterNot { it.moveNumber == finalBoard.moveNumber }),
                currentGame.playerMoves
            )
        }
        saveCurrent()
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

    fun gotoBookmark(board: Board) {
        if (board.moveNumber >= currentGame.shortRecord.finalBoard.moveNumber) {
            historyIndex = -1
        } else {
            historyIndex = board.moveNumber
        }
    }
}