package org.andstatus.game2048.model

import korlibs.io.serialization.json.toJson
import korlibs.time.DateFormat
import korlibs.time.DateTime
import korlibs.time.DateTimeTz
import org.andstatus.game2048.MyContext
import org.andstatus.game2048.stubGameId

private const val keyGame = "game"
private const val keyNote = "note"
private const val keyId = "id"
private const val keyStart = "start"
private const val keyFinalPosition = "finalBoard"
private const val keyBookmarks = "bookmarks"

class ShortRecord(
    val myContext: MyContext, val board: Board, val note: String, var id: Int, val start: DateTimeTz,
    val finalPosition: GamePosition, val bookmarks: List<GamePosition>
) {

    val isStub = id == stubGameId

    override fun toString(): String =
        if (isStub) "Stub"
        else "id:$id score:${finalPosition.score} ${finalPosition.startingDateTimeString}"

    val jsonFileName: String
        get() =
            "${start.format(FILENAME_FORMAT)}_${finalPosition.score}.game2048.json"

    fun save() {
        if (isStub) return

        toSharedJson().let {
            myContext.storage[keyGameRecord] = it
        }
    }

    fun toSharedJson(): String = toMap().toJson()

    fun toMap(): Map<String, Any> = mapOf(
        keyNote to note,
        keyStart to start.format(DateFormat.FORMAT1),
        keyFinalPosition to finalPosition.toMap(),
        keyBookmarks to bookmarks.map { it.toMap() },
        keyId to id,
        "type" to "org.andstatus.game2048:GameRecord:2",
    )

    val keyGameRecord get() = keyGameRecord(id)

    fun replayedAtPosition(position: GamePosition): ShortRecord =
        ShortRecord(
            myContext, board, note, id, start, position,
            bookmarks.filterNot { it.plyNumber >= position.plyNumber })

    companion object {
        val FILENAME_FORMAT = DateFormat("yyyy-MM-dd-HH-mm")

        fun fromId(myContext: MyContext, id: Int): ShortRecord? =
            myContext.storage.getOrNull(keyGameRecord(id))
                ?.let { fromSharedJson(myContext, SequenceLineReader(sequenceOf(it)), id) }

        private fun keyGameRecord(id: Int) = keyGame + id

        fun sharedJsonToId(json: String, defaultId: Int): Int = json.parseJsonMap()[keyId] as Int? ?: defaultId

        fun fromSharedJson(myContext: MyContext, json: SequenceLineReader, newId: Int): ShortRecord? {
            val aMap = json.parseJsonMap()
            val note: String = aMap[keyNote] as String? ?: ""
            val start: DateTimeTz = aMap[keyStart]?.let { DateTime.parse(it as String) } ?: return null

            val finalPosition: GamePosition = aMap[keyFinalPosition]
                ?.let { GamePosition.fromJson(json = it, myContext = myContext) }
                ?: return null

            val board = Board(myContext, finalPosition.board.width)
            val bookmarks: List<GamePosition> = aMap[keyBookmarks]?.parseJsonArray()
                ?.mapNotNull { GamePosition.fromJson(json = it, myContext = myContext) }
                ?: emptyList()
            return ShortRecord(myContext, board, note, newId, start, finalPosition, bookmarks)
        }

        fun delete(myContext: MyContext, id: Int): Boolean =
            myContext.storage.remove(keyGameRecord(id))

    }
}
