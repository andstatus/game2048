package org.andstatus.game2048

enum class PlayerMoveEnum(val id: String) {
    LEFT("left"),
    RIGHT("right"),
    UP("up"),
    DOWN("down"),
    PLACE("place"),
    LOAD("load");

    fun reverseDirection(): Direction = when(this) {
        LEFT -> Direction.RIGHT
        RIGHT -> Direction.LEFT
        UP -> Direction.DOWN
        DOWN -> Direction.UP
        else -> Direction.LEFT
    }

    companion object {
        fun fromId(value: String): PlayerMoveEnum? = values().firstOrNull { it.id == value}
    }
}