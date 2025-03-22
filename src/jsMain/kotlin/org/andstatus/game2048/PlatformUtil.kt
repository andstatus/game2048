package org.andstatus.game2048

import korlibs.io.concurrent.atomic.KorAtomicRef
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.korge.view.Stage
import korlibs.logger.Console
import korlibs.math.geom.SizeInt
import org.andstatus.game2048.presenter.Presenter
import kotlin.coroutines.CoroutineContext

private const val platformSourceFolder = "jsMain"

actual val CoroutineContext.gameWindowSize: SizeInt get() = defaultDesktopGameWindowSize

actual val CoroutineContext.isDarkThemeOn: Boolean get() = false

actual val defaultLanguage: String get() = ""

actual fun Presenter.shareText(actionTitle: String, fileName: String, value: Sequence<String>) =
    shareTextCommon(actionTitle, fileName, value)

actual fun Stage.loadJsonGameRecord(myContext: MyContext, sharedJsonHandler: (Sequence<String>) -> Unit) {
    Console.log("$platformSourceFolder, loadJsonGameRecord")
    sharedJsonHandler(emptySequence())
}

actual fun Stage.exitApp() {}

actual fun <T> initAtomicReference(initial: T): KorAtomicRef<T> = korAtomic(initial)

actual fun <T> KorAtomicRef<T>.compareAndSetFixed(expect: T, update: T): Boolean = compareAndSet(expect, update)
