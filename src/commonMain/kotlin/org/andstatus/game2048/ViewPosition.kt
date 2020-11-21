package org.andstatus.game2048

var cellMargin: Double = 0.0
var boardWidth: Double = 0.0
var cellSize: Double = 0.0
var boardLeft: Double = 0.0
var boardTop: Double = 0.0
var buttonRadius = 0.0

fun Square.positionX() = boardLeft + cellMargin + (cellSize + cellMargin) * x
fun Square.positionY() = boardTop + cellMargin + (cellSize + cellMargin) * y
