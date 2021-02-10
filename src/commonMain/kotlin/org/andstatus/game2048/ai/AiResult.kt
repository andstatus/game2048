package org.andstatus.game2048.ai

import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.PlyEnum

class AiResult(val plyEnum: PlyEnum, val referenceScore: Int, val maxPosition: GamePosition?, val takenMillis: Int) {

    constructor(plyEnum: PlyEnum, referenceScore: Int, maxScore: GamePosition?): this(plyEnum, referenceScore, maxScore, 0)

    constructor(position: GamePosition?): this(
        position?.prevPly?.plyEnum ?: PlyEnum.EMPTY,
        position?.prevPly?.points() ?: 0,
        position,
        0)

    companion object {
        val empty = AiResult(null)
    }

    fun isEmpty() = plyEnum == PlyEnum.EMPTY

    fun withTime(takenMillis: Int) = if (isEmpty()) {
        empty
    } else {
        AiResult(plyEnum, referenceScore, maxPosition, takenMillis)
    }

    val maxScore = maxPosition?.score ?: 0
}