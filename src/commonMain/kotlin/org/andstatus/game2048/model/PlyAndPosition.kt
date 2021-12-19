package org.andstatus.game2048.model

class PlyAndPosition(val ply:Ply, val position: GamePosition) {

    fun copy(plyNew: Ply = ply.copy()): PlyAndPosition = PlyAndPosition(
        plyNew,
        with (position) {
            GamePosition(board, pieces.copyOf(), score, startingDateTime, gameClock.copy(), retries, plyNumber)
        }
    )

    override fun equals(other: Any?): Boolean {
        return other is PlyAndPosition && ply.plyEnum == other.ply.plyEnum &&
            position.plyNumber == other.position.plyNumber
    }

    override fun hashCode(): Int {
        var result = ply.plyEnum.hashCode()
        result = 31 * result + position.plyNumber
        return result
    }

    override fun toString(): String = "${ply.plyEnum.id}, $position"

}
