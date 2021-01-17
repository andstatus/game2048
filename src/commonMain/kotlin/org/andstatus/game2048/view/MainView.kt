package org.andstatus.game2048.view

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.addTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.andstatus.game2048.view.AppBar.Companion.setupAppBar

class MainView private constructor(
    val appBar: AppBar,
    val scoreBar: ScoreBar,
    val boardView: BoardView) : Container() {

    fun show(appBarButtonsToShow: List<AppBarButtonsEnum>, playSpeed: Int) {
        appBar.show(this, appBarButtonsToShow)
        scoreBar.show(this, playSpeed)
        boardView.setOnTop(this)
        this.addTo(appBar.viewData.gameStage)
    }

    companion object {
        suspend fun ViewData.setupMainView(coroutineScope: CoroutineScope): MainView {
            with(coroutineScope) {
                val appBar = async { setupAppBar() }
                val scoreBar = async { setupScoreBar() }
                val boardView = async { BoardView(this@setupMainView) }

                return MainView(appBar.await(), scoreBar.await(), boardView.await())
            }
        }
    }
}