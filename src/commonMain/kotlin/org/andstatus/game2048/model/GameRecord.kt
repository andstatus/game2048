package org.andstatus.game2048.model

import korlibs.time.DateTimeTz
import org.andstatus.game2048.MyContext
import org.andstatus.game2048.myLog

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
        fun newEmpty(myContext: MyContext, id: Int) = ShortRecord(
            myContext, myContext.defaultBoard, "", id,
            DateTimeTz.nowLocal(), GamePosition(myContext.defaultBoard), emptyList()
        )
            .let { GameRecord(it, GamePlies.fromPlies(it, emptyList())) }

        fun fromId(myContext: MyContext, id: Int): GameRecord? = ShortRecord.fromId(myContext, id)?.makeGameRecord()

        fun ShortRecord.makeGameRecord(): GameRecord {
            val gamePlies: GamePlies = GamePlies.fromId(this)
            return GameRecord(this, gamePlies)
        }

        fun fromSharedJson(myContext: MyContext, reader: SequenceLineReader, newId: Int): GameRecord? {
            myLog("Game fromSharedJson newId:$newId...")
            return ShortRecord.fromSharedJson(myContext, reader, newId)?.let { shortRecord ->
                GameRecord(shortRecord, GamePlies.fromSharedJson(shortRecord, reader.unRead()))
            }
        }

        fun delete(myContext: MyContext, id: Int): Boolean {
            GamePlies.delete(myContext, id)
            return ShortRecord.delete(myContext, id)
        }
    }

}
