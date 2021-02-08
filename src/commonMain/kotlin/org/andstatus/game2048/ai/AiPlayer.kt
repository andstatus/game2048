package org.andstatus.game2048.ai

import com.soywiz.klock.Stopwatch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies

/** @author yvolk@yurivolkov.com */
class AiPlayer(val settings: Settings) {

    private class FirstMove(val plyEnum: PlyEnum, val position: GamePosition)

    private class MoveAndScore(val plyEnum: PlyEnum, val referenceScore: Int, val maxScore: Int) {
        constructor(position: GamePosition): this(position.prevPly.plyEnum, position.prevPly.points(), 0)

        fun toAiResult(takenMillis: Int) = if (plyEnum.isEmpty()) {
            AiResult.empty
        } else {
            AiResult(plyEnum, referenceScore, maxScore, takenMillis)
        }
    }

    fun nextPly(position: GamePosition): AiResult = Stopwatch().start().let { stopWatch ->
        when (settings.aiAlgorithm) {
            AiAlgorithm.RANDOM -> MoveAndScore(allowedRandomMove(position))
            AiAlgorithm.MAX_SCORE_OF_ONE_MOVE -> MoveAndScore(moveWithMaxScore(position))
            AiAlgorithm.MAX_EMPTY_BLOCKS_OF_N_MOVES -> maxEmptyBlocksNMoves(position, 8)
            AiAlgorithm.MAX_SCORE_OF_N_MOVES -> maxScoreNMoves(position, 10)
            AiAlgorithm.LONGEST_RANDOM_PLAY -> longestRandomPlayAdaptive(position, 50)
        }.toAiResult(stopWatch.elapsed.millisecondsInt)
    }

    private fun moveWithMaxScore(position: GamePosition): GamePosition = UserPlies
        .map(position::calcUserPly)
        .filter { it.prevPly.isNotEmpty() }
        .maxByOrNull{ it.prevPly.points() }
        ?: position.nextNoPly()

    private fun maxScoreNMoves(position: GamePosition, nMoves: Int): MoveAndScore =
        playNMoves(position, nMoves)
        .maxByOrNull(this::meanScoreForList)
        ?.let {
            MoveAndScore(it.key, meanScoreForList(it), it.value.map{ it.score}.maxOrNull() ?: 0)
        }
        ?: MoveAndScore(position.nextNoPly())

    private fun meanScoreForList(entry: Map.Entry<PlyEnum, List<GamePosition>>): Int =
        if (entry.value.isEmpty()) 0 else entry.value.sumBy(GamePosition::score) / entry.value.size

    private fun maxEmptyBlocksNMoves(position: GamePosition, nMoves: Int): MoveAndScore =
        playNMoves(position, nMoves)
        .maxByOrNull {
            if (it.value.isEmpty()) 0 else it.value.sumBy { it.freeCount() } / it.value.size
        }
        ?.let { entry ->
            MoveAndScore(entry.key,
                if(entry.value.isEmpty()) 0 else entry.value.sumBy { it.score } / entry.value.size,
                entry.value.map { it.score }.maxOrNull() ?: 0
            )
        }
        ?: MoveAndScore(position.nextNoPly())

    private fun playNMoves(position: GamePosition, nMoves: Int): Map<PlyEnum, List<GamePosition>> {
        var plyToPositions: Map<PlyEnum, List<GamePosition>> = playUserMoves(position)
            .fold(HashMap()) { aMap, mm ->
                aMap.apply {
                    put(mm.plyEnum, listOf(mm.position))
                }
            }
        (2..nMoves).forEach {
            plyToPositions = plyToPositions.mapValues {
                it.value.flatMap { pos ->
                    playUserMoves(pos).map(FirstMove::position)
                }
            }
        }
        return plyToPositions
    }

    private fun longestRandomPlayAdaptive(position: GamePosition, nAttemptsInitial: Int): MoveAndScore {
        val nAttempts = nAttemptsInitial * when(position.freeCount()) {
            in 0..5 -> 8
            in 6..8 -> 4
            in 9..11 -> 2
            else -> 1
        }

        return longestRandomPlay(position, nAttempts)
    }

    private fun longestRandomPlay(position: GamePosition, nAttempts: Int): MoveAndScore {
        val firstMoves: List<FirstMove> = playUserMoves(position)

        val list: List<MoveAndScore> = firstMoves.map { firstMove ->
            val attempts: MutableList<GamePosition> = ArrayList()
            repeat(nAttempts) {
                firstMove.position
                    .let(this::playRandomTillEnd)
                    .also(attempts::add)
            }
            MoveAndScore(firstMove.plyEnum,
                attempts.sumBy { it.score } / attempts.size,
                attempts.map { it.score }.maxOrNull() ?: 0
            )
        }

        return list.maxByOrNull { it.referenceScore } ?: MoveAndScore(position.nextNoPly())
    }

    private fun playRandomTillEnd(positionIn: GamePosition): GamePosition {
        var position = positionIn
        do {
            position = allowedRandomMove(position)
            if (position.prevPly.isNotEmpty()) {
                position = position.randomComputerPly()
            }
        } while (position.prevPly.isNotEmpty())
        return position
    }

    private fun playUserMoves(position: GamePosition): List<FirstMove> = UserPlies
        .mapNotNull { move ->
            position.calcUserPly(move)
                .takeIf { it.prevPly.isNotEmpty() }
                ?.randomComputerPly()
                ?.let { FirstMove(move, it) }
        }

    private fun allowedRandomMove(position: GamePosition): GamePosition {
        UserPlies.shuffled().forEach {
            position.calcUserPly(it).also {
                if (it.prevPly.isNotEmpty()) return it
            }
        }
        return position.nextNoPly()
    }

}