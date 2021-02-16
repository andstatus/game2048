package org.andstatus.game2048.ai

import com.soywiz.klock.Stopwatch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.meanBy
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies
import org.andstatus.game2048.myLog
import org.andstatus.game2048.timeIsUp
import kotlin.math.pow

/** @author yvolk@yurivolkov.com */
class AiPlayer(val settings: Settings) {

    private class FirstMove(val plyEnum: PlyEnum, val positions: List<GamePosition>)

    fun nextPly(position: GamePosition): AiResult = Stopwatch().start().let { stopWatch ->
        when (settings.aiAlgorithm) {
            AiAlgorithm.RANDOM -> AiResult(allowedRandomMove(position))
            AiAlgorithm.MAX_SCORE_OF_ONE_MOVE -> moveWithMaxScore(position)
            AiAlgorithm.MAX_EMPTY_BLOCKS_OF_N_MOVES -> maxEmptyBlocksNMoves(position, 12, 5)
            AiAlgorithm.MAX_SCORE_OF_N_MOVES -> maxScoreNMoves(position, 12, 5)
            AiAlgorithm.LONGEST_RANDOM_PLAY -> longestRandomPlayAdaptive(position, 3, 3)
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
        .maxByOrNull{ it.referencePosition.score} ?: AiResult.empty(position)

    private fun playNMoves(position: GamePosition, nMoves: Int, timeIsUp: () -> Boolean): List<FirstMove> {
        var firstMoves: List<FirstMove> = playUserPlies(position)
        for (i in 2..nMoves) {
            if (timeIsUp()) break
            firstMoves = firstMoves
                .map { firstMove ->
                    firstMove.positions
                        .flatMap {
                            if (timeIsUp()) return firstMoves
                            playUserPlies(it)
                        }
                        .flatMap { it.positions }
                        .filter { it.prevPly.isNotEmpty() }
                        .let { FirstMove(firstMove.plyEnum, it) }
                }
        }
        return firstMoves
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
            if ( maxPosition.moveNumber - position.moveNumber > 60 || additionalAttempts > 12 || timeIsUp())
                return bestMove?.let { firstMove ->
                    AiResult(firstMove.plyEnum,
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
    ): List<FirstMove> = playUserPlies(position, 10)
        .map { firstMove ->
            val positions: MutableList<GamePosition> = ArrayList()
            for (i in 1..attemptsPower.intPowerOf2()) {
                for (pos1 in firstMove.positions) {
                    if (timeIsUp() && prevResult.isNotEmpty()) break
                    positions.add(playRandomTillEnd(pos1))
                }
            }
            prevResult.find { it.plyEnum == firstMove.plyEnum }?.let {
                positions.addAll(it.positions)
            }
            FirstMove(firstMove.plyEnum, positions)
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
                ?.randomComputerPly(Piece.N2)
                ?.let { FirstMove(plyEnum, listOf(it)) }
        }

    private fun playUserPlies(position: GamePosition, nTimes: Int): List<FirstMove> = UserPlies
        .mapNotNull { plyEnum ->
            position.calcUserPly(plyEnum).takeIf { it.prevPly.isNotEmpty() }
        }.map { position2 ->
            val positions: List<GamePosition> = (1..nTimes)
                .mapNotNull {
                    position2.randomComputerPly()
                        .takeIf { it.prevPly.isNotEmpty() }
                }
                .fold(emptyList(), { acc, position3 -> acc + position3 })
            FirstMove(position2.prevPly.plyEnum, positions)
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