package org.andstatus.game2048.model

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.serialization.json.toJson
import com.soywiz.korio.util.StrReader
import org.andstatus.game2048.Settings

private const val keyNote = "note"
private const val keyId = "id"
private const val keyStart = "start"
private const val keyPlayersMoves = "playersMoves"
private const val keyFinalPosition = "finalBoard"
private const val keyBookmarks = "bookmarks"

class GameRecord(val shortRecord: ShortRecord, val plies: List<Ply>) {

    fun toJsonString(): String = shortRecord.toMap().toJson()
        .let { StringBuilder(it) }
        .apply {
            plies.forEach { ply ->
                ply.toMap().toJson()
                    .let { json -> append(json) }
            }
        }.toString()

    var id: Int by shortRecord::id
    val score get() = shortRecord.finalPosition.score
    override fun toString(): String = shortRecord.toString()

    companion object {
        fun newWithPositionAndMoves(position: GamePosition, bookmarks: List<GamePosition>, plies: List<Ply>) =
            GameRecord(
                ShortRecord(position.board, "", 0, position.startingDateTime, position, bookmarks),
                plies
            )

        fun fromJson(settings: Settings, json: String, newId: Int? = null): GameRecord? {
                val reader = StrReader(json)
                val aMap: Map<String, Any> = reader.asJsonMap()
                return ShortRecord.fromJsonMap(settings, aMap, newId)?.let { shortRecord ->
                    val plies: List<Ply> =
                        if (aMap.containsKey(keyPlayersMoves))
                            aMap[keyPlayersMoves]?.asJsonArray()
                        else {
                            val list: MutableList<Any> = ArrayList()
                            while (reader.hasMore) {
                                Json.parse(reader)?.let {
                                    list.add(it)
                                }
                            }
                            list
                        }
                            ?.mapNotNull { Ply.fromJson(shortRecord.board, it) } ?: emptyList()
                    if (plies.size > shortRecord.finalPosition.plyNumber) {
                        // Fix for older versions, which didn't store move number
                        shortRecord.finalPosition.plyNumber = plies.size
                    }
                    GameRecord(shortRecord, plies)
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

            fun fromJsonMap(settings: Settings, aMap: Map<String, Any>, newId: Int?): ShortRecord? {
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