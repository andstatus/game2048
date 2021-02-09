package org.andstatus.game2048.ai

import com.soywiz.klock.Stopwatch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies

/** @author yvolk@yurivolkov.com */
class AiPlayer(val settings: Settings) {

    private class FirstMove(val plyEnum: PlyEnum, val position: GamePosition)

    fun nextPly(position: GamePosition): AiResult = Stopwatch().start().let { stopWatch ->
        when (settings.aiAlgorithm) {
            AiAlgorithm.RANDOM -> AiResult(allowedRandomMove(position))
            AiAlgorithm.MAX_SCORE_OF_ONE_MOVE -> moveWithMaxScore(position)
            AiAlgorithm.MAX_EMPTY_BLOCKS_OF_N_MOVES -> maxEmptyBlocksNMoves(position, 8)
            AiAlgorithm.MAX_SCORE_OF_N_MOVES -> maxScoreNMoves(position, 10)
            AiAlgorithm.LONGEST_RANDOM_PLAY -> longestRandomPlayAdaptive(position, 50)
        }.withTime(stopWatch.elapsed.millisecondsInt)
    }

    private fun moveWithMaxScore(position: GamePosition): AiResult = UserPlies
        .map(position::calcUserPly)
        .filter { it.prevPly.isNotEmpty() }
        .maxByOrNull{ it.prevPly.points() }
        .let(::AiResult)

    private fun maxEmptyBlocksNMoves(position: GamePosition, nMoves: Int): AiResult =
        playNMoves(position, nMoves)
            .maxByOrNull {
                if (it.value.isEmpty()) 0 else it.value.sumBy { it.freeCount() } / it.value.size
            }
            ?.let { entry ->
                AiResult(entry.key,
                    if(entry.value.isEmpty()) 0 else entry.value.sumBy { it.score } / entry.value.size,
                    entry.value.map { it.score }.maxOrNull() ?: 0
                )
            }
            ?: AiResult.empty

    private fun maxScoreNMoves(position: GamePosition, nMoves: Int): AiResult =
        playNMoves(position, nMoves)
        .maxByOrNull(this::meanScoreForList)
        ?.let {
            AiResult(it.key, meanScoreForList(it), it.value.map{ it.score}.maxOrNull() ?: 0)
        }
        ?: AiResult.empty

    private fun meanScoreForList(entry: Map.Entry<PlyEnum, List<GamePosition>>): Int =
        if (entry.value.isEmpty()) 0 else entry.value.sumBy(GamePosition::score) / entry.value.size

    private fun playNMoves(position: GamePosition, nMoves: Int): Map<PlyEnum, List<GamePosition>> {
        var plyToPositions: Map<PlyEnum, List<GamePosition>> = playUserPlies(position)
            .fold(HashMap()) { aMap, mm ->
                aMap.apply {
                    put(mm.plyEnum, listOf(mm.position))
                }
            }
        (2..nMoves).forEach {
            plyToPositions = plyToPositions.mapValues {
                it.value.flatMap { pos ->
                    playUserPlies(pos).map(FirstMove::position)
                }
            }
        }
        return plyToPositions
    }

    private fun longestRandomPlayAdaptive(position: GamePosition, nAttemptsInitial: Int): AiResult =
        when (position.freeCount()) {
            in 0..5 -> 8
            in 6..8 -> 4
            in 9..11 -> 2
            else -> 1
        }.let {
            longestRandomPlay(position, nAttemptsInitial * it)
        }

    private fun longestRandomPlay(position: GamePosition, nAttempts: Int): AiResult {
        val firstMoves: List<FirstMove> = playUserPlies(position)

        val list: List<AiResult> = firstMoves.map { firstMove ->
            val attempts: MutableList<GamePosition> = ArrayList()
            repeat(nAttempts) {
                firstMove.position
                    .let(this::playRandomTillEnd)
                    .also(attempts::add)
            }
            AiResult(firstMove.plyEnum,
                attempts.sumBy { it.score } / attempts.size,
                attempts.map { it.score }.maxOrNull() ?: 0
            )
        }

        return list.maxByOrNull { it.referenceScore } ?: AiResult.empty
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

    private fun playUserPlies(position: GamePosition): List<FirstMove> = UserPlies
        .mapNotNull { plyEnum ->
            position.calcUserPly(plyEnum)
                .takeIf { it.prevPly.isNotEmpty() }
                ?.randomComputerPly()
                ?.let { FirstMove(plyEnum, it) }
        }

    private fun allowedRandomMove(position: GamePosition): GamePosition {
        UserPlies.shuffled().forEach {
            position.calcUserPly(it).also {
                if (it.prevPly.isNotEmpty()) return it
            }
        }
        return position.noPly()
    }

}