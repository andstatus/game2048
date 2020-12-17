package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korge.view.Stage
import com.soywiz.korma.geom.SizeInt
import kotlin.coroutines.CoroutineContext

const val platformSourceFolder = "jvmMain"

actual val CoroutineContext.gameWindowSize: SizeInt get() =
    when (System.getProperty("user.screen.orientation")) {
        "landscape" -> SizeInt(defaultPortraitGameWindowSize.height, defaultPortraitGameWindowSize.width)
        "tall" -> SizeInt(defaultDesktopGameWindowSize.width, defaultDesktopGameWindowSize.height + 64)
        else -> defaultDesktopGameWindowSize
    }

actual val CoroutineContext.isDarkThemeOn: Boolean get() = System.getProperty("user.color.theme") == "dark"

actual val defaultLanguage: String get() = java.util.Locale.getDefault().language

actual fun Stage.shareText(actionTitle: String, fileName: String, value: String) {
    Console.log("$platformSourceFolder, shareText '$fileName' (${value.length} bytes):\n${value}")
}

actual fun Stage.loadJsonGameRecord(consumer: (String) -> Unit) {
    Console.log("$platformSourceFolder, loadJsonGameRecord")
    consumer("")
}

actual fun Stage.closeGameApp() {}
