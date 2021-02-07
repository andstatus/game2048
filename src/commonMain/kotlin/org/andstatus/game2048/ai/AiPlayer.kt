package org.andstatus.game2048.ai

import com.soywiz.klock.Stopwatch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.GameModel
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies

/** @author yvolk@yurivolkov.com */
class AiPlayer(val settings: Settings) {

    private class FirstMove(val plyEnum: PlyEnum, val model: GameModel)

    private class MoveAndScore(val plyEnum: PlyEnum, val referenceScore: Int, val maxScore: Int) {
        constructor(gameModel: GameModel): this(gameModel.prevMove.plyEnum, gameModel.prevMove.points(), 0)
    }

    fun nextMove(model: GameModel): AiResult {
        return Stopwatch().start().let { stopWatch ->
            val mas: MoveAndScore =  when(settings.aiAlgorithm) {
                AiAlgorithm.RANDOM -> MoveAndScore(allowedRandomMove(model))
                AiAlgorithm.MAX_SCORE_OF_ONE_MOVE -> MoveAndScore(moveWithMaxScore(model))
                AiAlgorithm.MAX_EMPTY_BLOCKS_OF_N_MOVES -> maxEmptyBlocksNMoves(model, 8)
                AiAlgorithm.MAX_SCORE_OF_N_MOVES -> maxScoreNMoves(model, 10)
                AiAlgorithm.LONGEST_RANDOM_PLAY -> longestRandomPlayAdaptive(model, 50)
            }
            AiResult(mas.plyEnum, mas.referenceScore, mas.maxScore, stopWatch.elapsed.millisecondsInt)
        }
    }

    private fun moveWithMaxScore(model: GameModel): GameModel {
        return  UserPlies
            .map(model::calcUserMove)
            .filter { it.prevMove.isNotEmpty() }
            .maxByOrNull{ it.prevMove.points() }
            ?: allowedRandomMove(model)
    }

    private fun maxScoreNMoves(model: GameModel, nMoves: Int): MoveAndScore {
        return playNMoves(model, nMoves)
            .maxByOrNull(this::meanScoreForList)
            ?.let {
                MoveAndScore(it.key, meanScoreForList(it), it.value.map{ it.score}.maxOrNull() ?: 0)
            }
            ?: MoveAndScore(allowedRandomMove(model))
    }

    private fun meanScoreForList(entry: Map.Entry<PlyEnum, List<GameModel>>): Int {
        return if (entry.value.isEmpty()) 0 else entry.value.sumBy(GameModel::score) / entry.value.size
    }

    private fun maxEmptyBlocksNMoves(model: GameModel, nMoves: Int): MoveAndScore {
        return playNMoves(model, nMoves)
            .maxByOrNull {
                if (it.value.isEmpty()) 0 else it.value.sumBy { it.board.freeCount() } / it.value.size
            }
            ?.let { entry ->
                MoveAndScore(entry.key,
                    if(entry.value.isEmpty()) 0 else entry.value.sumBy { it.score } / entry.value.size,
                    entry.value.map { it.score }.maxOrNull() ?: 0
                )
            }
            ?: MoveAndScore(allowedRandomMove(model))
    }

    private fun playNMoves(model: GameModel, nMoves: Int): Map<PlyEnum, List<GameModel>> {
        var moveToModels: Map<PlyEnum, List<GameModel>> = playUserMoves(model)
            .fold(HashMap()) { aMap, mm ->
                aMap.apply {
                    put(mm.plyEnum, listOf(mm.model))
                }
            }
        (2..nMoves).forEach {
            moveToModels = moveToModels.mapValues {
                it.value.flatMap { model ->
                    playUserMoves(model)
                        .map(FirstMove::model)
                }
            }
        }
        return moveToModels
    }

    private fun longestRandomPlayAdaptive(model: GameModel, nAttemptsInitial: Int): MoveAndScore {
        val nAttempts = nAttemptsInitial * when(model.board.freeCount()) {
            in 0..5 -> 8
            in 6..8 -> 4
            in 9..11 -> 2
            else -> 1
        }

        return longestRandomPlay(model, nAttempts)
    }

    private fun longestRandomPlay(model: GameModel, nAttempts: Int): MoveAndScore {
        val firstMoves: List<FirstMove> = playUserMoves(model)

        val list: List<MoveAndScore> = firstMoves.map { firstMove ->
            val attempts: MutableList<GameModel> = ArrayList()
            repeat(nAttempts) {
                firstMove.model
                    .let(this::playRandomTillEnd)
                    .also(attempts::add)
            }
            MoveAndScore(firstMove.plyEnum,
                attempts.sumBy { it.score } / attempts.size,
                attempts.map { it.score }.maxOrNull() ?: 0
            )
        }

        return list.maxByOrNull { it.referenceScore } ?: MoveAndScore(allowedRandomMove(model))
    }

    private fun playRandomTillEnd(modelIn: GameModel): GameModel {
        var model = modelIn
        do {
            model = allowedRandomMove(model)
            if (model.prevMove.isNotEmpty()) {
                model = model.randomComputerMove()
            }
        } while (model.prevMove.isNotEmpty())
        return model
    }

    private fun playUserMoves(model: GameModel): List<FirstMove> = UserPlies
        .mapNotNull { move ->
            model.calcUserMove(move)
                .takeIf { it.prevMove.isNotEmpty() }
                ?.randomComputerMove()
                ?.let { FirstMove(move, it) }
        }

    private fun allowedRandomMove(model: GameModel): GameModel {
        UserPlies.shuffled().forEach {
            model.calcUserMove(it).also {
                if (it.prevMove.isNotEmpty()) return it
            }
        }
        return model.nextEmpty()
    }

}