package org.andstatus.game2048.view

import korlibs.io.async.launch
import korlibs.korge.time.delay
import korlibs.korge.view.Container
import korlibs.korge.view.addTo
import korlibs.korge.view.position
import korlibs.time.milliseconds
import org.andstatus.game2048.Settings
import org.andstatus.game2048.ai.AiAlgorithm

fun ViewData.selectAiAlgorithm(settings: Settings) = myWindow("select_ai_algorithm") {
    var selected = settings.aiAlgorithm
    var buttons: List<Container> = emptyList()
    var xInd = 0
    var yInd = 0

    suspend fun button(buttonEnum: AiAlgorithm, handler: (AiAlgorithm) -> Unit): Container {
        if (isPortrait) {
            yInd++
            if (yInd > 8) return Container()
        } else {
            yInd++
            if (yInd > 4 && xInd == 0) {
                xInd = 5
                yInd = 1
            }
            if (yInd > 4) return Container()
        }

        return wideButton(
            if (buttonEnum == selected) "radio_button_checked" else "radio_button_unchecked",
            buttonEnum.labelKey
        ) {
            handler(buttonEnum)
        }.apply {
            position(buttonXs[xInd], buttonYs[yInd])
            addTo(window)
        }
    }


    suspend fun showOptions(handler: (AiAlgorithm) -> Unit) {
        val oldButtons = buttons
        buttons = listOf(
            button(AiAlgorithm.RANDOM, handler),
            button(AiAlgorithm.MAX_SCORE_OF_ONE_MOVE, handler),
            button(AiAlgorithm.MAX_EMPTY_BLOCKS_OF_N_MOVES, handler),
            button(AiAlgorithm.MAX_SCORE_OF_N_MOVES, handler),
            button(AiAlgorithm.LONGEST_RANDOM_PLAY, handler),
        )
        oldButtons.forEach { b -> b.removeFromParent() }
    }

    fun onSelected(algorithm: AiAlgorithm) {
        selected = algorithm
        korgeCoroutineScope.launch {
            showOptions {}
            delay(100.milliseconds)
            presenter.onAiAlgorithmSelect(selected)
            window.removeFromParent()
        }
    }

    showOptions(::onSelected)
}
