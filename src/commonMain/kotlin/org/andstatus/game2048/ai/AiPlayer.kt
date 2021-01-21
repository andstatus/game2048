package org.andstatus.game2048.ai

import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.GameModel
import org.andstatus.game2048.model.PlayerMoveEnum
import kotlin.random.Random
import kotlin.random.nextInt

class AiPlayer(val settings: Settings) {
    val model = GameModel(settings) { _, _ -> }

    fun nextMove(board: Board): PlayerMoveEnum {
        return allowedRandomMove(board)
    }

    private fun allowedRandomMove(board: Board): PlayerMoveEnum {
        if (settings.allowUsersMoveWithoutBlockMoves) return randomMove()

        model.composerMove(board)
        while (!model.noMoreMoves()) {
            randomMove().let {
                if (model.calcMove(it).moves.isNotEmpty()) return it
            }
        }
        return randomMove()
    }

    private fun randomMove() = PlayerMoveEnum.values()[Random.nextInt(0..3)]

}