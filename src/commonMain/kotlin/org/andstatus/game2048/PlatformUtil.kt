package org.andstatus.game2048

import com.soywiz.korma.geom.SizeInt

expect val gameWindowSize: SizeInt

expect val isDarkThemeOn: Boolean

expect val defaultLanguage: String

expect fun shareText(actionTitle: String, fileName: String, value: String)

expect fun loadJsonGameRecord(consumer: (String) -> Unit)
