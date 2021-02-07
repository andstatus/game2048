package org.andstatus.game2048.model

private const val keyPiece = "piece"
private const val keySquare = "square"

data class PlacedPiece(val piece: Piece, val square: Square) {
    fun toMap(): Map<String, Any> = mapOf(
            keyPiece to piece.id,
            keySquare to square.toMap()
    )

    companion object {
        fun fromJson(board: Board, json: Any): PlacedPiece? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val piece = aMap[keyPiece]?.let { Piece.fromId(it as Int) }
            val square = aMap[keySquare]?.let { Square.fromJson(board, it) }
            return if (piece != null && square != null)
                PlacedPiece(piece, square)
            else
                null
        }
    }
}