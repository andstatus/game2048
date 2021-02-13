package org.andstatus.game2048.model

enum class PlyEnum(val id: String, val symbol: String) {
    LEFT("left", "←"),
    RIGHT("right", "→"),
    UP("up", "↑"),
    DOWN("down", "↓"),
    PLACE("place", ""),
    LOAD("load", ""),
    DELAY("delay", ""),
    EMPTY("empty", ""),
    ;

    fun isEmpty() = this == EMPTY

    fun reverseDirection(): Direction = when(this) {
        LEFT -> Direction.RIGHT
        RIGHT -> Direction.LEFT
        UP -> Direction.DOWN
        DOWN -> Direction.UP
        else -> Direction.LEFT
    }

    companion object {
        val UserPlies get() = listOf(LEFT, RIGHT, UP, DOWN)
        fun fromId(value: String): PlyEnum? = values().firstOrNull { it.id == value}
    }

    override fun toString(): String {
        return id
    }
}