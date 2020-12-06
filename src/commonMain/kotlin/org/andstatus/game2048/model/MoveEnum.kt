package org.andstatus.game2048.model

enum class MoveEnum(val id: String) {
    ONE("one"),
    MERGE("merge"),
    PLACE("place"),
    LOAD("load"),
    DELAY("delay");

    companion object {
        fun fromId(value: String): MoveEnum? = values().firstOrNull { it.id == value}
    }
}