package org.andstatus.game2048.presenter

import org.andstatus.game2048.model.Board
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.view.ViewData

class BoardViews(val viewData: ViewData, val blocks: MutableList<PlacedBlock> = ArrayList()) {

    val blocksOnBoard: List<List<Block>>
        get() = viewData.settings.squares.array
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
        blocks.add(PlacedBlock(block, placedPiece.square))
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

    fun copy() = BoardViews(viewData, blocks)

    fun addBlock(destination: PlacedPiece): Block = Block(destination.piece, viewData)
            .addTo(viewData.mainView.boardView, destination.square)
            .also { set(destination, it) }

    fun removeBlock(block: Block): Block? =
            blocks.find { it.block == block }?.also {
                blocks.remove(it)
                it.block.removeFromParent()
            }?.block

}
