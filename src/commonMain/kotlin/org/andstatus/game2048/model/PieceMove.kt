package org.andstatus.game2048.model

private const val keyPieceMoveEnum = "move"
private const val keyPieceMoveEnumV1 = "moveName"
private const val keyFirst = "first"
private const val keySecond = "second"
private const val keyMerged = "merged"
private const val keyDestination = "destination"
private const val keyPosition = "board"

sealed class PieceMove(val pieceMoveEnum: PieceMoveEnum) {
    open fun points() = 0

    fun toMap(): Map<String, Any> = when (this) {
        is PieceMoveOne -> mapOf(
            keyPieceMoveEnum to pieceMoveEnum.id,
            keyFirst to first.toMap(),
            keyDestination to destination.toMap()
        )

        is PieceMoveMerge -> mapOf(
            keyPieceMoveEnum to pieceMoveEnum.id,
            keyFirst to first.toMap(),
            keySecond to second.toMap(),
            keyMerged to merged.toMap()
        )

        is PieceMovePlace -> mapOf(
            keyPieceMoveEnum to pieceMoveEnum.id,
            keyFirst to first.toMap()
        )

        is PieceMoveLoad -> mapOf(
            keyPieceMoveEnum to pieceMoveEnum.id,
            keyPosition to position.toMap()
        )

        is PieceMoveDelay -> emptyMap()
    }

    companion object {
        fun fromJson(board: Board, json: Any): PieceMove? {
            val aMap: Map<String, Any> = json.parseJsonMap()
            val moveEnum =
                (aMap[keyPieceMoveEnum] ?: aMap[keyPieceMoveEnumV1])?.let { PieceMoveEnum.fromId(it as String) }
            return when (moveEnum) {
                PieceMoveEnum.ONE -> {
                    val first = aMap[keyFirst]?.let { PlacedPiece.fromJson(board, it) }
                    val destination = aMap[keyDestination]?.let { Square.fromJson(board, it) }
                    return if (first != null && destination != null)
                        PieceMoveOne(first, destination)
                    else
                        null;
                }

                PieceMoveEnum.MERGE -> {
                    val first = aMap[keyFirst]?.let { PlacedPiece.fromJson(board, it) }
                    val second = aMap[keySecond]?.let { PlacedPiece.fromJson(board, it) }
                    val merged = aMap[keyMerged]?.let { PlacedPiece.fromJson(board, it) }
                    return if (first != null && second != null && merged != null)
                        PieceMoveMerge(first, second, merged)
                    else
                        null;
                }

                PieceMoveEnum.PLACE -> {
                    val first = aMap[keyFirst]?.let { PlacedPiece.fromJson(board, it) }
                    return if (first != null)
                        PieceMovePlace(first)
                    else
                        null;
                }

                PieceMoveEnum.LOAD -> {
                    val position = aMap[keyPosition]?.let { GamePosition.fromJson(board, it) }
                    return if (position != null)
                        PieceMoveLoad(position)
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
data class PieceMoveMerge(val first: PlacedPiece, val second: PlacedPiece, val merged: PlacedPiece) :
    PieceMove(PieceMoveEnum.MERGE) {
    override fun points() = first.piece.value
}

data class PieceMovePlace(val first: PlacedPiece) : PieceMove(PieceMoveEnum.PLACE)
data class PieceMoveLoad(val position: GamePosition) : PieceMove(PieceMoveEnum.LOAD) {
    override fun points() = position.score
}

data class PieceMoveDelay(val delayMs: Int) : PieceMove(PieceMoveEnum.DELAY)