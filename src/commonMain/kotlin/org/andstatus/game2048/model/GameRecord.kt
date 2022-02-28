package org.andstatus.game2048.model

import com.soywiz.klock.DateTimeTz
import org.andstatus.game2048.Settings
import org.andstatus.game2048.myLog

const val keyGame = "game"

class GameRecord(val shortRecord: ShortRecord, val gamePlies: GamePlies) {

    fun save() {
        if (shortRecord.isStub) return

        myLog("Starting to save $this")
        shortRecord.save()
        gamePlies.save()
        myLog("Saved $this")
    }

    fun toSharedJsonSequence(): Sequence<String> = sequence {
       yield(load().shortRecord.toSharedJson())
       yieldAll(gamePlies.toSharedJsonSequence())
    }

    var id: Int by shortRecord::id
    val score get() = shortRecord.finalPosition.score

    fun load(): GameRecord = gamePlies.load().let { this }
    val isEmpty: Boolean = shortRecord.finalPosition.placedPieces().isEmpty()
    val isReady: Boolean get() = gamePlies.isReady

    fun toLongString(): String = "$shortRecord, " + gamePlies.toLongString()

    override fun toString(): String = "$shortRecord, " + gamePlies.toShortString()

    fun replayedAtPosition(position: GamePosition): GameRecord =
        if (position.plyNumber >= shortRecord.finalPosition.plyNumber)
            this
        else
            shortRecord.replayedAtPosition(position).let {
                GameRecord(it, gamePlies.take(position.plyNumber - 1))
            }

    companion object {
        fun newEmpty(settings: Settings, id: Int) = ShortRecord(settings, settings.defaultBoard, "", id,
            DateTimeTz.nowLocal(), GamePosition(settings.defaultBoard), emptyList())
            .let { GameRecord(it, GamePlies.fromPlies(it, emptyList())) }

        fun fromId(settings: Settings, id: Int): GameRecord? = ShortRecord.fromId(settings, id)?.makeGameRecord()

        fun ShortRecord.makeGameRecord(): GameRecord {
            val gamePlies: GamePlies = GamePlies.fromId(this)
            return GameRecord(this, gamePlies)
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
