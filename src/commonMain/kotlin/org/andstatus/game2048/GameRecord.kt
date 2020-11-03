package org.andstatus.game2048

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz

private const val keyId = "id"
private const val keyStart = "start"
private const val keyPlayersMoves = "playersMoves"
private const val keyFinalBoard = "finalBoard"

class GameRecord(val shortRecord: ShortRecord, val playerMoves: List<PlayerMove>) {

    fun toMap(): Map<String, Any> = shortRecord.toMap() +
            (keyPlayersMoves to playerMoves.map { it.toMap() })

    var id: Int by shortRecord::id
    val score get() = shortRecord.finalBoard.score
    override fun toString(): String = shortRecord.toString()

    companion object {
        fun newWithBoardAndMoves(board: Board, playerMoves: List<PlayerMove>) =
                GameRecord(ShortRecord(0, board.dateTime, board), playerMoves)

        fun fromJson(json: Any): GameRecord? =
                ShortRecord.fromJson(json)?.let { shortRecord ->
                    val playerMoves: List<PlayerMove> = json.asJsonMap()[keyPlayersMoves]?.asJsonArray()
                            ?.mapNotNull { PlayerMove.fromJson(it) } ?: emptyList()
                    GameRecord(shortRecord, playerMoves)
                }
    }

    class ShortRecord(var id: Int, val start: DateTimeTz, val finalBoard: Board) {

        override fun toString(): String = "${finalBoard.score} $timeString id:$id"

        val timeString get() = finalBoard.dateTime.format(SUMMARY_FORMAT)

        fun toMap(): Map<String, Any> = mapOf(
                keyId to id,
                keyStart to start.format(DateFormat.FORMAT1),
                keyFinalBoard to finalBoard.toMap()
        )

        companion object {
            val SUMMARY_FORMAT = DateFormat("yyyy-MM-dd HH:mm")

            fun fromJson(json: Any): ShortRecord? {
                val aMap: Map<String, Any> = json.asJsonMap()
                val id = aMap[keyId]?.let { it as Int } ?: 0
                val start: DateTimeTz? = aMap[keyStart]?.let { DateTime.parse(it as String) }
                val finalBoard: Board? = aMap[keyFinalBoard]?.let { Board.fromJson(it) }
                return if (start != null && finalBoard != null)
                    ShortRecord(id, start, finalBoard)
                else null
            }
        }
    }
}