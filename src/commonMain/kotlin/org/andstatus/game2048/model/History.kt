package org.andstatus.game2048.model

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.weeks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.GameRecord.ShortRecord.Companion.fromJsonMap
import org.andstatus.game2048.myLog
import org.andstatus.game2048.myMeasured
import org.andstatus.game2048.myMeasuredIt

private val keyCurrentGame = "current"
private val keyGame = "game"
val keyGameMode = "gameMode"

private val gameIdsRange = 1..60
private val maxOlderGames = 30

/** @author yvolk@yurivolkov.com */
class History(val settings: Settings,
              var currentGame: GameRecord,
              var recentGames: List<GameRecord.ShortRecord> = emptyList()) {
    private val keyBest = "best"

    // 1. Info on previous games
    var bestScore: Int = settings.storage.getOrNull(keyBest)?.toInt() ?: 0

    // 2. This game, see for the inspiration https://en.wikipedia.org/wiki/Portable_Game_Notation
    var historyIndex = -1
    val gameMode: GameMode = GameMode().apply {
        modeEnum = GameModeEnum.fromId(settings.storage.getOrNull(keyGameMode) ?: "").let {
            when(it) {
                GameModeEnum.AI_PLAY, GameModeEnum.PLAY -> GameModeEnum.PLAY
                else -> GameModeEnum.STOP
            }
        }
    }

    companion object {
        suspend fun load(settings: Settings): History = coroutineScope {
            val dCurrentGame = async {
                myMeasured("Current game loaded") {
                    settings.storage.getOrNull(keyCurrentGame)
                            ?.let { GameRecord.fromJson(settings, it) }
                            ?: GameRecord.newWithPositionAndMoves(
                                    GamePosition(settings.defaultBoard), emptyList(), emptyList())
                }
            }
            History(settings, dCurrentGame.await())
        }

        private fun loadRecentGames(settings: Settings): List<GameRecord.ShortRecord> = myMeasured("Recent games loaded") {
            gameIdsRange.fold(emptyList()) { acc, ind ->
                settings.storage.getOrNull(keyGame + ind)
                    ?.let { fromJsonMap(settings, it.asJsonMap(), null) }
                    ?.let { acc + it } ?: acc
            }
        }
    }

    fun loadRecentGames(): History {
        recentGames = loadRecentGames(settings)
        return this
    }

    fun openGame(id: Int): GameRecord? =
        settings.storage.getOrNull(keyGame + id)
            ?.let {
                myLog("On open gameId:$id, json.length:${it.length} ${it.substring(0..200)}...")
                GameRecord.fromJson(settings, it)
            }
            ?.also {
                if (it.id == id) {
                    myLog("Opened game $it")
                } else {
                    myLog("Fixed id $id while opening game $it")
                    it.id = id
                }
                currentGame = it
                gameMode.modeEnum = GameModeEnum.STOP
            }
            ?: run {
                myLog("Failed to open game $id")
                null
            }


    fun saveCurrent(coroutineScope: CoroutineScope? = null): History {
        settings.storage[keyGameMode] = gameMode.modeEnum.id
        val isNew = currentGame.id <= 0

        if (isNew && currentGame.score < 1) {
            // Store only the latest game without score
            recentGames.filter { it.finalPosition.score < 1 }.forEach {
                settings.storage.native.remove(keyGame + it.id)
            }
        }

        myMeasuredIt((if (isNew) "New" else "Old") + " game saved") {
            val idToStore = if (isNew) idForNewGame().also {
                currentGame.id = it
            } else currentGame.id
            updateBestScore()
            currentGame.toJsonString().let {
                settings.storage[keyCurrentGame] = it
                settings.storage[keyGame + idToStore] = it
            }
            currentGame
        }
        return if (coroutineScope == null) {
            loadRecentGames()
        } else {
            coroutineScope.launch {
                loadRecentGames()
            }
            this
        }
    }

    private fun updateBestScore() {
        if (bestScore < currentGame.score) {
            bestScore = currentGame.score
            settings.storage[keyBest] = bestScore.toString()
        }
    }

    fun idForNewGame(): Int {
        val maxGames = gameIdsRange.last
        if (recentGames.size > maxOlderGames) {
            val keepAfter = DateTimeTz.nowLocal().minus(1.weeks)
            val olderGames = recentGames.filter { it.finalPosition.startingDateTime < keepAfter }
            val id = when {
                olderGames.size > 20 -> olderGames.minByOrNull { it.finalPosition.score }?.id
                recentGames.size >= maxGames -> recentGames.minByOrNull { it.finalPosition.score }?.id
                else -> null
            }
            if (id != null) return id
        }
        return (gameIdsRange.find { id -> recentGames.none { it.id == id } }
            ?: recentGames.minByOrNull { it.finalPosition.startingDateTime }?.id
            ?: gameIdsRange.first)
    }

    fun deleteCurrent() {
        if (currentGame.id == 0) return

        settings.storage.native.remove(keyGame + currentGame.id)
        loadRecentGames()
    }

    val currentPly: Ply?
        get() = when {
            historyIndex < 0 || historyIndex >= currentGame.plies.size -> null
            else -> currentGame.plies[historyIndex]
        }

    fun add(position: GamePosition) {
        currentGame = when (position.prevPly.plyEnum) {
            PlyEnum.LOAD -> {
                GameRecord.newWithPositionAndMoves(position, emptyList(), emptyList())
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
                        currentGame.shortRecord.bookmarks.filterNot { it.plyNumber > historyIndex }
                    }
                }
                val playerMoves = when {
                    historyIndex < 0 -> {
                        currentGame.plies
                    }
                    historyIndex == 0 -> {
                        emptyList()
                    }
                    else -> {
                        currentGame.plies.take(historyIndex)
                    }
                }.let {
                    val toDrop = it.size - settings.maxMovesToStore * 2 + 1
                    if (toDrop > 0) it.drop(toDrop) else it
                } + position.prevPly
                with(currentGame.shortRecord) {
                    GameRecord(GameRecord.ShortRecord(board, note, id, start, position, bookmarksNew), playerMoves)
                }
            }
        }
        updateBestScore()
        historyIndex = -1
    }

    fun createBookmark(gamePosition: GamePosition) {
        currentGame = with(currentGame.shortRecord) {
            GameRecord(
                    GameRecord.ShortRecord(board, note, id, start, finalPosition,
                            bookmarks.filter { it.plyNumber != gamePosition.plyNumber } +
                                    gamePosition.copy()),
                    currentGame.plies
            )
        }
    }

    fun deleteBookmark(gamePosition: GamePosition) {
        currentGame = with(currentGame.shortRecord) {
            GameRecord(
                GameRecord.ShortRecord(board, note, id, start, finalPosition, bookmarks
                    .filterNot { it.plyNumber == gamePosition.plyNumber }),
                currentGame.plies
            )
        }
    }

    fun canUndo(): Boolean = settings.allowUndo &&
            historyIndex != 0 && historyIndex != 1 &&
            currentGame.plies.size > 1 &&
            currentGame.plies.lastOrNull()?.player == PlayerEnum.COMPUTER

    fun undo(): Ply? {
        if (historyIndex < 0 && currentGame.plies.isNotEmpty()) {
            historyIndex = currentGame.plies.size - 1
        } else if (historyIndex > 0 && historyIndex < currentGame.plies.size)
            historyIndex--
        else {
            return null
        }
        return currentPly
    }

    fun canRedo(): Boolean {
        return historyIndex >= 0 && historyIndex < currentGame.plies.size
    }

    fun redo(): Ply? {
        if (canRedo()) {
            return currentPly?.also {
                if (historyIndex < currentGame.plies.size - 1)
                    historyIndex++
                else {
                    historyIndex = -1
                }
            }
        }
        historyIndex = -1
        return null
    }

    fun gotoBookmark(position: GamePosition) {
        if (position.plyNumber >= currentGame.shortRecord.finalPosition.plyNumber) {
            historyIndex = -1
        } else {
            historyIndex = position.plyNumber
        }
    }
}