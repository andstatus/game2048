package org.andstatus.game2048.ai

import com.soywiz.klock.Stopwatch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.GameModel
import org.andstatus.game2048.model.PlayerMove
import org.andstatus.game2048.model.PlayerMoveEnum
import org.andstatus.game2048.model.PlayerMoveEnum.Companion.UserMoves
import kotlin.random.Random
import kotlin.random.nextInt

class AiPlayer(val settings: Settings) {

    private class MoveAndModel(val moveEnum: PlayerMoveEnum, val model: GameModel)

    private class MoveAndScore(val moveEnum: PlayerMoveEnum, val referenceScore: Int, val maxScore: Int) {
        constructor(playerMove: PlayerMove): this(playerMove.playerMoveEnum, playerMove.points(), 0)
    }

    fun nextMove(model: GameModel): AiResult {
        val board: Board = model.board
        return Stopwatch().start().let { stopWatch ->
            val mas: MoveAndScore =  when(settings.aiAlgorithm) {
                AiAlgorithm.RANDOM -> MoveAndScore(allowedRandomMove(board))
                AiAlgorithm.MAX_SCORE_OF_ONE_MOVE -> MoveAndScore(moveWithMaxScore(board))
                AiAlgorithm.MAX_EMPTY_BLOCKS_OF_N_MOVES -> maxEmptyBlocksNMoves(board, 8)
                AiAlgorithm.MAX_SCORE_OF_N_MOVES -> maxScoreNMoves(board, 10)
                AiAlgorithm.LONGEST_RANDOM_PLAY -> longestRandomPlayAdaptive(board, 50)
            }
            AiResult(mas.moveEnum, mas.referenceScore, mas.maxScore, stopWatch.elapsed.millisecondsInt)
        }
    }

    private fun fromBoard(board: Board): GameModel = GameModel(settings, board) { _, _ -> }

    private fun moveWithMaxScore(board: Board): PlayerMove {
        val model = fromBoard(board)
        return  UserMoves
            .map(model::calcMove)
            .filter { it.moves.isNotEmpty() }
            .maxByOrNull{ it.moves.sumBy{ it.points() }}
            ?: allowedRandomMove(board)
    }

    private fun maxScoreNMoves(board: Board, nMoves: Int): MoveAndScore {
        return playNMoves(board, nMoves)
            .maxByOrNull(this::meanScoreForList)
            ?.let {
                MoveAndScore(it.key, meanScoreForList(it), it.value.map{ it.score}.maxOrNull() ?: 0)
            }
            ?: MoveAndScore(allowedRandomMove(board))
    }

    private fun meanScoreForList(entry: Map.Entry<PlayerMoveEnum, List<GameModel>>): Int {
        return if (entry.value.isEmpty()) 0 else entry.value.sumBy(GameModel::score) / entry.value.size
    }

    private fun maxEmptyBlocksNMoves(board: Board, nMoves: Int): MoveAndScore {
        return playNMoves(board, nMoves)
            .minByOrNull {
                if (it.value.isEmpty()) 0 else it.value.sumBy { it.board.piecesCount() } / it.value.size
            }
            ?.let { entry ->
                MoveAndScore(entry.key,
                    if(entry.value.isEmpty()) 0 else entry.value.sumBy { it.score } / entry.value.size,
                    entry.value.map { it.score }.maxOrNull() ?: 0
                )
            }
            ?: MoveAndScore(allowedRandomMove(board))
    }

    private fun playNMoves(board: Board, nMoves: Int): Map<PlayerMoveEnum, List<GameModel>> {
        var moveToModels: Map<PlayerMoveEnum, List<GameModel>> = playUserMoves(fromBoard(board))
            .fold(HashMap()) { aMap, mm ->
                aMap.apply {
                    put(mm.moveEnum, listOf(mm.model))
                }
            }
        (2..nMoves).forEach {
            moveToModels = moveToModels.mapValues {
                it.value.flatMap { model ->
                    playUserMoves(model)
                        .map(MoveAndModel::model)
                }
            }
        }
        return moveToModels
    }

    private fun longestRandomPlayAdaptive(board: Board, nAttemptsInitial: Int): MoveAndScore {
        val nAttempts = nAttemptsInitial * when(board.array.size - board.piecesCount()) {
            in 0..5 -> 64
            in 6..7 -> 16
            in 8..11 -> 4
            else -> 1
        }

        return longestRandomPlay(board, nAttempts)
    }

    private fun longestRandomPlay(board: Board, nAttempts: Int): MoveAndScore {
        val firstMoves: List<MoveAndModel> = playUserMoves(fromBoard(board))

        val list: List<MoveAndScore> = firstMoves.map { firstMove ->
            val attempts: MutableList<GameModel> = ArrayList()
            repeat(nAttempts) {
                firstMove.model
                    .let(this::playRandomTillEnd)
                    .also(attempts::add)
            }
            MoveAndScore(firstMove.moveEnum,
                attempts.sumBy { it.score } / attempts.size,
                attempts.map { it.score }.maxOrNull() ?: 0
            )
        }

        return list.maxByOrNull { it.referenceScore } ?: MoveAndScore(allowedRandomMove(board))
    }

    private fun playRandomTillEnd(modelIn: GameModel): GameModel {
        var model = modelIn
        while (!model.noMoreMoves()) {
            model = allowedRandomMove(model.board)
                .let(model::play)
                .model.randomComputerMove()
                .model
        }
        return model
    }

    private fun playUserMoves(model: GameModel): List<MoveAndModel> = UserMoves
        .mapNotNull { move ->
            model.calcMove(move)
                .takeIf { it.moves.isNotEmpty() }
                ?.let(model::play)
                ?.model?.randomComputerMove()
                ?.let { MoveAndModel(move, it.model) }
        }

    private fun allowedRandomMove(board: Board): PlayerMove {
        val model = fromBoard(board)
        if (settings.allowUsersMoveWithoutBlockMoves) return randomMove().let(model::calcMove)

        while (!model.noMoreMoves()) {
            randomMove().let(model::calcMove).let {
                if (it.moves.isNotEmpty()) return it
            }
        }
        return randomMove().let(model::calcMove)
    }

    private fun randomMove() = UserMoves[Random.nextInt(0..3)]

}