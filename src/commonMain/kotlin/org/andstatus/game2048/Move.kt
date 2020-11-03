package org.andstatus.game2048

private const val keyMoveEnum = "moveName"
private const val keyFirst = "first"
private const val keySecond = "second"
private const val keyMerged = "merged"
private const val keyDestination = "destination"
private const val keyBoard = "board"

sealed class Move(val moveEnum: MoveEnum) {
    open fun points() = 0

    fun toMap(): Map<String, Any> = when(this) {
        is MoveOne -> mapOf(
                    keyMoveEnum to moveEnum.id,
                    keyFirst to first.toMap(),
                    keyDestination to destination.toMap()
            )
        is MoveMerge -> mapOf(
                keyMoveEnum to moveEnum.id,
                keyFirst to first.toMap(),
                keySecond to second.toMap(),
                keyMerged to merged.toMap()
        )
        is MovePlace -> mapOf(
                keyMoveEnum to moveEnum.id,
                keyFirst to first.toMap()
        )
        is MoveLoad -> mapOf(
                keyMoveEnum to moveEnum.id,
                keyBoard to board.toMap()
        )
        is MoveDelay -> emptyMap()
    }

    companion object {
        fun fromJson(json: Any): Move? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val moveEnum = aMap[keyMoveEnum]?.let { MoveEnum.fromId(it as String) }
            return when(moveEnum) {
                MoveEnum.ONE -> {
                    val first = aMap[keyFirst]?.let { PlacedPiece.fromJson(it)}
                    val destination = aMap[keyDestination]?.let { Square.fromJson(it)}
                    return if (first != null && destination != null)
                        MoveOne(first, destination)
                    else
                        null;
                }
                MoveEnum.MERGE -> {
                    val first = aMap[keyFirst]?.let { PlacedPiece.fromJson(it)}
                    val second = aMap[keySecond]?.let { PlacedPiece.fromJson(it)}
                    val merged = aMap[keyMerged]?.let { PlacedPiece.fromJson(it)}
                    return if (first != null && second != null && merged != null)
                        MoveMerge(first,second, merged)
                    else
                        null;
                }
                MoveEnum.PLACE -> {
                    val first = aMap[keyFirst]?.let { PlacedPiece.fromJson(it)}
                    return if (first != null)
                        MovePlace(first)
                    else
                        null;
                }
                MoveEnum.LOAD -> {
                    val board = aMap[keyBoard]?.let { Board.fromJson(it)}
                    return if (board != null)
                        MoveLoad(board)
                    else
                        null;
                }
                MoveEnum.DELAY -> null
                null -> null
            }
        }
    }
}

data class MoveOne(val first: PlacedPiece, val destination: Square) : Move(MoveEnum.ONE)
data class MoveMerge(val first: PlacedPiece, val second: PlacedPiece, val merged: PlacedPiece) : Move(MoveEnum.MERGE) {
    override fun points() = first.piece.value
}
data class MovePlace(val first: PlacedPiece) : Move(MoveEnum.PLACE)
data class MoveLoad(val board: Board) : Move(MoveEnum.LOAD) {
    override fun points() = board.score
}
data class MoveDelay(val delayMs: Int) : Move(MoveEnum.DELAY)