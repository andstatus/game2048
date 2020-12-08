package org.andstatus.game2048.view

enum class ColorThemeEnum(val labelKey: String) {
    DEVICE_DEFAULT("device_default"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun load(value: String?): ColorThemeEnum = values().firstOrNull { it.labelKey == value } ?: DEVICE_DEFAULT
    }
}