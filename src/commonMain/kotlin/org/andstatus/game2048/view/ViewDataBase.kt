package org.andstatus.game2048.view

import com.soywiz.korge.view.Stage
import com.soywiz.korio.concurrent.atomic.incrementAndGet
import com.soywiz.korio.concurrent.atomic.korAtomic
import org.andstatus.game2048.defaultPortraitGameWindowSize
import org.andstatus.game2048.defaultPortraitRatio
import org.andstatus.game2048.defaultPortraitTextSize
import org.andstatus.game2048.gameWindowSize
import org.andstatus.game2048.myLog

/** @author yvolk@yurivolkov.com */
interface ViewDataBase {
    val gameStage: Stage
    val animateViews: Boolean
    val id: Int
    val duplicateKeyPressFilter: DuplicateKeyPressFilter
    val isPortrait: Boolean
    val gameViewLeft: Int
    val gameViewTop: Int
    val gameViewWidth: Int
    val gameViewHeight: Int
    val gameScale: Double
    val defaultTextSize: Double
    val buttonMargin: Double
    val cellMargin: Double
    val buttonRadius: Double
    val buttonSize : Double
    val boardLeft: Double
    val buttonXs: List<Double>
    val buttonYs: List<Double>
    val boardTop: Double
    val statusBarLeft: Double
    val statusBarTop: Double
}

/** The object is initialized instantly */
class ViewDataQuick(override val gameStage: Stage, override val animateViews: Boolean = true) : ViewDataBase {
    override val id: Int = nextId()
    override val duplicateKeyPressFilter = DuplicateKeyPressFilter()

    override val isPortrait: Boolean
    override val gameViewLeft: Int
    override val gameViewTop: Int
    override val gameViewWidth: Int
    override val gameViewHeight: Int

    override val gameScale: Double
    override val defaultTextSize: Double
    override val buttonMargin: Double
    override val cellMargin: Double
    override val buttonRadius: Double
    override val buttonSize : Double
    override val boardLeft: Double
    override val buttonXs: List<Double>
    override val buttonYs: List<Double>
    override val boardTop: Double
    override val statusBarLeft: Double
    override val statusBarTop: Double

    init {
        isPortrait = gameStage.views.virtualWidth < gameStage.views.virtualHeight
        if (isPortrait) {
            val windowRatio = gameStage.views.virtualWidth.toDouble() /  gameStage.views.virtualHeight
            if (windowRatio >= defaultPortraitRatio) {
                gameViewHeight = gameStage.views.virtualHeight
                gameViewWidth = (gameViewHeight * defaultPortraitRatio).toInt()
                gameViewLeft = (gameStage.views.virtualWidth - gameViewWidth) / 2
                gameViewTop = 0
                gameScale = gameViewHeight.toDouble() / defaultPortraitGameWindowSize.height
            } else {
                gameViewWidth = gameStage.views.virtualWidth
                gameViewHeight = (gameViewWidth / defaultPortraitRatio).toInt()
                gameViewLeft = 0
                gameViewTop = (gameStage.views.virtualHeight - gameViewHeight) / 2
                gameScale = gameViewWidth.toDouble() / defaultPortraitGameWindowSize.width
            }
            defaultTextSize = if (gameViewHeight == defaultPortraitGameWindowSize.height) defaultPortraitTextSize
            else defaultPortraitTextSize * gameViewHeight / defaultPortraitGameWindowSize.height
        } else {
            gameViewHeight = gameStage.views.virtualHeight
            gameViewWidth = (gameViewHeight / defaultPortraitRatio).toInt()
            gameViewLeft = (gameStage.views.virtualWidth - gameViewWidth) / 2
            gameViewTop = 0
            gameScale = gameViewWidth.toDouble() / defaultPortraitGameWindowSize.height

            defaultTextSize = if (gameViewWidth == defaultPortraitGameWindowSize.height) defaultPortraitTextSize
            else defaultPortraitTextSize * gameViewWidth / defaultPortraitGameWindowSize.height
        }

        buttonMargin = 27 * gameScale

        cellMargin = 15 * gameScale
        buttonRadius = 8 * gameScale

        if (isPortrait) {
            buttonSize = (gameViewWidth - buttonMargin * 6) / 5
            boardLeft = gameViewLeft + buttonMargin
        } else {
            buttonSize = (gameViewWidth / 2 - buttonMargin * 6) / 5
            boardLeft = gameViewLeft + gameViewWidth / 2 + buttonMargin
        }
        statusBarLeft = gameViewLeft + buttonMargin

        buttonXs = (0 .. 4).fold(emptyList()) { acc, i ->
            acc + (statusBarLeft + i * (buttonSize + buttonMargin))
        }
        buttonYs = (0 .. 9).fold(emptyList()) { acc, i ->
            acc + (gameViewTop + buttonMargin + i * (buttonSize + buttonMargin))
        }

        if (isPortrait) {
            boardTop = buttonYs[4]
            statusBarTop = buttonYs[9]
        } else {
            boardTop = buttonYs[0]
            statusBarTop = buttonYs[4]
        }

        myLog(
            "Window:${gameStage.coroutineContext.gameWindowSize.width}x${gameStage.coroutineContext.gameWindowSize.height}" +
                    " -> Virtual:${gameStage.views.virtualWidth}x${gameStage.views.virtualHeight}" +
                    " -> Game:${gameViewWidth}x$gameViewHeight, top:$gameViewTop, left:$gameViewLeft"
        )
    }

    companion object {
        private val nextIdHolder = korAtomic(0)
        fun nextId(): Int = nextIdHolder.incrementAndGet()
    }
}
