package org.andstatus.game2048.model

private const val keyPlayerEnum = "player"
private const val keyPlayerEnumV1 = "playerEnum"
private const val keyPlyEnum = "ply"
private const val keyPlyEnumV1 = "moveEnum"
private const val keySeconds = "seconds"
private const val keyMoves = "moves"
private const val keyRetries = "retries"

/** @author yvolk@yurivolkov.com
 * on Ply term see https://en.wikipedia.org/wiki/Ply_(game_theory)
 * */
data class Ply(
    val player: PlayerEnum,
    val plyEnum: PlyEnum,
    val seconds: Int,
    val retries: Int,
    val pieceMoves: List<PieceMove>
) {

    fun toMap(): Map<String, Any> = mapOf(
        keySeconds to seconds,
        keyMoves to pieceMoves.map { it.toMap() }
    )
        .let { if (player == PlayerEnum.COMPUTER) it else it + (keyPlayerEnum to player.id) }
        .let { if (plyEnum == PlyEnum.PLACE) it else it + (keyPlyEnum to plyEnum.id) }
        .let { if (retries == 0) it else it + (keyRetries to retries) }

    fun isEmpty() = plyEnum.isEmpty()
    fun isNotEmpty() = !isEmpty()

    fun points(): Int = pieceMoves.map { it.points() }.sum()

    fun isValid(board: Board): Boolean = this.isNotEmpty() && (this.pieceMoves.isNotEmpty() ||
            (player == PlayerEnum.USER && board.allowUsersMoveWithoutBlockMoves))

    companion object{
        val emptyPly = Ply(PlayerEnum.COMPOSER, PlyEnum.EMPTY, 0, 0, emptyList())

        fun composerPly(position: GamePosition) =
                Ply(
                    PlayerEnum.COMPOSER,
                    PlyEnum.LOAD,
                    position.gameClock.playedSeconds,
                    0,
                    listOf(PieceMoveLoad(position))
                )

        fun computerPly(placedPiece: PlacedPiece, seconds: Int) =
                Ply(PlayerEnum.COMPUTER, PlyEnum.PLACE, seconds, 0, listOf(PieceMovePlace(placedPiece)))

        fun userPly(plyEnum: PlyEnum, seconds: Int, retries: Int, pieceMoves: List<PieceMove>) =
                Ply(PlayerEnum.USER, plyEnum, seconds, retries, pieceMoves)

        fun delay(delayMs: Int = 500) =
                Ply(PlayerEnum.COMPOSER, PlyEnum.DELAY, 0, 0, listOf(PieceMoveDelay(delayMs)))

        fun fromJson(board: Board, json: Any): Ply? {
            val aMap: Map<String, Any> = json.parseJsonMap()
            val player = (aMap[keyPlayerEnum] ?: aMap[keyPlayerEnumV1])?.let { PlayerEnum.fromId(it.toString()) }
                ?: PlayerEnum.COMPUTER
            val plyEnum = (aMap[keyPlyEnum] ?: aMap[keyPlyEnumV1])?.let { PlyEnum.fromId(it.toString()) }
                ?: PlyEnum.PLACE
            val seconds: Int = aMap[keySeconds] as Int? ?: 0
            val retries: Int = aMap[keyRetries] as Int? ?: 0
            val pieceMoves: List<PieceMove>? = aMap[keyMoves]?.parseJsonArray()?.mapNotNull { PieceMove.fromJson(board, it) }
            return if (pieceMoves != null)
                Ply(player, plyEnum, seconds, retries, pieceMoves)
            else
                null
        }
    }
}