package org.andstatus.game2048

private const val keyPlayerEnum = "playerEnum"
private const val keyPlayersMoveEnum = "moveEnum"
private const val keyMoves = "moves"

data class PlayersMove(val player: PlayerEnum, val playersMoveEnum: PlayersMoveEnum, val moves: List<Move>) {

    fun toJson(): Map<String, Any> = mapOf(
        keyPlayerEnum to player.id,
        keyPlayersMoveEnum to playersMoveEnum.id,
        keyMoves to moves.map{ it.toJson() }
    )

    companion object{
        fun computersMove(placedPiece: PlacedPiece) =
                PlayersMove(PlayerEnum.SETTER, PlayersMoveEnum.PLACE, listOf(MovePlace(placedPiece)))

        fun usersMove(playersMoveEnum: PlayersMoveEnum, moves: List<Move>) =
                PlayersMove(PlayerEnum.SWIPER, playersMoveEnum, moves)

        fun fromJson(json: Any): PlayersMove? {
            val aMap: Map<String, Any> = json as Map<String, Any>
            val player = aMap[keyPlayerEnum]?.let { PlayerEnum.fromId(it.toString()) }
            val playersMoveEnum = aMap[keyPlayersMoveEnum]?.let { PlayersMoveEnum.fromId(it.toString())}
            val moves: List<Move>? = aMap[keyMoves]?.asJsonArray()?.mapNotNull { Move.fromJson(it) }
            return if (player != null && playersMoveEnum != null && moves != null)
                PlayersMove(player, playersMoveEnum, moves)
            else
                null;
        }
    }
}