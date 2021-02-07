package org.andstatus.game2048.model

private const val keyX = "x"
private const val keyY = "y"

data class Square(val x: Int, val y: Int) {
    var ind: Int = 0
    var nextToIterate: Array<Square?> = Array(4) { null }
    var nextInDirections: Array<Square?> = Array(4) { null }

    fun nextToIterate(direction: Direction): Square? =
            when (direction) {
                Direction.LEFT -> nextToIterate[0]
                Direction.RIGHT -> nextToIterate[1]
                Direction.UP -> nextToIterate[2]
                Direction.DOWN -> nextToIterate[3]
            }

    fun nextInThe(direction: Direction) : Square? =
            when (direction) {
                Direction.LEFT -> nextInDirections[0]
                Direction.RIGHT -> nextInDirections[1]
                Direction.UP -> nextInDirections[2]
                Direction.DOWN -> nextInDirections[3]
            }

    fun toMap(): Map<String, Int> = mapOf(
            keyX to x,
            keyY to y
    )

    companion object {
        fun fromJson(board: Board, json: Any): Square? {
            val aMap: Map<String, Any> = json.asJsonMap()
            val x = aMap[keyX] as Int?
            val y = aMap[keyY] as Int?
            return if (x != null && x >= 0 && y != null && y >= 0)
                board.toSquare(x, y)
            else
                null;
        }
    }
}