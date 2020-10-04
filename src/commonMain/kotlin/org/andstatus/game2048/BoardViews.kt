package org.andstatus.game2048

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage

class BoardViews(val stage: Stage, val width: Int = 4, val height: Int = 4,
            private val array: Array<Block?> = Array(width * height) { null }) {
    private val size = width * height
    var gameOver: Container? = null

    operator fun get(square: Square): Block? = square.toInd()?.let { array[it] }

    operator fun set(square: Square, value: Block?) {
        square.toInd()?.let { array[it] = value }
    }

    private fun Square.toInd(): Int? {
        return if (x < 0 || y < 0 || x >= width || y >= height)
            null
        else x + y * width
    }

    fun load(board: Board) {
        board.pieces().forEach {
            set(it.square, Block(it.piece).addTo(stage, it.square))
        }
    }

    fun removeFromParent() {
        gameOver?.removeFromParent()
        array.forEach { block -> block?.removeFromParent() }
    }

    fun copy() = BoardViews(stage, width, height, array.copyOf()).apply {
        gameOver = this@BoardViews.gameOver
    }

    fun addBlock(destination: PlacedPiece): Block =
        Block(destination.piece).addTo(stage, destination.square).also{
            set(destination.square, it)
        }

    fun removeBlock(square: Square) {
        get(square)?.let{
            set(square, null)
            it.removeFromParent()
        }
    }

}
