package org.andstatus.game2048.model

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.korio.serialization.json.toJson
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.Plies.Companion.appendPlies
import org.andstatus.game2048.myLog

const val keyGame = "game"
private const val keyNote = "note"
private const val keyId = "id"
private const val keyStart = "start"
private const val keyFinalPosition = "finalBoard"
private const val keyBookmarks = "bookmarks"

class GameRecord(val shortRecord: ShortRecord, val plies: Plies) {

    fun save(settings: Settings) {
        myLog("Starting to save $this")
        toSharedJson().let {
            settings.storage[keyGame + id] = it
        }
    }

    fun toSharedJson(): String = load()
        .shortRecord.toMap().toJson()
        .let { StringBuilder(it) }
        .appendPlies(plies)
        .toString()

    var id: Int by shortRecord::id
    val score get() = shortRecord.finalPosition.score

    fun load(): GameRecord = plies.load().let { this }
    val isReady: Boolean get() = !notCompleted
    val notCompleted: Boolean get() = plies.notCompleted

    override fun toString(): String = "$shortRecord, " +
            if (notCompleted) "loading..." else "${plies.size} plies"

    companion object {
        fun newWithPositionAndMoves(position: GamePosition, bookmarks: List<GamePosition>, plies: Plies) =
            GameRecord(
                ShortRecord(position.board, "", 0, position.startingDateTime, position, bookmarks),
                plies
            )

        fun fromId(settings: Settings, id: Int): GameRecord? {
            val shortRecord: ShortRecord? = ShortRecord.fromId(settings, id)
            return shortRecord?.let {
                val plies: Plies = Plies.fromId(settings, it)
                GameRecord(it, plies)
            }
        }

        fun fromSharedJson(settings: Settings, json: String, newId: Int? = null): GameRecord? {
            myLog("Game fromSharedJson newId:$newId, length:${json.length} ${json.substring(0..200)}...")
            return ShortRecord.fromSharedJson(settings, json, newId)?.let { shortRecord ->
                GameRecord(shortRecord, Plies.fromSharedJson(json, shortRecord))
            }
        }
    }

    class ShortRecord(val board: Board, val note: String, var id: Int, val start: DateTimeTz,
                      val finalPosition: GamePosition, val bookmarks: List<GamePosition>) {

        override fun toString(): String = "${finalPosition.score} ${finalPosition.startingDateTimeString} id:$id"

        val jsonFileName: String get() =
            "${start.format(FILENAME_FORMAT)}_${finalPosition.score}.game2048.json"

        fun toMap(): Map<String, Any> = mapOf(
                keyNote to note,
                keyStart to start.format(DateFormat.FORMAT1),
                keyFinalPosition to finalPosition.toMap(),
                keyBookmarks to bookmarks.map { it.toMap() },
                keyId to id,
                "type" to "org.andstatus.game2048:GameRecord:1",
        )

        companion object {
            val FILENAME_FORMAT = DateFormat("yyyy-MM-dd-HH-mm")

            fun fromId(settings: Settings, id: Int): ShortRecord? =
                settings.storage.getOrNull(keyGame + id)
                    ?.let { fromSharedJson(settings, it, id) }

            fun fromSharedJson(settings: Settings, json: String, newId: Int?): ShortRecord? {
                val aMap = json.asJsonMap()
                val board = settings.defaultBoard // TODO Create / load here
                val note: String = aMap[keyNote] as String? ?: ""
                val id = newId ?: aMap[keyId]?.let { it as Int } ?: 0
                val start: DateTimeTz? = aMap[keyStart]?.let { DateTime.parse(it as String) }
                val finalPosition: GamePosition? = aMap[keyFinalPosition]
                    ?.let { GamePosition.fromJson(board, it) }
                val bookmarks: List<GamePosition> = aMap[keyBookmarks]?.asJsonArray()
                    ?.mapNotNull { GamePosition.fromJson(board, it) }
                    ?: emptyList()
                return if (start != null && finalPosition != null)
                    ShortRecord(board, note, id, start, finalPosition, bookmarks)
                else null
            }

        }
    }
}
