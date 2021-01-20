package org.andstatus.game2048.ai

import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.PlayerMoveEnum
import kotlin.random.Random
import kotlin.random.nextInt

class AiPlayer(val settings: Settings) {

    suspend fun nextMove(board: Board): PlayerMoveEnum {
        return PlayerMoveEnum.values()[Random.nextInt(0 .. 3)]
    }

}