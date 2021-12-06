package org.andstatus.game2048.model

import com.soywiz.korio.serialization.json.toJson
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.GamePlies.Companion.appendPlies
import org.andstatus.game2048.myLog

const val keyGame = "game"

class GameRecord(val shortRecord: ShortRecord, val gamePlies: GamePlies) {

    fun save(settings: Settings) {
        myLog("Starting to save $this")
        toSharedJson().let {
            settings.storage[keyGame + id] = it
        }
    }

    fun toSharedJson(): String = load()
        .shortRecord.toMap().toJson()
        .let { StringBuilder(it) }
        .appendPlies(gamePlies)
        .toString()

    var id: Int by shortRecord::id
    val score get() = shortRecord.finalPosition.score

    fun load(): GameRecord = gamePlies.load().let { this }
    val isReady: Boolean get() = !notCompleted
    val notCompleted: Boolean get() = gamePlies.notCompleted

    override fun toString(): String = "$shortRecord, " +
            if (notCompleted) "loading..." else "${gamePlies.size} plies"

    companion object {
        fun newWithPositionAndPlies(position: GamePosition, bookmarks: List<GamePosition>, plies: List<Ply>) =
            ShortRecord(position.board, "", 0, position.startingDateTime, position, bookmarks).let {
                GameRecord(it, GamePlies(it, plies))
            }

        fun fromId(settings: Settings, id: Int): GameRecord? {
            val shortRecord: ShortRecord? = ShortRecord.fromId(settings, id)
            return shortRecord?.let {
                val gamePlies: GamePlies = GamePlies.fromId(settings, it)
                GameRecord(it, gamePlies)
            }
        }

        fun fromSharedJson(settings: Settings, json: String, newId: Int? = null): GameRecord? {
            myLog("Game fromSharedJson newId:$newId, length:${json.length} ${json.substring(0..200)}...")
            return ShortRecord.fromSharedJson(settings, json, newId)?.let { shortRecord ->
                GameRecord(shortRecord, GamePlies.fromSharedJson(json, shortRecord))
            }
        }
    }

}
