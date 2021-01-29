package org.andstatus.game2048.view

import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import com.soywiz.korge.view.position
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.launch
import org.andstatus.game2048.Settings
import org.andstatus.game2048.ai.AiAlgorithm

fun ViewData.selectAiAlgorithm(settings: Settings) = myWindow("select_ai_algorithm") {
    var selected = settings.aiAlgorithm
    var buttons: List<Container> = emptyList()

    suspend fun button(buttonEnum: AiAlgorithm, yInd: Int, handler: (AiAlgorithm) -> Unit): Container =
        wideButton(if (buttonEnum == selected) "radio_button_checked" else "radio_button_unchecked", buttonEnum.labelKey) {
            handler(buttonEnum)
        }.apply {
            position(buttonXs[0], buttonYs[yInd])
            addTo(window)
        }


    suspend fun showOptions( handler: (AiAlgorithm) -> Unit) {
        var yInd: Int = 0
        val oldButtons = buttons
        buttons = listOf(
            button(AiAlgorithm.RANDOM, ++yInd, handler),
            button(AiAlgorithm.MAX_SCORE_OF_ONE_MOVE, ++yInd, handler),
            button(AiAlgorithm.MAX_EMPTY_BLOCKS_OF_N_MOVES, ++yInd, handler),
            button(AiAlgorithm.MAX_SCORE_OF_N_MOVES, ++yInd, handler),
            button(AiAlgorithm.LONGEST_RANDOM_PLAY, ++yInd, handler),
        )
        oldButtons.forEach{ b -> b.removeFromParent() }
    }

    fun onSelected(algorithm: AiAlgorithm) {
        selected = algorithm
        gameStage.launch {
            showOptions {}
            delay(100.milliseconds)
            presenter.onSelectAiAlgorithm(selected)
            window.removeFromParent()
        }
    }

    showOptions(::onSelected)
}
