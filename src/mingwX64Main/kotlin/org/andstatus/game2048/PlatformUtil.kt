package org.andstatus.game2048

import korlibs.logger.Console
import korlibs.korge.view.Stage
import korlibs.io.concurrent.atomic.KorAtomicRef
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.math.geom.SizeInt
import korlibs.math.geom.times
import org.andstatus.game2048.presenter.Presenter
import platform.posix.exit
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.freeze

private const val platformSourceFolder = "mingwX64Main"

actual val CoroutineContext.gameWindowSize: SizeInt get() = defaultPortraitGameWindowSize * 0.66

actual val CoroutineContext.isDarkThemeOn: Boolean get() = false

actual val defaultLanguage: String get() = ""

actual fun Presenter.shareText(actionTitle: String, fileName: String, value: Sequence<String>) =
    shareTextCommon(actionTitle, fileName, value)

actual fun Stage.loadJsonGameRecord(settings: Settings, sharedJsonHandler: (Sequence<String>) -> Unit) {
    Console.log("$platformSourceFolder, loadJsonGameRecord")
    sharedJsonHandler(emptySequence())
}

actual fun Stage.exitApp() {
    exit(0)
}

actual fun <T> initAtomicReference(initial: T): KorAtomicRef<T> = korAtomic(initial.freeze())

actual fun <T> KorAtomicRef<T>.compareAndSetFixed(expect: T, update: T): Boolean =
    compareAndSet(expect, update.freeze())
