package org.andstatus.game2048.ai

import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.GameModel
import org.andstatus.game2048.model.MovesAndModel
import org.andstatus.game2048.model.PlayerMoveEnum
import org.andstatus.game2048.model.PlayerMoveEnum.Companion.UserMoves
import kotlin.random.Random
import kotlin.random.nextInt

class AiPlayer(val settings: Settings) {

    fun nextMove(board: Board): PlayerMoveEnum =
        when(settings.aiAlgorithm) {
            AiAlgorithm.RANDOM -> allowedRandomMove(board)
            AiAlgorithm.MAX_SCORE_OF_NEXT_MOVE -> moveWithMaxScore(board)
            AiAlgorithm.MAX_SCORE_OF_N_MOVES -> maxScoreNMoves(board, 4)
            AiAlgorithm.MAX_FREE_OF_N_MOVES -> maxFreeNMoves(board, 8)
    }

    private fun fromBoard(board: Board): GameModel = GameModel(settings, board) { _, _ -> }

    private fun moveWithMaxScore(board: Board): PlayerMoveEnum {
        val model = fromBoard(board)
        return  UserMoves
            .map(model::calcMove)
            .filter { it.moves.isNotEmpty() }
            .maxByOrNull{ it.moves.sumBy{ it.points() }}
            ?.playerMoveEnum
            ?: allowedRandomMove(board)
    }

    private fun maxScoreNMoves(board: Board, nMoves: Int): PlayerMoveEnum {
        return playNMoves(board, nMoves)
            .maxByOrNull { it.model.score }
            ?.moves
            ?.firstOrNull()
            ?.playerMoveEnum
            ?: allowedRandomMove(board)
    }

    private fun maxFreeNMoves(board: Board, nMoves: Int): PlayerMoveEnum {
        return playNMoves(board, nMoves)
            .minByOrNull { it.model.board.array.filterNotNull().size }
            ?.moves
            ?.firstOrNull()
            ?.playerMoveEnum
            ?: allowedRandomMove(board)
    }

    private fun playNMoves(board: Board, nMoves: Int): List<MovesAndModel> {
        var listOfMm = playMoves(fromBoard(board))
        (2..nMoves).forEach {
            listOfMm = listOfMm.flatMap { mm ->
                playMoves(mm.model)
                    .map { mm2 -> MovesAndModel(mm.moves, mm2.model) }
            }
        }
        return listOfMm
    }

    private fun playMoves(model: GameModel) = UserMoves
        .map(model::calcMove)
        .filter { it.moves.isNotEmpty() }
        .map(model::play)

    private fun allowedRandomMove(board: Board): PlayerMoveEnum {
        if (settings.allowUsersMoveWithoutBlockMoves) return randomMove()

        val model = fromBoard(board)
        while (!model.noMoreMoves()) {
            randomMove().let {
                if (model.calcMove(it).moves.isNotEmpty()) return it
            }
        }
        return randomMove()
    }

    private fun randomMove() = UserMoves[Random.nextInt(0..3)]

}