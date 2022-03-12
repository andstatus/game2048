package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.korge.view.Stage
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korma.geom.SizeInt
import java.io.File
import java.io.OutputStreamWriter
import kotlin.coroutines.CoroutineContext

private const val platformSourceFolder = "jvmMain"

actual val CoroutineContext.gameWindowSize: SizeInt get() =
    when (System.getProperty("user.screen.orientation")) {
        "landscape" -> SizeInt(defaultPortraitGameWindowSize.height, defaultPortraitGameWindowSize.width)
        "tall" -> SizeInt(defaultDesktopGameWindowSize.width, defaultDesktopGameWindowSize.height + 64)
        else -> defaultDesktopGameWindowSize
    }

actual val CoroutineContext.isDarkThemeOn: Boolean get() = System.getProperty("user.color.theme") == "dark"

actual val defaultLanguage: String get() = java.util.Locale.getDefault().language

actual fun Stage.shareText(actionTitle: String, fileName: String, value: Sequence<String>) {
    val file = File(fileName)
    if (file.exists()) {
        myLog("File already exists: '${file.absolutePath}'")
    } else {
        if (file.createNewFile()) {
            saveToFile(file, value)
            return
        }
        myLog("Cannot create file: '${file.absolutePath}'")
    }
    shareTextCommon(actionTitle, fileName, value)
}

fun saveToFile(file: File, value: Sequence<String>) {
    file.outputStream().use { outputStream ->
        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            value.forEach {
                writer.write(it)
                writer.write("\n")
            }
        }
    }
}

actual fun Stage.loadJsonGameRecord(settings: Settings, sharedJsonHandler: (Sequence<String>) -> Unit) {
    val fileName = if (settings.isTestRun) {
        "./src/commonTest/resources/sharedGames/game1.json"
    } else findSharedGameToLoad()
    Console.log("$platformSourceFolder, loadJsonGameRecord from file: $fileName")
    if (!fileName.isNullOrBlank()) {
        sharedJsonHandler(FileSequence(fileName))
    }
}

private fun findSharedGameToLoad(): String? {
    val folder = File(".")
    return folder
        .listFiles()
        ?.filter { it.isFile }
        ?.filter { it.name.endsWith(".json") }
        ?.firstOrNull()
        .also { file ->
            if (file == null) {
                myLog("Couldn't find any shared Game in '${folder.absolutePath}'")
            } else {
                myLog("Found shared Game file '${file.absolutePath}'")
            }
        }
        ?.absolutePath
}

actual fun Stage.closeGameApp() {}

actual fun <T> initAtomicReference(initial: T): KorAtomicRef<T> = korAtomic(initial)

actual fun <T> KorAtomicRef<T>.compareAndSetFixed(expect: T, update: T): Boolean = compareAndSet(expect, update)
