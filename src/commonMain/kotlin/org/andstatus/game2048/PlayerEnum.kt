package org.andstatus.game2048

enum class PlayerEnum(val id: String) {
    SWIPER("swiper"), SETTER("setter");

    companion object {
        fun fromId(value: String): PlayerEnum? = PlayerEnum.values().firstOrNull { it.id == value}
    }
}