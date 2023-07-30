package org.andstatus.game2048.view

import korlibs.image.text.TextAlignment
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.addTo
import korlibs.korge.view.align.centerXOn
import korlibs.korge.view.position
import korlibs.korge.view.positionX
import korlibs.korge.view.positionY
import korlibs.korge.view.roundRect
import korlibs.korge.view.text
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Size
import kotlin.math.abs

fun ViewData.setupScoreBar(): ScoreBar {
    return ScoreBar(this)
}

class ScoreBar(val viewData: ViewData): Container() {
    val gameTime: Text
    val moveNumber: Text
    var score: Text
    val bestScore: Text
    val retries: Text

    init {
        with(viewData) {
            val scoreButtonWidth = (viewData.boardWidth - 2 * buttonMargin) / 3
            val barTopInd = 1
            val textYLabelPadding = 28 * gameScale
            val textYPadding = textYLabelPadding * 0.6
            val scoreLabelSize = buttonSize * 0.53f
            val scoreTextSize = buttonSize * 0.66f

            var posX = statusBarLeft
            val bgScore = roundRect(Size(scoreButtonWidth, buttonSize), RectCorners(buttonRadius), fill = gameColors.buttonBackground) {
                position(posX, buttonYs[barTopInd + 1])
            }
            text(stringResources.text("score_upper"), scoreLabelSize, gameColors.buttonLabelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                positionX(bgScore.pos.x + scoreButtonWidth / 2)
                positionY(buttonYs[barTopInd + 1] + textYLabelPadding)
            }
            score = text("", scoreTextSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
                centerXOn(bgScore)
                positionY(buttonYs[barTopInd + 1] + scoreLabelSize + textYPadding)
            }

            val bgRetries = roundRect(Size(scoreButtonWidth, buttonSize), RectCorners(buttonRadius), fill = gameColors.buttonBackground) {
                position(posX, buttonYs[barTopInd + 2])
            }
            text(stringResources.text("retries"), scoreLabelSize, gameColors.buttonLabelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                positionX(bgRetries.pos.x + scoreButtonWidth / 2)
                positionY(buttonYs[barTopInd + 2] + textYLabelPadding)
            }
            retries = text("", scoreTextSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
                centerXOn(bgRetries)
                positionY(buttonYs[barTopInd + 2] + scoreLabelSize + textYPadding)
            }

            posX += scoreButtonWidth + buttonMargin
            val bgBest = roundRect(Size(scoreButtonWidth, buttonSize), RectCorners(buttonRadius), fill = gameColors.buttonBackground) {
                position(posX, buttonYs[barTopInd + 1])
            }
            text(stringResources.text("best"), scoreLabelSize, gameColors.buttonLabelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                positionX(bgBest.pos.x + scoreButtonWidth / 2)
                positionY(buttonYs[barTopInd + 1] + textYLabelPadding)
            }
            bestScore = text("", scoreTextSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
                centerXOn(bgBest)
                positionY(buttonYs[barTopInd + 1] + scoreLabelSize + textYPadding)
            }

            posX += scoreButtonWidth + buttonMargin
            val bgMove = roundRect(Size(scoreButtonWidth, buttonSize), RectCorners(buttonRadius), fill = gameColors.buttonBackground) {
                position(posX, buttonYs[barTopInd + 1])
                onClick {
                    presenter.onMoveButtonClick()
                }
            }
            text(stringResources.text("move_upper"), scoreLabelSize, gameColors.buttonLabelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                positionX(bgMove.pos.x + scoreButtonWidth / 2)
                positionY(buttonYs[barTopInd + 1] + textYLabelPadding)
            }
            moveNumber = text("", scoreTextSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
                centerXOn(bgMove)
                positionY(buttonYs[barTopInd + 1] + scoreLabelSize + textYPadding)
            }

            val bgTime = roundRect(Size(scoreButtonWidth, buttonSize), RectCorners(buttonRadius), fill = gameColors.buttonBackground) {
                position(posX, buttonYs[barTopInd + 2])
            }
            text(stringResources.text("time"), scoreLabelSize, gameColors.buttonLabelText, font,
                TextAlignment.MIDDLE_CENTER
            ) {
                positionX(bgTime.pos.x + scoreButtonWidth / 2)
                positionY(buttonYs[barTopInd + 2] + textYLabelPadding)
            }
            gameTime = text("00:00:00", scoreLabelSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
                centerXOn(bgTime)
                positionY(buttonYs[barTopInd + 2] + scoreLabelSize + textYPadding)
            }
        }
    }

    fun show(parent: Container, playSpeed: Int) {
        addTo(parent)
        moveNumber.text = viewData.presenter.model.moveNumber.toString() +
            (if (playSpeed < 0) " «" else "") +
            (if (playSpeed > 0) " »" else "") +
            (if (playSpeed != 0) abs(playSpeed) else "")
        bestScore.text = viewData.presenter.bestScore.toString()
        score.text = viewData.presenter.score.toString()
        retries.text = viewData.presenter.retries.toString()
    }
}
