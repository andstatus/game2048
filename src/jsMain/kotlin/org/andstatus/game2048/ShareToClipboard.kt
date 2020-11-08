package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log

actual fun shareToClipboard(value: String): Unit {
  Console.log("jsMain, shareToClipboard: $value")
}