package org.andstatus.game2048.model

enum class PlayerEnum(val id: String) {
    COMPUTER("computer"),
    USER("user"),
    COMPOSER("composer");

    companion object {
        fun fromId(value: String): PlayerEnum? = values().firstOrNull { it.id == value}
    }
}