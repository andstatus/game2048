package org.andstatus.game2048

import com.soywiz.korge.view.Stage
import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korma.geom.SizeInt
import kotlin.coroutines.CoroutineContext

expect val CoroutineContext.gameWindowSize: SizeInt

expect val CoroutineContext.isDarkThemeOn: Boolean

expect val defaultLanguage: String

expect fun Stage.shareText(actionTitle: String, fileName: String, value: Sequence<String>)

expect fun Stage.loadJsonGameRecord(sharedJsonHandler: (Sequence<String>) -> Unit)

expect fun Stage.closeGameApp()

expect fun <T> initAtomicReference(initial: T): KorAtomicRef<T>

expect fun <T> KorAtomicRef<T>.compareAndSetFixed(expect: T, update: T): Boolean
