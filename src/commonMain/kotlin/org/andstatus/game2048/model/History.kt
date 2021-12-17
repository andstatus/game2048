package org.andstatus.game2048.model

import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.weeks
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.concurrent.atomic.korAtomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.gameIsLoading
import org.andstatus.game2048.keyCurrentGameId
import org.andstatus.game2048.model.GameRecord.Companion.makeGameRecord
import org.andstatus.game2048.myLog
import org.andstatus.game2048.myMeasuredIt

const val keyGameMode = "gameMode"

private val gameIdsRange = 1..60
private const val maxOlderGames = 30

/** @author yvolk@yurivolkov.com */
class History(
    val settings: Settings,
    currentGameIn: GameRecord?,
    recentGamesIn: List<ShortRecord> = emptyList()
) {
    private val keyBest = "best"
    private val recentGamesRef: KorAtomicRef<List<ShortRecord>> = korAtomic(recentGamesIn)
    val recentGames get() = recentGamesRef.value
    val currentGameRef: KorAtomicRef<GameRecord?> = korAtomic(currentGameIn)
    val currentGame: GameRecord
        get() = currentGameRef.value
            ?: latestOtherGame(0)?.also { currentGameRef.compareAndSet(null, it) }
            ?: newEmptyGame.also { currentGameRef.compareAndSet(null, it) }
    private val newEmptyGame get() = GameRecord.newEmpty(settings, idForNewGame()).load()

    // 1. Info on previous games
    var bestScore: Int = settings.storage.getOrNull(keyBest)?.toInt() ?: 0

    // 2. This game, see for the inspiration https://en.wikipedia.org/wiki/Portable_Game_Notation
    /** 0 means that the pointer is turned off */
    var redoPlyPointer: Int = 0
    val gameMode: GameMode = GameMode().apply {
        modeEnum = GameModeEnum.fromId(settings.storage.getOrNull(keyGameMode) ?: "").let {
            when (it) {
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
    }

    private fun ensureRecentGames(): History = if (recentGames.isEmpty()) loadRecentGames() else this

    fun loadRecentGames(): History {
        myMeasuredIt("Recent games loaded") {
            recentGamesRef.value = gameIdsRange.fold(emptyList()) { acc, ind ->
                ShortRecord.fromId(settings, ind)
                    ?.let { acc + it } ?: acc
            }
            "${recentGames.size} records"
        }
        return this
    }

    fun openNewGame(): GameRecord = newEmptyGame.let {
        openGame(it, it.id) ?: throw IllegalStateException("Failed to open new empty game")
    }

    fun openGame(id: Int): GameRecord? =
        currentGameRef.value?.let {
            if (it.id == id) return it
            else null
        }
            ?: GameRecord.fromId(settings, id)
                ?.also { openGame(it, id) }

    fun openGame(game: GameRecord?, id: Int): GameRecord? = game
        ?.also {
            if (it.id == id) {
                myLog("Opened game $it")
            } else {
                myLog("Fixed id $id while opening game $it")
                it.id = id
            }
            currentGameRef.value = it
            settings.storage[keyCurrentGameId] = it.id
            gameMode.modeEnum = if (it.isEmpty) GameModeEnum.PLAY else GameModeEnum.STOP
        }
        ?: run {
            myLog("Failed to open game $id")
            null
        }

    fun saveCurrent(coroutineScope: CoroutineScope): History {
        settings.storage[keyGameMode] = gameMode.modeEnum.id
        currentGameRef.value?.let { game ->
            settings.storage[keyCurrentGameId] = game.id

            coroutineScope.launch {
                myMeasuredIt("Game saved") {
                    updateBestScore()
                    game.save()
                    gameIsLoading.compareAndSet(true, false)
                    game
                }
                loadRecentGames()
            }
        }
        return this
    }

    private fun updateBestScore() {
        (currentGameRef.value?.score ?: 0).let { score ->
            if (bestScore < score) {
                bestScore = score
                settings.storage[keyBest] = score.toString()
            }
        }
    }

    fun idForNewGame(): Int = ensureRecentGames()
        .let { idToDelete() ?: unusedGameId() }
        .also {
            deleteGame(it)
            myLog("idForNewGame: $it")
        }

    private fun idToDelete() = if (recentGames.size > maxOlderGames) {
        val keepAfter = DateTimeTz.nowLocal().minus(1.weeks)
        val olderGames = recentGames.filterNot {
            it.finalPosition.startingDateTime >= keepAfter || it.id == currentGameRef.value?.id
        }
        when {
            olderGames.size > 20 -> olderGames.minByOrNull { it.finalPosition.score }?.id
            recentGames.size >= gameIdsRange.last -> recentGames.minByOrNull { it.finalPosition.score }?.id
            else -> null
        }
    } else null

    private fun unusedGameId() = gameIdsRange.filterNot { it == currentGameRef.value?.id }
        .find { id -> recentGames.none { it.id == id } }
        ?: recentGames.filterNot { it.id == currentGameRef.value?.id }
            .minByOrNull { it.finalPosition.startingDateTime }?.id
        ?: throw IllegalStateException("Failed to find unusedGameId")

    fun deleteCurrent() = currentGameRef.value?.let { deleteGame(it.id) }

    fun deleteGame(id: Int) {
        GameRecord.delete(settings, id)
        recentGamesRef.value = recentGames.filterNot { it.id == id }
        if (currentGameRef.value?.id == id) {
            currentGameRef.value = latestOtherGame(id)
        }
    }

    fun latestOtherGame(notTheId: Int): GameRecord? = recentGames
        .filterNot { it.id == notTheId }
        .maxByOrNull { it.finalPosition.startingDateTime }
        ?.makeGameRecord()

    val plyToRedo: Ply?
        get() = currentGameRef.value?.let { game ->
            when {
                redoPlyPointer < 1 || redoPlyPointer > game.gamePlies.size -> null
                else -> game.gamePlies[redoPlyPointer]
            }
        }

    fun add(position: GamePosition) = currentGameRef.value?.let { game ->
        currentGameRef.value = when (position.prevPly.plyEnum) {
            PlyEnum.LOAD -> game.replayedAtPosition(position)
            else -> {
                val bookmarksNew = when {
                    redoPlyPointer < 1 -> {
                        game.shortRecord.bookmarks
                    }
                    redoPlyPointer == 1 -> {
                        emptyList()
                    }
                    else -> {
                        game.shortRecord.bookmarks.filterNot { it.plyNumber >= redoPlyPointer }
                    }
                }
                val gamePliesNew = when {
                    redoPlyPointer < 1 -> {
                        game.gamePlies
                    }
                    redoPlyPointer == 1 -> {
                        GamePlies(game.shortRecord)
                    }
                    else -> {
                        game.gamePlies.take(redoPlyPointer - 1)
                    }
                } + position.prevPly
                with(game.shortRecord) {
                    GameRecord(ShortRecord(settings, board, note, id, start, position, bookmarksNew), gamePliesNew)
                }
            }
        }
        updateBestScore()
        redoPlyPointer = 0
    }

    fun createBookmark(gamePosition: GamePosition) = currentGameRef.value?.let { game ->
        currentGameRef.value = with(game.shortRecord) {
            GameRecord(
                ShortRecord(settings, board, note, id, start, finalPosition,
                    bookmarks.filter { it.plyNumber != gamePosition.plyNumber } +
                            gamePosition.copy()),
                game.gamePlies
            )
        }
    }

    fun deleteBookmark(gamePosition: GamePosition) = currentGameRef.value?.let { game ->
        currentGameRef.value = with(game.shortRecord) {
            GameRecord(
                ShortRecord(settings, board, note, id, start, finalPosition, bookmarks
                    .filterNot { it.plyNumber == gamePosition.plyNumber }),
                game.gamePlies
            )
        }
    }

    fun canUndo(): Boolean = currentGameRef.value?.let { game ->
        settings.allowUndo &&
            redoPlyPointer != 1 && redoPlyPointer != 2 &&
            game.gamePlies.size > 1 &&
            game.gamePlies.lastOrNull()?.player == PlayerEnum.COMPUTER
    } ?: false

    fun undo(): Ply? = currentGameRef.value?.let { game ->
        if (!canUndo()) {
            return null
        } else if (redoPlyPointer < 1 && game.gamePlies.size > 0) {
            // Point to the last ply
            redoPlyPointer = game.gamePlies.size
        } else if (redoPlyPointer > 1 && redoPlyPointer <= game.gamePlies.size + 1)
            redoPlyPointer--
        else {
            return null
        }
        return plyToRedo
    }

    fun canRedo(): Boolean = currentGameRef.value?.let { game ->
        return redoPlyPointer > 0 && redoPlyPointer <= game.gamePlies.size
    } ?: false

    fun redo(): Ply? = currentGameRef.value?.let { game ->
        if (canRedo()) {
            return plyToRedo?.also {
                if (redoPlyPointer < game.gamePlies.size)
                    redoPlyPointer++
                else {
                    redoPlyPointer = 0
                }
            }
        }
        redoPlyPointer = 0
        return null
    }

    fun gotoBookmark(position: GamePosition) = currentGameRef.value?.let { game ->
        if (position.plyNumber >= game.shortRecord.finalPosition.plyNumber) {
            redoPlyPointer = 0
        } else {
            redoPlyPointer = position.plyNumber + 1
        }
    }
}
