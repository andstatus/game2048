package org.andstatus.game2048

sealed class Move {
    open fun points() = 0
}

class MoveOne(val first: PlacedPiece, val destination: Square) : Move()
class MoveMerge(val first: PlacedPiece, val second: PlacedPiece, val merged: PlacedPiece) : Move() {
    override fun points() = first.piece.value
}
class MovePlace(val first: PlacedPiece) : Move()
class MoveLoad(val board: Board) : Move() {
    override fun points() = board.score
}