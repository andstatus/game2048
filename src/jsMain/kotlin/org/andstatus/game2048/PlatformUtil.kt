package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log

actual fun shareText(actionTitle: String, fileName: String, value: String) {
  Console.log("jsMain, shareText '$fileName' (${value.length} bytes):\n${value}")
}