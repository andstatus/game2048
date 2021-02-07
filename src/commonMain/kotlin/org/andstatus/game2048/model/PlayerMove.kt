package org.andstatus.game2048.model

import org.andstatus.game2048.Settings

private const val keyPlayerEnum = "playerEnum"
private const val keyPlayersMoveEnum = "moveEnum"
private const val keySeconds = "seconds"
private const val keyMoves = "moves"

data class PlayerMove(val player: PlayerEnum, val playerMoveEnum: PlayerMoveEnum, val seconds: Int, val pieceMoves: List<PieceMove>) {

    fun toMap(): Map<String, Any> = mapOf(
        keyPlayerEnum to player.id,
        keyPlayersMoveEnum to playerMoveEnum.id,
        keySeconds to seconds,
        keyMoves to pieceMoves.map{ it.toMap() }
    )

    fun isEmpty() = playerMoveEnum.isEmpty()
    fun isNotEmpty() = !isEmpty()

    fun points(): Int = pieceMoves.map { it.points() }.sum()

    companion object{
        val emptyMove = PlayerMove(PlayerEnum.COMPOSER, PlayerMoveEnum.EMPTY, 0, emptyList())

        fun composerMove(board: Board) =
                PlayerMove(PlayerEnum.COMPOSER, PlayerMoveEnum.LOAD, board.gameClock.playedSeconds, listOf(PieceMoveLoad(board)))

        fun computerMove(placedPiece: PlacedPiece, seconds: Int) =
                PlayerMove(PlayerEnum.COMPUTER, PlayerMoveEnum.PLACE, seconds, listOf(PieceMovePlace(placedPiece)))

        fun userMove(playerMoveEnum: PlayerMoveEnum, seconds: Int, pieceMoves: List<PieceMove>) =
                PlayerMove(PlayerEnum.USER, playerMoveEnum, seconds, pieceMoves)

        fun delay(delayMs: Int = 500) =
                PlayerMove(PlayerEnum.COMPOSER, PlayerMoveEnum.DELAY, 0, listOf(PieceMoveDelay(delayMs)))

        fun fromJson(settings: Settings, json: Any): PlayerMove? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val player = aMap[keyPlayerEnum]?.let { PlayerEnum.fromId(it.toString()) }
            val playersMoveEnum = aMap[keyPlayersMoveEnum]?.let { PlayerMoveEnum.fromId(it.toString()) }
            val seconds: Int = aMap[keySeconds] as Int? ?: 0
            val pieceMoves: List<PieceMove>? = aMap[keyMoves]?.asJsonArray()?.mapNotNull { PieceMove.fromJson(settings, it) }
            return if (player != null && playersMoveEnum != null && pieceMoves != null)
                PlayerMove(player, playersMoveEnum, seconds, pieceMoves)
            else
                null
        }
    }
}