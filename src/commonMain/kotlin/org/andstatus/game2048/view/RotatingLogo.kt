package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.presenter.Block

class RotatingLogo(val gameView: GameView, val buttonSize: Double): Container() {
    var piece: Piece = Piece.N2048

    init {
        setBlock()
        with(gameView) {
            this@RotatingLogo.customOnClick {
                piece = piece.next()
                setBlock()
                gameView.presenter.showControls()
            }
        }
    }

    private fun setBlock() {
        this.removeChildren()
        Block(piece, gameView, buttonSize).addTo(this)
    }
}