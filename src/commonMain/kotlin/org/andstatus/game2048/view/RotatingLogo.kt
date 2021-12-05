package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import org.andstatus.game2048.model.Piece
import org.andstatus.game2048.presenter.Block

fun rotatingLogo(viewData: ViewData, buttonSize: Double): EButton {
    val container = Container()
    var piece: Piece = Piece.N2048

    fun setBlock() {
        container.removeChildren()
        Block(piece, viewData, buttonSize).addTo(container)
    }

    setBlock()
    with(viewData) {
        container.customOnClick {
            piece = piece.next()
            setBlock()
            viewData.presenter.asyncShowMainView()
        }
    }
    return EButton(AppBarButtonsEnum.APP_LOGO, container)
}