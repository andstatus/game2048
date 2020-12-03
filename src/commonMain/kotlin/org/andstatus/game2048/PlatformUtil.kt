package org.andstatus.game2048

import com.soywiz.korge.view.Stage
import com.soywiz.korma.geom.SizeInt
import kotlin.coroutines.CoroutineContext

expect val CoroutineContext.gameWindowSize: SizeInt

expect val CoroutineContext.isDarkThemeOn: Boolean

expect val defaultLanguage: String

expect fun Stage.shareText(actionTitle: String, fileName: String, value: String)

expect fun Stage.loadJsonGameRecord(consumer: (String) -> Unit)

expect fun Stage.closeGameApp()
