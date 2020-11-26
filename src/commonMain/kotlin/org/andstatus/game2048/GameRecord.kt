package org.andstatus.game2048

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz

private const val keyNote = "note"
private const val keyId = "id"
private const val keyStart = "start"
private const val keyPlayersMoves = "playersMoves"
private const val keyFinalBoard = "finalBoard"
private const val keyBookmarks = "bookmarks"

class GameRecord(val shortRecord: ShortRecord, val playerMoves: List<PlayerMove>) {

    fun toMap(): Map<String, Any> = shortRecord.toMap() +
            (keyPlayersMoves to playerMoves.map { it.toMap() })

    var id: Int by shortRecord::id
    val score get() = shortRecord.finalBoard.score
    override fun toString(): String = shortRecord.toString()

    companion object {
        fun newWithBoardAndMoves(board: Board, bookmarks: List<Board>, playerMoves: List<PlayerMove>) =
                GameRecord(ShortRecord("", 0, board.dateTime, board, bookmarks), playerMoves)

        fun fromJson(json: Any, newId: Int? = null): GameRecord? =
                ShortRecord.fromJson(json, newId)?.let { shortRecord ->
                    val playerMoves: List<PlayerMove> = json.asJsonMap()[keyPlayersMoves]?.asJsonArray()
                            ?.mapNotNull { PlayerMove.fromJson(it) } ?: emptyList()
                    GameRecord(shortRecord, playerMoves)
                }
    }

    class ShortRecord(val note: String, var id: Int, val start: DateTimeTz, val finalBoard: Board,
        val bookmarks: List<Board>) {

        override fun toString(): String = "${finalBoard.score} ${finalBoard.timeString} id:$id"

        val jsonFileName: String get() =
            "${start.format(FILENAME_FORMAT)}_${finalBoard.score}.game2048.json"

        fun toMap(): Map<String, Any> = mapOf(
                keyNote to note,
                keyStart to start.format(DateFormat.FORMAT1),
                keyFinalBoard to finalBoard.toMap(),
                keyBookmarks to bookmarks.map { it.toMap() },
                keyId to id,
                "type" to "org.andstatus.game2048:GameRecord:1",
        )

        companion object {
            val FILENAME_FORMAT = DateFormat("yyyy-MM-dd-HH-mm")

            fun fromJson(json: Any, newId: Int? = null): ShortRecord? {
                val aMap: Map<String, Any> = json.asJsonMap()
                val note: String = aMap[keyNote] as String? ?: ""
                val id = newId ?: aMap[keyId]?.let { it as Int } ?: 0
                val start: DateTimeTz? = aMap[keyStart]?.let { DateTime.parse(it as String) }
                val finalBoard: Board? = aMap[keyFinalBoard]?.let { Board.fromJson(it) }
                val bookmarks: List<Board> = json.asJsonMap()[keyBookmarks]?.asJsonArray()
                        ?.mapNotNull { Board.fromJson(it) } ?: emptyList()
                return if (start != null && finalBoard != null)
                    ShortRecord(note, id, start, finalBoard, bookmarks)
                else null
            }
        }
    }
}