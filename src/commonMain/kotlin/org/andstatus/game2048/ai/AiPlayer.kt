package org.andstatus.game2048.ai

import com.soywiz.klock.Stopwatch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.meanBy
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies
import org.andstatus.game2048.myLog
import org.andstatus.game2048.timeIsUp
import kotlin.math.pow

/** @author yvolk@yurivolkov.com */
class AiPlayer(val settings: Settings) {

    private class FirstMove(val plyEnum: PlyEnum, val positions: List<GamePosition>) {
        fun randomComputerPly() = FirstMove(plyEnum, positions.map { it.randomComputerPly() })
        inline fun mapPositions(action: (List<GamePosition>) -> List<GamePosition>) =
            FirstMove(plyEnum, action(positions))
    }

    fun nextPly(position: GamePosition): AiResult = Stopwatch().start().let { stopWatch ->
        when (settings.aiAlgorithm) {
            AiAlgorithm.RANDOM -> AiResult(allowedRandomMove(position))
            AiAlgorithm.MAX_SCORE_OF_ONE_MOVE -> moveWithMaxScore(position)
            AiAlgorithm.MAX_EMPTY_BLOCKS_OF_N_MOVES -> maxEmptyBlocksNMoves(position, 12, 5)
            AiAlgorithm.MAX_SCORE_OF_N_MOVES -> maxScoreNMoves(position, 12, 5)
            AiAlgorithm.LONGEST_RANDOM_PLAY -> longestRandomPlayAdaptive(position, 3, 10)
        }.withContext(position, stopWatch.elapsed.millisecondsInt)
            .also { myLog(it) }
    }

    private fun moveWithMaxScore(position: GamePosition): AiResult = UserPlies
        .map(position::calcUserPly)
        .filter { it.prevPly.isNotEmpty() }
        .maxByOrNull { it.prevPly.points() }
        ?.let(::AiResult)
        ?: AiResult.empty(position)

    private fun maxEmptyBlocksNMoves(position: GamePosition, nMoves: Int, maxSeconds: Int): AiResult =
        playNMoves(position, nMoves, timeIsUp(maxSeconds))
            .maxByOrNull {
                it.positions.meanBy { it.freeCount() }
            }
            ?.let { entry ->
                AiResult(entry.plyEnum,
                    entry.positions.mean() ?: position,
                    entry.positions.maxByOrNull { it.score } ?: position,
                    "N$nMoves",
                    position
                )
            }
            ?: AiResult.empty(position)

    private fun maxScoreNMoves(position: GamePosition, nMoves: Int, maxSeconds: Int): AiResult =
        playNMoves(position, nMoves, timeIsUp(maxSeconds))
            .map {
                AiResult(it.plyEnum,
                    it.positions.mean() ?: position,
                    it.positions.maxByOrNull { it.score } ?: position,
                    "N$nMoves",
                    position
                )
            }
            .maxByOrNull { it.referencePosition.score } ?: AiResult.empty(position)

    private fun playNMoves(position: GamePosition, nMoves: Int, timeIsUp: () -> Boolean): List<FirstMove> {
        var allMoves: List<FirstMove> = emptyList()
        for (i in 1..nMoves) {
            allMoves = if (allMoves.isEmpty()) {
                playUserPlies(position).map { it.randomComputerPly() }
            } else allMoves.map {
                it.mapPositions { positions: List<GamePosition> -> positions
                    .flatMap {
                        if (timeIsUp()) return allMoves
                        playUserPlies(it)
                    }
                    .map { it.randomComputerPly() }
                    .flatMap { it.positions }
                }
            }
        }
        return allMoves
    }

    private fun longestRandomPlayAdaptive(position: GamePosition, powerInitial: Int, maxSeconds: Int): AiResult {
        val timeIsUp = timeIsUp(maxSeconds)
        var attemptsPowerOf2: Int = powerInitial + when (position.freeCount()) {
            in 0..1 -> 4
            in 2..5 -> 3
            in 6..8 -> 2
            in 9..11 -> 1
            else -> 0
        }
        var additionalAttempts = 0
        var prevMoves: List<FirstMove> = emptyList()
        do {
            prevMoves = longestRandomPlay(prevMoves, position, attemptsPowerOf2, timeIsUp)
            val bestMove: FirstMove? = prevMoves.maxByOrNull { it.positions.meanBy(GamePosition::score) }
            val referencePosition = bestMove?.positions?.mean() ?: position
            val maxPosition = bestMove?.positions?.maxByOrNull { it.score } ?: position
            if (maxPosition.moveNumber - position.moveNumber > 50 || additionalAttempts > 13 || timeIsUp())
                return bestMove?.let { firstMove ->
                    AiResult(
                        firstMove.plyEnum,
                        referencePosition,
                        maxPosition,
                        "N$attemptsPowerOf2",
                        position
                    )
                } ?: AiResult.empty(position)
            attemptsPowerOf2++
            additionalAttempts++
        } while (true)
    }

    private fun Int.intPowerOf2(): Int = 2.0f.pow(this).toInt()

    private fun List<GamePosition>.mean(): GamePosition? = sortedBy { it.score }.let {
        when {
            it.isEmpty() -> null
            it.size == 1 -> it.get(0)
            else -> it.get(it.size / 2)
        }
    }

    private fun longestRandomPlay(
        prevResult: List<FirstMove>, position: GamePosition,
        attemptsPower: Int, timeIsUp: () -> Boolean
    ): List<FirstMove> {
        var allMoves: List<FirstMove> = prevResult
        val firstMoves: List<FirstMove> = playUserPlies(position)
        if (firstMoves.isEmpty()) return allMoves

        for (i in 1..attemptsPower.intPowerOf2() * 10) {
            allMoves = firstMoves.map { firstMove ->
                val nextMove = firstMove.randomComputerPly()
                    .mapPositions {
                        it.map { pos ->
                            if (timeIsUp() && allMoves.isNotEmpty()) return allMoves

                            playRandomTillEnd(pos)
                        }
                    }

                allMoves.find { it.plyEnum == firstMove.plyEnum }
                    ?.let { move ->
                        nextMove.mapPositions { it + move.positions }
                    }
                    ?: nextMove
            }
        }
        return allMoves
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
                ?.let { FirstMove(plyEnum, listOf(it)) }
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