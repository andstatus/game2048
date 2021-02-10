package org.andstatus.game2048.ai

import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.PlyEnum

class AiResult(
    val plyEnum: PlyEnum,
    val referenceScore: Int,
    val maxPosition: GamePosition,
    val initialPosition: GamePosition,
    val takenMillis: Int
) {

    constructor(plyEnum: PlyEnum, referenceScore: Int, maxPosition: GamePosition): this(
        plyEnum,
        referenceScore,
        maxPosition,
        maxPosition,
        0
    )

    constructor(position: GamePosition): this(
        position.prevPly.plyEnum,
        position.prevPly.points(),
        position,
        position,
        0
    )

    companion object {
        fun empty(position: GamePosition) = AiResult(PlyEnum.EMPTY, position.score, position, position, 0)
    }

    fun isEmpty() = plyEnum == PlyEnum.EMPTY

    fun withContext(initialPosition: GamePosition, takenMillis: Int) = if (isEmpty()) {
        empty(initialPosition)
    } else {
        AiResult(plyEnum, referenceScore, maxPosition, initialPosition, takenMillis)
    }

    val maxScore = maxPosition.score
    val moreMoves = maxPosition.moveNumber - initialPosition.moveNumber
}