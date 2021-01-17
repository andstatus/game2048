package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.presenter.Block

class RotatingLogo(val viewData: ViewData, val buttonSize: Double): Container() {
    var piece: Piece = Piece.N2048

    init {
        setBlock()
        with(viewData) {
            this@RotatingLogo.customOnClick {
                piece = piece.next()
                setBlock()
                viewData.presenter.showMainView()
            }
        }
    }

    private fun setBlock() {
        this.removeChildren()
        Block(piece, viewData, buttonSize).addTo(this)
    }
}