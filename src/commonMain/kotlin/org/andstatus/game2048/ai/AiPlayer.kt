package org.andstatus.game2048.ai

import com.soywiz.klock.Stopwatch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.GameModel
import org.andstatus.game2048.model.MovesAndModel
import org.andstatus.game2048.model.PlayerMove
import org.andstatus.game2048.model.PlayerMoveEnum
import org.andstatus.game2048.model.PlayerMoveEnum.Companion.UserMoves
import kotlin.random.Random
import kotlin.random.nextInt

class AiPlayer(val settings: Settings) {

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
            .let { map ->
                map.maxByOrNull(this::meanScoreForList)
                ?.let {
                    MoveAndScore(it.key, meanScoreForList(it), it.value.map{ it.score}.maxOrNull() ?: 0)
                }
            }
            ?: MoveAndScore(allowedRandomMove(board))
    }

    private fun maxScoreForList(entry: Map.Entry<PlayerMoveEnum, List<GameModel>>): Int {
        return entry.value.map(GameModel::score).maxOrNull() ?: 0
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
        var models: Map<PlayerMoveEnum, List<GameModel>> = playUserMoves(fromBoard(board))
            .map(this::playComputerMove)
            .fold(HashMap()) { aMap, mm ->
                mm.moves.firstOrNull()?.let {
                    aMap.put(it.playerMoveEnum, listOf(mm.model))
                    aMap
                } ?: aMap
            }
        (2..nMoves).forEach {
            models = models.mapValues {
                it.value.flatMap { model ->
                    playUserMoves(model)
                        .map(this::playComputerMove)
                }.map { it.model }
            }
        }
        return models
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
        val firstMoves: Map<PlayerMoveEnum, GameModel> = playUserMoves(fromBoard(board))
            .map(this::playComputerMove)
            .fold(HashMap()) { map, mm ->
                mm.moves.firstOrNull()?.let {
                    map.put(it.playerMoveEnum, mm.model)
                    map
                } ?: map
            }

        val list = firstMoves.map { entry ->
            val attempts: MutableList<GameModel> = ArrayList()
            repeat(nAttempts) {
                attempts.add(playRandomTillEnd(entry.value))
            }
            MoveAndScore(entry.key,
                attempts.sumBy { it.score } / attempts.size,
                attempts.map{ it.score}.maxOrNull() ?: 0
            )
        }

        return list.maxByOrNull { it.referenceScore } ?: MoveAndScore(allowedRandomMove(board))
    }

    private fun playRandomTillEnd(modelIn: GameModel): GameModel {
        var model = modelIn
        while (!model.noMoreMoves()) {
            model = allowedRandomMove(model.board)
                .let(model::play)
                .let(this::playComputerMove)
                .model
        }
        return model
    }

    private fun playUserMoves(model: GameModel): List<MovesAndModel> = UserMoves
        .map(model::calcMove)
        .filter { it.moves.isNotEmpty() }
        .map(model::play)

    private fun playComputerMove(mm: MovesAndModel): MovesAndModel {
        return mm.model.randomComputerMove().let { MovesAndModel(mm.moves, it.model) }
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