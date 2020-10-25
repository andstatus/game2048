package org.andstatus.game2048

private const val keyPlayerEnum = "playerEnum"
private const val keyPlayersMoveEnum = "moveEnum"
private const val keyMoves = "moves"

data class PlayerMove(val player: PlayerEnum, val playerMoveEnum: PlayerMoveEnum, val moves: List<Move>) {

    fun toJson(): Map<String, Any> = mapOf(
        keyPlayerEnum to player.id,
        keyPlayersMoveEnum to playerMoveEnum.id,
        keyMoves to moves.map{ it.toJson() }
    )

    companion object{
        fun composerMove(board: Board) =
                PlayerMove(PlayerEnum.COMPOSER, PlayerMoveEnum.LOAD, listOf(MoveLoad(board)))

        fun computerMove(placedPiece: PlacedPiece) =
                PlayerMove(PlayerEnum.COMPUTER, PlayerMoveEnum.PLACE, listOf(MovePlace(placedPiece)))

        fun userMove(playerMoveEnum: PlayerMoveEnum, moves: List<Move>) =
                PlayerMove(PlayerEnum.USER, playerMoveEnum, moves)

        fun delay(delayMs: Int = 500) =
                PlayerMove(PlayerEnum.COMPOSER, PlayerMoveEnum.DELAY, listOf(MoveDelay(delayMs)))

        fun fromJson(json: Any): PlayerMove? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val player = aMap[keyPlayerEnum]?.let { PlayerEnum.fromId(it.toString()) }
            val playersMoveEnum = aMap[keyPlayersMoveEnum]?.let { PlayerMoveEnum.fromId(it.toString())}
            val moves: List<Move>? = aMap[keyMoves]?.asJsonArray()?.mapNotNull { Move.fromJson(it) }
            return if (player != null && playersMoveEnum != null && moves != null)
                PlayerMove(player, playersMoveEnum, moves)
            else
                null
        }
    }
}