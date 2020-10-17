package org.andstatus.game2048

enum class PlayersMoveEnum(val id: String) {
    LEFT("left"), RIGHT("right"), UP("up"), DOWN("down"), PLACE("place");

    fun reverseDirection(): Direction = when(this) {
        LEFT -> Direction.RIGHT
        RIGHT -> Direction.LEFT
        UP -> Direction.DOWN
        DOWN -> Direction.UP
        PLACE -> Direction.LEFT
    }

    companion object {
        fun fromId(value: String): PlayersMoveEnum? = values().firstOrNull { it.id == value}
    }
}