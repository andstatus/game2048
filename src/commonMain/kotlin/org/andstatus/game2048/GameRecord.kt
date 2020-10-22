package org.andstatus.game2048

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.korio.serialization.json.toJson

private const val keyStart = "start"
private const val keyPlayersMoves = "playersMoves"
private const val keyFinalBoard = "finalBoard"

class GameRecord(val start: DateTimeTz, val playerMoves:List<PlayerMove>, val finalBoard: Board) {
    fun toJson(): String  = mapOf(
            keyStart to start.format(DateFormat.FORMAT1),
            keyPlayersMoves to playerMoves.map { it.toJson() },
            keyFinalBoard to finalBoard.toJson()
    ).toJson()

    companion object {
        fun fromJson(json: Any): GameRecord? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val start: DateTimeTz? = aMap[keyStart]?.let { DateTime.parse(it as String)}
            val playerMoves: List<PlayerMove>? = aMap[keyPlayersMoves]?.asJsonArray()
                    ?.mapNotNull { PlayerMove.fromJson(it) }
            val finalBoard: Board? = aMap[keyFinalBoard]?.let { Board.fromJson(it)}
            return if (start != null && playerMoves != null && finalBoard != null)
                GameRecord(start, playerMoves, finalBoard)
            else null
        }
    }
}