package org.andstatus.game2048.presenter

import korlibs.time.Stopwatch
import korlibs.time.millisecondsLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.andstatus.game2048.ai.AiPlayer
import org.andstatus.game2048.ai.AiPlayer.Companion.aiLaunched
import org.andstatus.game2048.model.GameModeEnum
import org.andstatus.game2048.myLog

/** @author yvolk@yurivolkov.com */
fun CoroutineScope.showAiTip(presenter: Presenter) = launchAi(presenter, "showAiTip") { aiPlayer ->
    with(presenter) {
        myLog("AI showAiTip")
        val gamePosition = model.gamePosition
        aiPlayer.nextPly(gamePosition).also {
            if (model.gamePosition === gamePosition) {
                withContext(view.korgeCoroutineContext) {
                    myLog("Showing AI tip 0: ${it.plyEnum}")
                    view.mainView.showStatusBar(it)
                }
            }
        }
    }
}

fun CoroutineScope.aiPlayLoop(presenter: Presenter, startCount: Int) = launchAi(presenter, "aiPlayLoop") { aiPlayer ->
    with(presenter) {
        while (startCount == clickCounter.value && !model.noMoreMoves()
            && gameMode.modeEnum == GameModeEnum.AI_PLAY
        ) {
            Stopwatch().start().let { stopWatch ->
                presenter.delayWhilePresenting()
                val gamePosition = model.gamePosition
                aiPlayer.nextPly(gamePosition).also { aiResult ->
                    if (gameMode.speed in 1..3) {
                        myLog("Showing AI tip ${gameMode.speed}: ${aiResult.plyEnum}")
                        withContext(view.korgeCoroutineContext) {
                            view.mainView.showStatusBar(aiResult)
                        }
                    }
                    delay(gameMode.delayMs.toLong() - stopWatch.elapsed.millisecondsLong)
                    if (!isPresenting.value && model.gamePosition === gamePosition && gameMode.modeEnum == GameModeEnum.AI_PLAY) {
                        userMove(aiResult.plyEnum)
                    }
                }
            }
        }
        if (gameMode.modeEnum == GameModeEnum.AI_PLAY) {
            withContext(view.korgeCoroutineContext) {
                onAiStopClick()
            }
        }
    }
}

private fun CoroutineScope.launchAi(presenter: Presenter, actionName: String, block: suspend (AiPlayer) -> Unit): Job {
    val aiPlayer = AiPlayer(presenter.model.settings)
    return launch {
        if (aiLaunched.compareAndSet(null, aiPlayer)) {
            try {
                myLog("AI $actionName launched")
                block(aiPlayer)
            } finally {
                myLog("AI $actionName completed")
                aiLaunched.value = null
            }
        } else {
            myLog("AI $actionName skipped: already launched")
        }
    }
}
