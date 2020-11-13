package org.andstatus.game2048

import com.soywiz.korge.Korge
import com.soywiz.korim.color.Colors

suspend fun main() {
    val stringResources = StringResources.load(defaultLanguage)
    Korge(width = 480, height = 680, title = stringResources.text("app_name"),
            gameId = "org.andstatus.game2048",
            bgcolor = Colors["#fdf7f0"]) {
        GameView.appEntry(this, stringResources, true)
    }
}