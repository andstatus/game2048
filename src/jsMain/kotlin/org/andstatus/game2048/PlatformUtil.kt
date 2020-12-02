package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korge.view.Stage
import com.soywiz.korma.geom.SizeInt
import kotlin.coroutines.CoroutineContext

const val platformSourceFolder = "jsMain"

actual val Stage.gameWindowSize: SizeInt get() = defaultGameWindowSize

actual val CoroutineContext.isDarkThemeOn: Boolean get() = false

actual val defaultLanguage: String get() = ""

actual fun Stage.shareText(actionTitle: String, fileName: String, value: String) {
    Console.log("$platformSourceFolder, shareText '$fileName' (${value.length} bytes):\n${value}")
}

actual fun Stage.loadJsonGameRecord(consumer: (String) -> Unit) {
    Console.log("$platformSourceFolder, loadJsonGameRecord")
    consumer("")
}

actual fun Stage.closeGameApp() {}