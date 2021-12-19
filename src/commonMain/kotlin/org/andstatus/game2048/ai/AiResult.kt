package org.andstatus.game2048.ai

import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.PlyAndPosition
import org.andstatus.game2048.model.PlyEnum

class AiResult(
    val plyEnum: PlyEnum,
    val referencePosition: GamePosition,
    val maxPosition: GamePosition,
    val note: String?,
    val initialPosition: GamePosition,
    val takenMillis: Int
) {

    constructor(
        plyEnum: PlyEnum, referencePosition: GamePosition, maxPosition: GamePosition,
        note: String?, initialPosition: GamePosition):
            this(plyEnum, referencePosition, maxPosition, note, initialPosition, 0)

    constructor(plyAndPosition: PlyAndPosition): this(
        plyAndPosition.ply.plyEnum,
        plyAndPosition.position,
        plyAndPosition.position,
        null,
        plyAndPosition.position,
        0
    )

    companion object {
        fun empty(position: GamePosition) = AiResult(PlyEnum.EMPTY, position, position, null, position, 0)
    }

    fun isEmpty() = plyEnum == PlyEnum.EMPTY

    fun withContext(initialPosition: GamePosition, takenMillis: Int) = if (isEmpty()) {
        empty(initialPosition)
    } else {
        AiResult(plyEnum, referencePosition, maxPosition, note, initialPosition, takenMillis)
    }

    val moreScore get() = referencePosition.score - initialPosition.score
    val moreScoreMax get() = maxPosition.score - initialPosition.score
    val moreMoves get() = referencePosition.moveNumber - initialPosition.moveNumber
    val moreMovesMax get() = maxPosition.moveNumber - initialPosition.moveNumber

    override fun toString(): String {
        return "AiResult ${initialPosition.moveNumber}, ${plyEnum.id}," +
                " score +$moreScore-$moreScoreMax, " +
                " $takenMillis ms, moves +$moreMoves-$moreMovesMax, $note"
    }
}