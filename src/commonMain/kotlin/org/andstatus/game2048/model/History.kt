package org.andstatus.game2048.model

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.weeks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.gameIsLoading
import org.andstatus.game2048.keyCurrentGame
import org.andstatus.game2048.myLog
import org.andstatus.game2048.myMeasured
import org.andstatus.game2048.myMeasuredIt

const val keyGameMode = "gameMode"

private val gameIdsRange = 1..60
private const val maxOlderGames = 30

/** @author yvolk@yurivolkov.com */
class History(val settings: Settings,
              var currentGameIn: GameRecord?,
              var recentGames: List<ShortRecord> = emptyList()) {
    private val keyBest = "best"
    var currentGame: GameRecord = currentGameIn
        ?: GameRecord.newWithPositionAndPlies(
            settings, GamePosition(settings.defaultBoard), idForNewGame(), emptyList(), emptyList())

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
                myMeasuredIt("Current game loaded") {
                    settings.currentGameId
                        ?.let { GameRecord.fromId(settings, it) }
                }
            }
            History(settings, dCurrentGame.await())
        }

        private fun loadRecentGames(settings: Settings): List<ShortRecord> =
            myMeasured("Recent games loaded") {
                gameIdsRange.fold(emptyList()) { acc, ind ->
                    ShortRecord.fromId(settings, ind)
                        ?.let { acc + it } ?: acc
                }
            }
    }

    fun loadRecentGames(): History {
        recentGames = loadRecentGames(settings)
        return this
    }

    fun openGame(id: Int): GameRecord? =
        if (currentGame.id == id) currentGame
        else GameRecord.fromId(settings, id)
            ?.also { openGame(it, id) }

    fun openGame(game: GameRecord?, id: Int): GameRecord? = game
        ?.also {
            if (it.id == id) {
                myLog("Opened game $it")
            } else {
                myLog("Fixed id $id while opening game $it")
                it.id = id
            }
            currentGame = it
            settings.storage[keyCurrentGame] = currentGame.id
            gameMode.modeEnum = GameModeEnum.STOP
        }
        ?: run {
            myLog("Failed to open game $id")
            null
        }

    fun saveCurrent(coroutineScope: CoroutineScope): History {
        settings.storage[keyGameMode] = gameMode.modeEnum.id
        // TODO: id > 0 below
        val isNew = currentGame.id <= 0

        if (isNew && currentGame.score < 1) {
            // Store only the latest game without score
            recentGames.filter { it.finalPosition.score < 1 }.forEach {
                settings.storage.native.remove(keyGame + it.id)
            }
        }

        val idToStore = if (isNew) idForNewGame().also {
            currentGame.id = it
        } else currentGame.id
        settings.storage[keyCurrentGame] = currentGame.id
        val game = currentGame

        coroutineScope.launch {
            myMeasuredIt((if (isNew) "New" else "Old") + " game saved") {
                updateBestScore()
                game.save()
                gameIsLoading.compareAndSet(true, false)
                game
            }
            loadRecentGames()
        }
        return this
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

    private val currentPly: Ply?
        get() = when {
            historyIndex < 0 || historyIndex >= currentGame.gamePlies.size -> null
            else -> currentGame.gamePlies[historyIndex]
        }

    fun add(position: GamePosition) {
        if (currentGame.notCompleted) return

        currentGame = when (position.prevPly.plyEnum) {
            PlyEnum.LOAD -> {
                GameRecord.newWithPositionAndPlies(settings, position, idForNewGame(), emptyList(), emptyList())
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
                val gamePliesNew = when {
                    historyIndex < 0 -> {
                        currentGame.gamePlies
                    }
                    historyIndex == 0 -> {
                        GamePlies(currentGame.shortRecord)
                    }
                    else -> {
                        currentGame.gamePlies.take(historyIndex)
                    }
                } + position.prevPly
                with(currentGame.shortRecord) {
                    GameRecord(ShortRecord(settings, board, note, id, start, position, bookmarksNew), gamePliesNew)
                }
            }
        }
        updateBestScore()
        historyIndex = -1
    }

    fun createBookmark(gamePosition: GamePosition) {
        if (currentGame.notCompleted) return

        currentGame = with(currentGame.shortRecord) {
            GameRecord(
                    ShortRecord(settings, board, note, id, start, finalPosition,
                            bookmarks.filter { it.plyNumber != gamePosition.plyNumber } +
                                    gamePosition.copy()),
                    currentGame.gamePlies
            )
        }
    }

    fun deleteBookmark(gamePosition: GamePosition) {
        if (currentGame.notCompleted) return

        currentGame = with(currentGame.shortRecord) {
            GameRecord(
                ShortRecord(settings, board, note, id, start, finalPosition, bookmarks
                    .filterNot { it.plyNumber == gamePosition.plyNumber }),
                currentGame.gamePlies
            )
        }
    }

    fun canUndo(): Boolean = currentGame.isReady &&
            settings.allowUndo &&
            historyIndex != 0 && historyIndex != 1 &&
            currentGame.gamePlies.size > 1 &&
            currentGame.gamePlies.lastOrNull()?.player == PlayerEnum.COMPUTER

    fun undo(): Ply? {
        if (!canUndo()) {
            return null
        } else if (historyIndex < 0 && currentGame.gamePlies.isNotEmpty()) {
            historyIndex = currentGame.gamePlies.size - 1
        } else if (historyIndex > 0 && historyIndex < currentGame.gamePlies.size)
            historyIndex--
        else {
            return null
        }
        return currentPly
    }

    fun canRedo(): Boolean {
        return currentGame.isReady &&  historyIndex >= 0 && historyIndex < currentGame.gamePlies.size
    }

    fun redo(): Ply? {
        if (canRedo()) {
            return currentPly?.also {
                if (historyIndex < currentGame.gamePlies.size - 1)
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
        if (currentGame.notCompleted) return

        if (position.plyNumber >= currentGame.shortRecord.finalPosition.plyNumber) {
            historyIndex = -1
        } else {
            historyIndex = position.plyNumber
        }
    }
}
