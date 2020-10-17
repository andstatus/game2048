package org.andstatus.game2048

private const val keyPiece = "piece"
private const val keySquare = "square"

data class PlacedPiece(val piece: Piece, val square: Square) {
    fun toJson(): Map<String, Any> = mapOf(
            keyPiece to piece.id,
            keySquare to square.toJson()
    )

    companion object {
        fun fromJson(json: Any): PlacedPiece? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val piece = aMap[keyPiece]?.let { Piece.fromId(it as Int) }
            val square = aMap[keySquare]?.let { Square.fromJson(it) }
            return if (piece != null && square != null)
                PlacedPiece(piece, square)
            else
                null
        }
    }
}