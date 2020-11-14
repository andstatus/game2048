package org.andstatus.game2048

expect val defaultLanguage: String

expect fun shareText(actionTitle: String, fileName: String, value: String)

expect fun loadJsonGameRecord(consumer: (String) -> Unit)
