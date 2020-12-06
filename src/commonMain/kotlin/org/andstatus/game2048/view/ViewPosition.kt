package org.andstatus.game2048.view

import org.andstatus.game2048.model.Square

var cellMargin: Double = 0.0
var boardWidth: Double = 0.0
var cellSize: Double = 0.0
var boardLeft: Double = 0.0
var boardTop: Double = 0.0
var buttonRadius = 0.0

fun Square.positionX() = cellMargin + (cellSize + cellMargin) * x
fun Square.positionY() = cellMargin + (cellSize + cellMargin) * y
