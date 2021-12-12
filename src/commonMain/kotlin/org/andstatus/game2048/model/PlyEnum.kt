package org.andstatus.game2048.model

import com.soywiz.korge.input.SwipeDirection

enum class PlyEnum(val id: String, val symbol: String, val swipeDirection: SwipeDirection?) {
    LEFT("left", "←", SwipeDirection.LEFT),
    RIGHT("right", "→", SwipeDirection.RIGHT),
    UP("up", "↑", SwipeDirection.TOP),
    DOWN("down", "↓", SwipeDirection.BOTTOM),
    PLACE("place", "", null),
    LOAD("load", "", null),
    DELAY("delay", "", null),
    EMPTY("empty", "", null),
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