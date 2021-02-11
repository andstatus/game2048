package org.andstatus.game2048.ai

import com.soywiz.klock.Stopwatch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies

/** @author yvolk@yurivolkov.com */
class AiPlayer(val settings: Settings) {

    private class FirstMove(val plyEnum: PlyEnum, val positions: List<GamePosition>)

    fun nextPly(position: GamePosition): AiResult = Stopwatch().start().let { stopWatch ->
        when (settings.aiAlgorithm) {
            AiAlgorithm.RANDOM -> AiResult(allowedRandomMove(position))
            AiAlgorithm.MAX_SCORE_OF_ONE_MOVE -> moveWithMaxScore(position)
            AiAlgorithm.MAX_EMPTY_BLOCKS_OF_N_MOVES -> maxEmptyBlocksNMoves(position, 8)
            AiAlgorithm.MAX_SCORE_OF_N_MOVES -> maxScoreNMoves(position, 10)
            AiAlgorithm.LONGEST_RANDOM_PLAY -> longestRandomPlayAdaptive(position, 5)
        }.withContext(position, stopWatch.elapsed.millisecondsInt)
    }

    private fun moveWithMaxScore(position: GamePosition): AiResult = UserPlies
        .map(position::calcUserPly)
        .filter { it.prevPly.isNotEmpty() }
        .maxByOrNull { it.prevPly.points() }
        ?.let(::AiResult)
        ?: AiResult.empty(position)

    private fun maxEmptyBlocksNMoves(position: GamePosition, nMoves: Int): AiResult =
        playNMoves(position, nMoves)
            .maxByOrNull {
                if (it.value.isEmpty()) 0 else it.value.sumBy { it.freeCount() } / it.value.size
            }
            ?.let { entry ->
                AiResult(entry.key,
                    if(entry.value.isEmpty()) 0 else entry.value.sumBy { it.score } / entry.value.size,
                    entry.value.maxByOrNull { it.score } ?: position,
                    "N$nMoves",
                    position
                )
            }
            ?: AiResult.empty(position)

    private fun maxScoreNMoves(position: GamePosition, nMoves: Int): AiResult =
        playNMoves(position, nMoves)
        .map {
            AiResult(it.key,
                if (it.value.isEmpty()) 0 else it.value.sumBy(GamePosition::score) / it.value.size,
                it.value.maxByOrNull { it.score } ?: position,
                "N$nMoves",
                position
            )
        }
        .maxByOrNull{ it.referenceScore} ?: AiResult.empty(position)

    private fun playNMoves(position: GamePosition, nMoves: Int): Map<PlyEnum, List<GamePosition>> =
        playUserPlies(position).map { firstMove ->
            var positions: List<GamePosition> = firstMove.positions
            repeat(nMoves - 1) {
                positions = positions
                    .flatMap(this::playUserPlies)
                    .flatMap { it.positions }
            }
            firstMove.plyEnum to positions
        }.toMap()

    private fun longestRandomPlayAdaptive(position: GamePosition, nAttemptsInitial: Int): AiResult {
        var multiplier = when (position.freeCount()) {
            in 0..1 -> 16
            in 2..5 -> 8
            in 6..8 -> 4
            in 9..11 -> 2
            else -> 1
        }
        do {
            val result = longestRandomPlay(position, nAttemptsInitial * multiplier)
            if (result.moreMoves > 50 || multiplier > 512) return result
            multiplier *= 2
        } while (true)
    }

    private fun longestRandomPlay(position: GamePosition, nAttempts: Int): AiResult =
        playUserPlies(position, 10).map { firstMove ->
            val positions: MutableList<GamePosition> = ArrayList()
            repeat(nAttempts) {
                firstMove.positions
                    .map(this::playRandomTillEnd)
                    .also(positions::addAll)
            }
            AiResult(firstMove.plyEnum,
                if (positions.isEmpty()) 0 else positions.sumBy { it.score } / positions.size,
                positions.maxByOrNull { it.score } ?: position,
                "N$nAttempts",
                position
            )
        }.maxByOrNull { it.referenceScore } ?: AiResult.empty(position)

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
                ?.randomComputerPly(Piece.N2)
                ?.let { FirstMove(plyEnum, listOf(it)) }
        }

    private fun playUserPlies(position: GamePosition, nTimes: Int): List<FirstMove> = UserPlies
        .mapNotNull { plyEnum ->
            position.calcUserPly(plyEnum).takeIf { it.prevPly.isNotEmpty() }
        }.map { position2 ->
            FirstMove(position2.prevPly.plyEnum, (1..nTimes).map { position2.randomComputerPly()})
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