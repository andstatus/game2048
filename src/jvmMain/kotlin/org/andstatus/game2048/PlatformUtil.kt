package org.andstatus.game2048

import korlibs.io.concurrent.atomic.KorAtomicRef
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.korge.view.Stage
import korlibs.logger.Console
import korlibs.math.geom.SizeInt
import org.andstatus.game2048.presenter.Presenter
import java.io.File
import java.io.OutputStreamWriter
import kotlin.coroutines.CoroutineContext

private const val platformSourceFolder = "jvmMain"

actual val CoroutineContext.gameWindowSize: SizeInt
    get() =
        when (System.getProperty("user.screen.orientation")) {
            "landscape" -> SizeInt(defaultPortraitGameWindowSize.height, defaultPortraitGameWindowSize.width)
            "tall" -> SizeInt(defaultDesktopGameWindowSize.width, defaultDesktopGameWindowSize.height + 64)
            else -> defaultDesktopGameWindowSize
        }

actual val CoroutineContext.isDarkThemeOn: Boolean get() = System.getProperty("user.color.theme") == "dark"

actual val defaultLanguage: String get() = java.util.Locale.getDefault().language

actual fun Presenter.shareText(actionTitle: String, fileName: String, value: Sequence<String>) {
    for (attempt in (0..100)) {
        val fileNameNew = if (attempt == 0) fileName else fileName.appendToFileName(" ($attempt)")
        val file = File(fileNameNew)
        if (file.exists()) {
            myLog("File already exists: '${file.absolutePath}'")
        } else {
            if (file.createNewFile()) {
                saveToFile(file, value)
                myLog("Saved: '${file.absolutePath}'")
                asyncShowMainView()
                return
            }
            myLog("Cannot create file: '${file.absolutePath}'")
            break
        }
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
    val fileName = if (isTestRun.value) {
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

actual fun Stage.exitApp() {
    System.exit(0)
}

actual fun <T> initAtomicReference(initial: T): KorAtomicRef<T> = korAtomic(initial)

actual fun <T> KorAtomicRef<T>.compareAndSetFixed(expect: T, update: T): Boolean = compareAndSet(expect, update)
