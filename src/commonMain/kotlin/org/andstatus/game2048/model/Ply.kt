package org.andstatus.game2048.model

private const val keyPlayerEnum = "playerEnum"
private const val keyPlyEnum = "moveEnum"
private const val keySeconds = "seconds"
private const val keyMoves = "moves"

data class Ply(val player: PlayerEnum, val plyEnum: PlyEnum, val seconds: Int, val pieceMoves: List<PieceMove>) {

    fun toMap(): Map<String, Any> = mapOf(
        keyPlayerEnum to player.id,
        keyPlyEnum to plyEnum.id,
        keySeconds to seconds,
        keyMoves to pieceMoves.map{ it.toMap() }
    )

    fun isEmpty() = plyEnum.isEmpty()
    fun isNotEmpty() = !isEmpty()

    fun points(): Int = pieceMoves.map { it.points() }.sum()

    companion object{
        val emptyPly = Ply(PlayerEnum.COMPOSER, PlyEnum.EMPTY, 0, emptyList())

        fun composerPly(position: GamePosition) =
                Ply(PlayerEnum.COMPOSER, PlyEnum.LOAD, position.data.gameClock.playedSeconds, listOf(PieceMoveLoad(position)))

        fun computerPly(placedPiece: PlacedPiece, seconds: Int) =
                Ply(PlayerEnum.COMPUTER, PlyEnum.PLACE, seconds, listOf(PieceMovePlace(placedPiece)))

        fun userPly(plyEnum: PlyEnum, seconds: Int, pieceMoves: List<PieceMove>) =
                Ply(PlayerEnum.USER, plyEnum, seconds, pieceMoves)

        fun delay(delayMs: Int = 500) =
                Ply(PlayerEnum.COMPOSER, PlyEnum.DELAY, 0, listOf(PieceMoveDelay(delayMs)))

        fun fromJson(board: Board, json: Any): Ply? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val player = aMap[keyPlayerEnum]?.let { PlayerEnum.fromId(it.toString()) }
            val plyEnum = aMap[keyPlyEnum]?.let { PlyEnum.fromId(it.toString()) }
            val seconds: Int = aMap[keySeconds] as Int? ?: 0
            val pieceMoves: List<PieceMove>? = aMap[keyMoves]?.asJsonArray()?.mapNotNull { PieceMove.fromJson(board, it) }
            return if (player != null && plyEnum != null && pieceMoves != null)
                Ply(player, plyEnum, seconds, pieceMoves)
            else
                null
        }
    }
}