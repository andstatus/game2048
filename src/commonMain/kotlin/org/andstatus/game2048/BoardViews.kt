package org.andstatus.game2048

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage

data class PlacedBlock(val block: Block, val square: Square)

class BoardViews(val stage: Stage, val width: Int = 4, val height: Int = 4,
            val blocks: MutableList<PlacedBlock> = ArrayList()) {
    private val size = width * height
    var gameOver: Container? = null

    val blocksOnBoard: List<List<Block>>
        get() = IntRange(0, width * height - 1).map { it.toSquare() }
                .map { square -> blocks.filter { it.square == square} }
                .map { it.map { it.block } }

    fun getAll(square: Square): List<Block> = blocks.filter { it.square == square }.map { it.block }

    operator fun get(placedPiece: PlacedPiece): Block? = blocks.filter {
        it.block.piece == placedPiece.piece && it.square == placedPiece.square }
            .map { it.block }
            .firstOrNull()

    operator fun set(placedPiece: PlacedPiece, block: Block) {
        blocks.find { it.block == block }?.also {
            blocks.remove(it)
        }
        placedPiece.square.toInd()?.also {
            blocks.add(PlacedBlock(block, placedPiece.square))
        }
    }

    private fun Square.toInd(): Int? {
        return if (x < 0 || y < 0 || x >= width || y >= height)
            null
        else x + y * width
    }

    private fun Int.toSquare(): Square? {
        if (this < 0 || this >= size) return null
        val x: Int = this % width
        return Square(x, (this - x) / width)
    }

    fun load(board: Board) {
        gameOver?.removeFromParent()
        gameOver = null
        blocks.forEach { it.block.removeFromParent() }
        blocks.clear()

        board.pieces().forEach {
            set(it, Block(it.piece).addTo(stage, it.square))
        }
    }

    fun copy() = BoardViews(stage, width, height, blocks).apply {
        gameOver = this@BoardViews.gameOver
    }

    fun addBlock(destination: PlacedPiece): Block = Block(destination.piece)
            .addTo(stage, destination.square)
            .also { set(destination, it) }

    fun removeBlock(block: Block): Block? =
            blocks.find { it.block == block }?.also {
                blocks.remove(it)
                it.block.removeFromParent()
            }?.block

}
