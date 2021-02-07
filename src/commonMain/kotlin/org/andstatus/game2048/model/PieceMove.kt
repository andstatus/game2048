package org.andstatus.game2048.model

import org.andstatus.game2048.Settings

private const val keyMoveEnum = "moveName"
private const val keyFirst = "first"
private const val keySecond = "second"
private const val keyMerged = "merged"
private const val keyDestination = "destination"
private const val keyBoard = "board"

sealed class PieceMove(val pieceMoveEnum: PieceMoveEnum) {
    open fun points() = 0

    fun toMap(): Map<String, Any> = when(this) {
        is PieceMoveOne -> mapOf(
                    keyMoveEnum to pieceMoveEnum.id,
                    keyFirst to first.toMap(),
                    keyDestination to destination.toMap()
            )
        is PieceMoveMerge -> mapOf(
                keyMoveEnum to pieceMoveEnum.id,
                keyFirst to first.toMap(),
                keySecond to second.toMap(),
                keyMerged to merged.toMap()
        )
        is PieceMovePlace -> mapOf(
                keyMoveEnum to pieceMoveEnum.id,
                keyFirst to first.toMap()
        )
        is PieceMoveLoad -> mapOf(
                keyMoveEnum to pieceMoveEnum.id,
                keyBoard to board.toMap()
        )
        is PieceMoveDelay -> emptyMap()
    }

    companion object {
        fun fromJson(settings: Settings, json: Any): PieceMove? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val moveEnum = aMap[keyMoveEnum]?.let { PieceMoveEnum.fromId(it as String) }
            return when(moveEnum) {
                PieceMoveEnum.ONE -> {
                    val first = aMap[keyFirst]?.let { PlacedPiece.fromJson(settings, it) }
                    val destination = aMap[keyDestination]?.let { Square.fromJson(settings, it) }
                    return if (first != null && destination != null)
                        PieceMoveOne(first, destination)
                    else
                        null;
                }
                PieceMoveEnum.MERGE -> {
                    val first = aMap[keyFirst]?.let { PlacedPiece.fromJson(settings, it) }
                    val second = aMap[keySecond]?.let { PlacedPiece.fromJson(settings, it) }
                    val merged = aMap[keyMerged]?.let { PlacedPiece.fromJson(settings, it) }
                    return if (first != null && second != null && merged != null)
                        PieceMoveMerge(first,second, merged)
                    else
                        null;
                }
                PieceMoveEnum.PLACE -> {
                    val first = aMap[keyFirst]?.let { PlacedPiece.fromJson(settings, it) }
                    return if (first != null)
                        PieceMovePlace(first)
                    else
                        null;
                }
                PieceMoveEnum.LOAD -> {
                    val board = aMap[keyBoard]?.let { Board.fromJson(settings, it) }
                    return if (board != null)
                        PieceMoveLoad(board)
                    else
                        null;
                }
                PieceMoveEnum.DELAY -> null
                null -> null
            }
        }
    }
}

data class PieceMoveOne(val first: PlacedPiece, val destination: Square) : PieceMove(PieceMoveEnum.ONE)
data class PieceMoveMerge(val first: PlacedPiece, val second: PlacedPiece, val merged: PlacedPiece) : PieceMove(PieceMoveEnum.MERGE) {
    override fun points() = first.piece.value
}
data class PieceMovePlace(val first: PlacedPiece) : PieceMove(PieceMoveEnum.PLACE)
data class PieceMoveLoad(val board: Board) : PieceMove(PieceMoveEnum.LOAD) {
    override fun points() = board.score
}
data class PieceMoveDelay(val delayMs: Int) : PieceMove(PieceMoveEnum.DELAY)