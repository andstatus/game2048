package org.andstatus.game2048

import com.soywiz.korge.view.Container

data class PlacedBlock(val block: Block, val square: Square)

class BoardViews(val gameView: GameView, val width: Int = 4, val height: Int = 4,
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
        removeGameOver()
        blocks.forEach { it.block.removeFromParent() }
        blocks.clear()

        board.pieces().forEach {
            addBlock(it)
        }
    }

    fun removeGameOver(): BoardViews {
        gameOver?.removeFromParent()
        gameOver = null
        return this
    }

    fun copy() = BoardViews(gameView, width, height, blocks).apply {
        gameOver = this@BoardViews.gameOver
    }

    fun addBlock(destination: PlacedPiece): Block = Block(destination.piece, gameView)
            .addTo(gameView.gameStage, destination.square)
            .also { set(destination, it) }

    fun removeBlock(block: Block): Block? =
            blocks.find { it.block == block }?.also {
                blocks.remove(it)
                it.block.removeFromParent()
            }?.block

}
