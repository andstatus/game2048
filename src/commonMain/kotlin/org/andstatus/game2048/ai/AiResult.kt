package org.andstatus.game2048.ai

import org.andstatus.game2048.model.PlyEnum

class AiResult(val move: PlyEnum, val referenceScore: Int, val maxScore: Int, val takenMillis: Int) {
    fun isEmpty() = move == PlyEnum.EMPTY

    companion object {
        val empty = AiResult(PlyEnum.EMPTY, 0, 0, 0)
    }
}