package org.andstatus.game2048

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.korio.serialization.json.toJson

private const val keyId = "id"
private const val keyStart = "start"
private const val keyPlayersMoves = "playersMoves"
private const val keyFinalBoard = "finalBoard"

class GameRecord(val start: DateTimeTz, val playerMoves:List<PlayerMove>, val finalBoard: Board, var id: Int = 0) {
    fun toJson(): String  = mapOf(
            keyId to id,
            keyStart to start.format(DateFormat.FORMAT1),
            keyPlayersMoves to playerMoves.map { it.toJson() },
            keyFinalBoard to finalBoard.toJson()
    ).toJson()

    companion object {
        fun fromJson(json: Any): GameRecord? {
            val summary = GameSummary.fromJson(json)
            return if (summary != null) {
                val aMap: Map<String, Any> = json.asJsonMap()
                val playerMoves: List<PlayerMove> = aMap[keyPlayersMoves]?.asJsonArray()
                        ?.mapNotNull { PlayerMove.fromJson(it) } ?: emptyList()
                GameRecord(summary.start, playerMoves, summary.finalBoard, summary.id)
            } else null
        }
    }
}

data class GameSummary(val id: Int, val start: DateTimeTz, val finalBoard: Board) {
    val summary get() = "${finalBoard.score} ${start.format(SUMMARY_FORMAT)}"

    companion object {
        val SUMMARY_FORMAT = DateFormat("yyyy-MM-dd HH:mm")

        fun fromJson(json: Any): GameSummary? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val id = aMap[keyId]?.let { it as Int } ?: 0
            val start: DateTimeTz? = aMap[keyStart]?.let { DateTime.parse(it as String)}
            val finalBoard: Board? = aMap[keyFinalBoard]?.let { Board.fromJson(it)}
            return if (start != null && finalBoard != null)
                GameSummary(id, start, finalBoard)
            else null
        }
    }
}