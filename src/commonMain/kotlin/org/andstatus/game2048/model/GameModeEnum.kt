package org.andstatus.game2048.model

enum class GameModeEnum(val id: String) {
    BACKWARDS("backwards"),
    FORWARD("forward"),
    STOP("stop"),
    PLAY("play");

    companion object {
        fun fromId(value: String): GameModeEnum = values().firstOrNull { it.id == value} ?: PLAY
    }
}