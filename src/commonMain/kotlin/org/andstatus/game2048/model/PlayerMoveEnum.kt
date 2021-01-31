package org.andstatus.game2048.model

enum class PlayerMoveEnum(val id: String, val symbol: String) {
    LEFT("left", "←"),
    RIGHT("right", "→"),
    UP("up", "↑"),
    DOWN("down", "↓"),
    PLACE("place", ""),
    LOAD("load", ""),
    DELAY("delay", "");

    fun reverseDirection(): Direction = when(this) {
        LEFT -> Direction.RIGHT
        RIGHT -> Direction.LEFT
        UP -> Direction.DOWN
        DOWN -> Direction.UP
        else -> Direction.LEFT
    }

    companion object {
        val UserMoves get() = listOf(LEFT, RIGHT, UP, DOWN)
        fun fromId(value: String): PlayerMoveEnum? = values().firstOrNull { it.id == value}
    }
}