package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.korge.view.Stage
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korma.geom.SizeInt
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

actual fun Stage.shareText(actionTitle: String, fileName: String, value: Sequence<String>) =
    shareTextCommon(actionTitle, fileName, value)

actual fun Stage.loadJsonGameRecord(sharedJsonHandler: (Sequence<String>) -> Unit) {
    Console.log("$platformSourceFolder, loadJsonGameRecord")
    sharedJsonHandler(emptySequence())
}

actual fun Stage.closeGameApp() {}

actual fun <T> initAtomicReference(initial: T): KorAtomicRef<T> = korAtomic(initial)

actual fun <T> KorAtomicRef<T>.compareAndSetFixed(expect: T, update: T): Boolean = compareAndSet(expect, update)
