package org.andstatus.game2048

import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import com.soywiz.korio.serialization.json.toJson

private const val keyStart = "start"
private const val keyPlayersMoves = "playersMoves"
private const val keyFinalBoard = "finalBoard"

class GameRecord(val start: DateTimeTz, val playersMoves:List<PlayersMove>, val finalBoard: Board) {
    fun toJson(): String  = mapOf(
            keyStart to start.format(DateFormat.FORMAT2),
            keyPlayersMoves to playersMoves.map { it.toJson() },
            keyFinalBoard to finalBoard.toJson()
    ).toJson()

    companion object {
        fun fromJson(json: Any): GameRecord? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val start: DateTimeTz? = aMap[keyStart]?.let { DateTime.parse(it as String)}
            val playersMoves: List<PlayersMove>? = aMap[keyPlayersMoves]?.asJsonArray()
                    ?.mapNotNull { PlayersMove.fromJson(it) }
            val finalBoard: Board? = aMap[keyFinalBoard]?.let { Board.fromJson(it)}
            return if (start != null && playersMoves != null && finalBoard != null)
                GameRecord(start, playersMoves, finalBoard)
            else null
        }
    }
}