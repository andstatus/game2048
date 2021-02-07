package org.andstatus.game2048.model

enum class PieceMoveEnum(val id: String) {
    ONE("one"),
    MERGE("merge"),
    PLACE("place"),
    LOAD("load"),
    DELAY("delay");

    companion object {
        fun fromId(value: String): PieceMoveEnum? = values().firstOrNull { it.id == value}
    }
}