package org.andstatus.game2048.view

import com.soywiz.korge.view.Stage
import org.andstatus.game2048.isDarkThemeOn

enum class ColorThemeEnum(val labelKey: String) {
    DEVICE_DEFAULT("device_default"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun deviceDefault(stage: Stage) = if (stage.coroutineContext.isDarkThemeOn) DARK else LIGHT

        fun load(value: String?): ColorThemeEnum = values().firstOrNull { it.labelKey == value } ?: DEVICE_DEFAULT
    }
}