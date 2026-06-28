package org.andstatus.game2048

import korlibs.korge.view.Stage
import korlibs.math.geom.SizeInt
import kotlinx.atomicfu.AtomicRef
import org.andstatus.game2048.presenter.Presenter
import kotlin.coroutines.CoroutineContext

actual val CoroutineContext.gameWindowSize: SizeInt
    get() = TODO("Not yet implemented")
actual val CoroutineContext.isDarkThemeOn: Boolean
    get() = TODO("Not yet implemented")
actual val defaultLanguage: String
    get() = TODO("Not yet implemented")

actual fun Presenter.shareText(
    actionTitle: String,
    fileName: String,
    value: Sequence<String>
) {
}

actual fun Stage.loadJsonGameRecord(
    myContext: MyContext,
    sharedJsonHandler: (Sequence<String>) -> Unit
) {
}

actual fun Stage.exitApp() {
}

actual fun <T> initAtomicReference(initial: T): AtomicRef<T> {
    TODO("Not yet implemented")
}

actual fun <T> AtomicRef<T>.compareAndSetFixed(expect: T, update: T): Boolean {
    TODO("Not yet implemented")
}
