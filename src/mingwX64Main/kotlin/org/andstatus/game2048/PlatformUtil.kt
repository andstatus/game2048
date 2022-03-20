package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.korge.view.Stage
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korma.geom.SizeInt
import com.soywiz.korma.geom.times
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
