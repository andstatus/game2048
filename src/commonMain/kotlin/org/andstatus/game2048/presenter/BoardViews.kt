package org.andstatus.game2048.presenter

import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import org.andstatus.game2048.initAtomicReference
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.view.ViewData

/** @author yvolk@yurivolkov.com */
class BoardViews(val viewData: ViewData, blocksIn: List<PlacedBlock> = emptyList()) {
    private val blocksRef: KorAtomicRef<List<PlacedBlock>> = initAtomicReference(blocksIn)
    val blocks get() = blocksRef.value

    val blocksOnBoard: List<List<Block>>
        get() = viewData.presenter.model.gamePosition.board.array
            .map { square -> blocks.filter { it.square == square} }
            .map { it.map { it.block } }

    fun getAll(square: Square): List<Block> = blocks.filter { it.square == square }.map { it.block }

    operator fun get(placedPiece: PlacedPiece): Block? = blocks.filter {
        it.block.piece == placedPiece.piece && it.square == placedPiece.square }
            .map { it.block }
            .firstOrNull()

    fun add(placedPiece: PlacedPiece, block: Block) {
        modify({ blocks1 -> blocks1 + PlacedBlock(block, placedPiece.square) }, true)
    }

    fun move(placedPiece: PlacedPiece, block: Block) {
        modify({ blocks1 -> blocks1
            .filter { it.block != block } + PlacedBlock(block, placedPiece.square) }, false)
    }

    fun load(position: GamePosition) {
        hideGameOver()
        modify({ _ -> emptyList() }, true)
        position.placedPieces().forEach {
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
            .also { add(destination, it) }

    fun removeBlock(block: Block): Block? =
        blocks.find { it.block == block }
            ?.also {
                modify({ blocks1 -> blocks1.filter { it.block != block } }, true)
            }?.block


    private fun modify(action: (List<PlacedBlock>) -> List<PlacedBlock>, removeFromParent: Boolean) {
        do {
            val blocks1 = blocks
            val blocks2 = action(blocks1)
            val success = blocksRef.compareAndSet(blocks1, blocks2)
            if (success && removeFromParent) {
                blocks1.forEach {
                    if (!blocks2.contains(it)) it.block.removeFromParent()
                }
            }
        } while (!success)
    }
}
