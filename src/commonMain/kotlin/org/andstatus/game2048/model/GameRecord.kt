package org.andstatus.game2048.model

import org.andstatus.game2048.Settings
import org.andstatus.game2048.myLog

const val keyGame = "game"

class GameRecord(val shortRecord: ShortRecord, val gamePlies: GamePlies) {

    fun save() {
        myLog("Starting to save $this")
        shortRecord.save()
        gamePlies.save()
        myLog("Saved $this")
    }

    fun toSharedJson(): String = load().shortRecord.toSharedJson() + gamePlies.toSharedJson()

    var id: Int by shortRecord::id
    val score get() = shortRecord.finalPosition.score

    fun load(): GameRecord = gamePlies.load().let { this }
    val isEmpty: Boolean = shortRecord.finalPosition.placedPieces().isEmpty()
    val isReady: Boolean get() = !notCompleted
    val notCompleted: Boolean get() = gamePlies.notCompleted

    fun toLongString(): String = toString() + gamePlies.toLongString()

    override fun toString(): String = "$shortRecord, " + gamePlies.toShortString()

    companion object {
        fun newWithPositionAndPlies(
            settings: Settings, position: GamePosition, id: Int, bookmarks: List<GamePosition>,
            plies: List<Ply>
        ) = ShortRecord(settings, position.board, "", id, position.startingDateTime, position, bookmarks)
            .let { GameRecord(it, GamePlies.fromPlies(it, plies)) }

        fun fromId(settings: Settings, id: Int): GameRecord? {
            val shortRecord: ShortRecord? = ShortRecord.fromId(settings, id)
            return shortRecord?.let {
                val gamePlies: GamePlies = GamePlies.fromId(settings, it)
                GameRecord(it, gamePlies)
            }
        }

        fun fromSharedJson(settings: Settings, json: String, newId: Int): GameRecord? {
            myLog("Game fromSharedJson newId:$newId, length:${json.length} ${json.substring(0..200)}...")
            return ShortRecord.fromSharedJson(settings, json, newId)?.let { shortRecord ->
                GameRecord(shortRecord, GamePlies.fromSharedJson(shortRecord, json))
            }
        }

        fun delete(settings: Settings, id: Int): Boolean {
            GamePlies.delete(settings, id)
            return ShortRecord.delete(settings, id)
        }
    }

}
