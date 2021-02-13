package org.andstatus.game2048.ai

import com.soywiz.klock.Stopwatch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.model.PlyEnum.Companion.UserPlies
import org.andstatus.game2048.myLog
import kotlin.math.pow

/** @author yvolk@yurivolkov.com */
class AiPlayer(val settings: Settings) {

    private class FirstMove(val plyEnum: PlyEnum, val positions: List<GamePosition>)

    fun timeIsUp(seconds: Int): () -> Boolean {
        val stopwatch: Stopwatch = Stopwatch().start()
        return { stopwatch.elapsed.seconds >= seconds }
    }

    fun nextPly(position: GamePosition): AiResult = Stopwatch().start().let { stopWatch ->
        when (settings.aiAlgorithm) {
            AiAlgorithm.RANDOM -> AiResult(allowedRandomMove(position))
            AiAlgorithm.MAX_SCORE_OF_ONE_MOVE -> moveWithMaxScore(position)
            AiAlgorithm.MAX_EMPTY_BLOCKS_OF_N_MOVES -> maxEmptyBlocksNMoves(position, 12, 5)
            AiAlgorithm.MAX_SCORE_OF_N_MOVES -> maxScoreNMoves(position, 12, 5)
            AiAlgorithm.LONGEST_RANDOM_PLAY -> longestRandomPlayAdaptive(position, 3, 20)
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
                if (it.positions.isEmpty()) 0 else it.positions.sumBy { it.freeCount() } / it.positions.size
            }
            ?.let { entry ->
                AiResult(entry.plyEnum,
                    if(entry.positions.isEmpty()) 0 else entry.positions.sumBy { it.score } / entry.positions.size,
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
                if (it.positions.isEmpty()) 0 else it.positions.sumBy(GamePosition::score) / it.positions.size,
                it.positions.maxByOrNull { it.score } ?: position,
                "N$nMoves",
                position
            )
        }
        .maxByOrNull{ it.referenceScore} ?: AiResult.empty(position)

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
        do {
            var result: AiResult? = null
            result = longestRandomPlay(result, position, attemptsPowerOf2, timeIsUp)
            if (result.moreMoves > 50 || attemptsPowerOf2 > 18 || timeIsUp()) return result
            attemptsPowerOf2++
        } while (true)
    }

    private fun Int.intPowerOf2(): Int = 2.0f.pow(this).toInt()

    private fun longestRandomPlay(
        prevResult: AiResult?, position: GamePosition,
        attemptsPower: Int, timeIsUp: () -> Boolean
    ): AiResult =
        playUserPlies(position, 10).map { firstMove ->
            val positions: MutableList<GamePosition> = ArrayList()
            repeat(attemptsPower.intPowerOf2()) {
                firstMove.positions
                    .map {
                        if (prevResult != null && timeIsUp()) return prevResult
                        playRandomTillEnd(it)
                    }
                    .also(positions::addAll)
            }
            AiResult(firstMove.plyEnum,
                if (positions.isEmpty()) 0 else positions.sumBy { it.score } / positions.size,
                positions.maxByOrNull { it.score } ?: position,
                "N$attemptsPower",
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