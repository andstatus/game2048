package org.andstatus.game2048

import com.soywiz.korim.font.BitmapFont
import kotlin.properties.Delegates

var font: BitmapFont by Delegates.notNull()

var boardWidth: Double = 0.0
var cellSize: Double = 0.0
var leftIndent: Double = 0.0
var topIndent: Double = 0.0

fun Square.positionX() = leftIndent + 10 + (cellSize + 10) * x
fun Square.positionY() = topIndent + 10 + (cellSize + 10) * y
