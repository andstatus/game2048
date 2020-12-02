package org.andstatus.game2048

import com.soywiz.korio.lang.currentThreadId

fun myLog(message: Any?) = println("game2048.log [${currentThreadId}] $message")