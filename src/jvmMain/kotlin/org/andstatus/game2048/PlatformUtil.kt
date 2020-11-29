package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korma.geom.SizeInt

const val platformSourceFolder = "jvmMain"

actual val gameWindowSize: SizeInt get() = defaultGameWindowSize

actual val isDarkThemeOn: Boolean = System.getProperty("user.color.theme") == "dark"

actual val defaultLanguage: String get() = java.util.Locale.getDefault().language

actual fun shareText(actionTitle: String, fileName: String, value: String) {
    Console.log("$platformSourceFolder, shareText '$fileName' (${value.length} bytes):\n${value}")
}

actual fun loadJsonGameRecord(consumer: (String) -> Unit) {
    Console.log("$platformSourceFolder, loadJsonGameRecord")
    consumer("")
}
