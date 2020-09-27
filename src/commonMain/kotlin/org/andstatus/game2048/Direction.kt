package org.andstatus.game2048

enum class Direction {
    LEFT, RIGHT, UP, DOWN;

    fun reverse(): Direction = when(this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
        UP -> DOWN
        DOWN -> UP
    }
}