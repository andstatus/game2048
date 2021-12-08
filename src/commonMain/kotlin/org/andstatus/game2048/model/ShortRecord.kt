package org.andstatus.game2048.model

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.korio.serialization.json.toJson
import org.andstatus.game2048.Settings

private const val keyNote = "note"
private const val keyId = "id"
private const val keyStart = "start"
private const val keyFinalPosition = "finalBoard"
private const val keyBookmarks = "bookmarks"

class ShortRecord(val settings: Settings, val board: Board, val note: String, var id: Int, val start: DateTimeTz,
                  val finalPosition: GamePosition, val bookmarks: List<GamePosition>) {

    override fun toString(): String = "${finalPosition.score} ${finalPosition.startingDateTimeString} id:$id"

    val jsonFileName: String get() =
        "${start.format(FILENAME_FORMAT)}_${finalPosition.score}.game2048.json"

    fun save() {
        toSharedJson().let {
            settings.storage[keyGame + id] = it
        }
    }

    fun toSharedJson(): String = toMap().toJson()

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

        fun sharedJsonToId(json: String, defaultId: Int): Int = json.asJsonMap()[keyId] as Int? ?: defaultId

        fun fromSharedJson(settings: Settings, json: String, newId: Int): ShortRecord? {
            val aMap = json.asJsonMap()
            val board = settings.defaultBoard // TODO Create / load here
            val note: String = aMap[keyNote] as String? ?: ""
            val start: DateTimeTz? = aMap[keyStart]?.let { DateTime.parse(it as String) }
            val finalPosition: GamePosition? = aMap[keyFinalPosition]
                ?.let { GamePosition.fromJson(board, it) }
            val bookmarks: List<GamePosition> = aMap[keyBookmarks]?.asJsonArray()
                ?.mapNotNull { GamePosition.fromJson(board, it) }
                ?: emptyList()
            return if (start != null && finalPosition != null)
                ShortRecord(settings, board, note, newId, start, finalPosition, bookmarks)
            else null
        }

    }
}