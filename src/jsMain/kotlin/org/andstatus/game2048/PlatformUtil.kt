package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korma.geom.SizeInt

const val platformSourceFolder = "jsMain"

actual val gameWindowSize: SizeInt get() = defaultGameWindowSize

actual val isDarkThemeOn: Boolean = false

actual val defaultLanguage: String get() = ""

actual fun shareText(actionTitle: String, fileName: String, value: String) {
    Console.log("$platformSourceFolder, shareText '$fileName' (${value.length} bytes):\n${value}")
}

actual fun loadJsonGameRecord(consumer: (String) -> Unit) {
    Console.log("$platformSourceFolder, loadJsonGameRecord")
    consumer("")
}
