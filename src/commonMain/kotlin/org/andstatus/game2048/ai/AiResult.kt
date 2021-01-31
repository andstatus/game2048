package org.andstatus.game2048.ai

import org.andstatus.game2048.model.PlayerMoveEnum

class AiResult(val move: PlayerMoveEnum, val referenceScore: Int, val maxScore: Int, val takenMillis: Int)