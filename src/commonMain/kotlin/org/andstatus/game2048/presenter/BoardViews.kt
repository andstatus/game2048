package org.andstatus.game2048.presenter

import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.view.ViewData

data class PlacedBlock(val block: Block, val square: Square)

class BoardViews(
    val viewData: ViewData,
    val width: Int = viewData.settings.boardWidth, val height: Int = viewData.settings.boardHeight,
    val blocks: MutableList<PlacedBlock> = ArrayList()
) {
    private val size = width * height

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
        return viewData.settings.squares.toSquare(x, (this - x) / width)
    }

    fun load(board: Board) {
        hideGameOver()
        blocks.forEach { it.block.removeFromParent() }
        blocks.clear()

        board.pieces().forEach {
            addBlock(it)
        }
    }

    fun hideGameOver(): BoardViews {
        viewData.mainView.boardView.hideGameOver()
        return this
    }

    fun copy() = BoardViews(viewData, width, height, blocks)

    fun addBlock(destination: PlacedPiece): Block = Block(destination.piece, viewData)
            .addTo(viewData.mainView.boardView, destination.square)
            .also { set(destination, it) }

    fun removeBlock(block: Block): Block? =
            blocks.find { it.block == block }?.also {
                blocks.remove(it)
                it.block.removeFromParent()
            }?.block

}
