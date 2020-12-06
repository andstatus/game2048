package org.andstatus.game2048.model

private const val keyPlayerEnum = "playerEnum"
private const val keyPlayersMoveEnum = "moveEnum"
private const val keySeconds = "seconds"
private const val keyMoves = "moves"

data class PlayerMove(val player: PlayerEnum, val playerMoveEnum: PlayerMoveEnum, val seconds: Int, val moves: List<Move>) {

    fun toMap(): Map<String, Any> = mapOf(
        keyPlayerEnum to player.id,
        keyPlayersMoveEnum to playerMoveEnum.id,
        keySeconds to seconds,
        keyMoves to moves.map{ it.toMap() }
    )

    companion object{
        fun composerMove(board: Board) =
                PlayerMove(PlayerEnum.COMPOSER, PlayerMoveEnum.LOAD, board.gameClock.playedSeconds, listOf(MoveLoad(board)))

        fun computerMove(placedPiece: PlacedPiece, seconds: Int) =
                PlayerMove(PlayerEnum.COMPUTER, PlayerMoveEnum.PLACE, seconds, listOf(MovePlace(placedPiece)))

        fun userMove(playerMoveEnum: PlayerMoveEnum, seconds: Int, moves: List<Move>) =
                PlayerMove(PlayerEnum.USER, playerMoveEnum, seconds, moves)

        fun delay(delayMs: Int = 500) =
                PlayerMove(PlayerEnum.COMPOSER, PlayerMoveEnum.DELAY, 0, listOf(MoveDelay(delayMs)))

        fun fromJson(json: Any): PlayerMove? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val player = aMap[keyPlayerEnum]?.let { PlayerEnum.fromId(it.toString()) }
            val playersMoveEnum = aMap[keyPlayersMoveEnum]?.let { PlayerMoveEnum.fromId(it.toString()) }
            val seconds: Int = aMap[keySeconds] as Int? ?: 0
            val moves: List<Move>? = aMap[keyMoves]?.asJsonArray()?.mapNotNull { Move.fromJson(it) }
            return if (player != null && playersMoveEnum != null && moves != null)
                PlayerMove(player, playersMoveEnum, seconds, moves)
            else
                null
        }
    }
}