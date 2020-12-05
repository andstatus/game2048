package org.andstatus.game2048

import com.soywiz.korev.Key
import com.soywiz.korev.PauseEvent
import com.soywiz.korev.addEventListener
import com.soywiz.korge.input.*
import com.soywiz.korge.ui.TextFormat
import com.soywiz.korge.ui.TextSkin
import com.soywiz.korge.ui.uiScrollableArea
import com.soywiz.korge.ui.uiText
import com.soywiz.korge.view.*
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korio.async.launch
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.vector.roundRect
import kotlin.math.abs
import kotlin.math.max
import kotlin.properties.Delegates

class GameView(val gameStage: Stage, val stringResources: StringResources, val animateViews: Boolean = true) {
    private val gameViewLeft: Int
    private val gameViewTop: Int
    private val gameViewWidth: Int
    private val gameViewHeight: Int
    private val gameScale: Double

    private val buttonPadding: Double

    private val buttonSize : Double
    var font: Font by Delegates.notNull()
    var gameColors: ColorTheme by Delegates.notNull()

    var presenter: Presenter by Delegates.notNull()

    internal var gameTime: Text by Delegates.notNull()
    private var usersMoveNumber: Text by Delegates.notNull()
    private var score: Text by Delegates.notNull()
    private var bestScore: Text by Delegates.notNull()

    private val pointNONE = Point(0, 0)
    private var buttonPointClicked = pointNONE
    private val buttonXs: List<Double>
    private val buttonYs: List<Double>
    private val duplicateKeyPressFilter = DuplicateKeyPressFilter()

    private class EButton(val enum: AppBarButtonsEnum, val container: Container)
    private infix fun AppBarButtonsEnum.to(container: Container): EButton = EButton(this, container)
    private var appBarButtons: List<EButton> by Delegates.notNull()

    private var boardControls: SolidRect by Delegates.notNull()

    companion object {
        suspend fun initialize(stage: Stage, animateViews: Boolean): GameView {
            loadSettings(stage)
            val stringResources = StringResources.load(defaultLanguage)
            stage.gameWindow.title = stringResources.text("app_name")

            val view = GameView(stage, stringResources, animateViews)
            view.font = resourcesVfs["assets/clear_sans.fnt"].readBitmapFont()
            view.gameColors = ColorTheme.load(stage)
            view.presenter = Presenter(view)
            view.setupStageBackground()
            view.setupAppBar()
            view.setupScoreBar()
            view.boardControls = view.setupBoardControls()

            view.presenter.onAppEntry()
            stage.gameWindow.addEventListener<PauseEvent> { view.presenter.onPauseEvent() }
            return view
        }
    }

    init {
        if (gameStage.views.virtualWidth < gameStage.views.virtualHeight) {
            if ( gameStage.views.virtualHeight / gameStage.views.virtualWidth >
                    defaultPortraitGameWindowSize.height / defaultPortraitGameWindowSize.width) {
                gameViewWidth = gameStage.views.virtualWidth
                gameViewHeight = gameViewWidth * defaultPortraitGameWindowSize.height / defaultPortraitGameWindowSize.width
                gameViewLeft = 0
                gameViewTop = (gameStage.views.virtualHeight - gameViewHeight) / 2
            } else {
                gameViewWidth = gameStage.views.virtualHeight * defaultPortraitGameWindowSize.width / defaultPortraitGameWindowSize.height
                gameViewHeight = gameStage.views.virtualHeight
                gameViewLeft = (gameStage.views.virtualWidth - gameViewWidth) / 2
                gameViewTop = 0
            }
        } else {
            gameViewWidth = gameStage.views.virtualHeight * defaultPortraitGameWindowSize.width / defaultPortraitGameWindowSize.height
            gameViewHeight = gameStage.views.virtualHeight
            gameViewLeft = (gameStage.views.virtualWidth - gameViewWidth) / 2
            gameViewTop = 0
        }
        myLog("Window:${gameStage.coroutineContext.gameWindowSize}" +
                " -> Virtual:${gameStage.views.virtualWidth}x${gameStage.views.virtualHeight}" +
                " -> Game:${gameViewWidth}x$gameViewHeight, top:$gameViewTop, left:$gameViewLeft")

        gameScale = gameViewHeight.toDouble() / defaultPortraitGameWindowSize.height
        buttonPadding = 27 * gameScale

        cellMargin = 15 * gameScale
        buttonRadius = 8 * gameScale

        val allCellMargins = cellMargin * (settings.boardWidth + 1)
        cellSize = (gameViewWidth - allCellMargins - 2 * buttonPadding) / settings.boardWidth
        buttonSize = (gameViewWidth - buttonPadding * 6) / 5
        boardWidth = cellSize * settings.boardWidth + allCellMargins
        boardLeft = gameViewLeft + (gameViewWidth - boardWidth) / 2

        buttonXs = (0 .. 4).fold(emptyList()) { acc, i ->
            acc + (boardLeft + i * (buttonSize + buttonPadding))
        }
        buttonYs = (0 .. 8).fold(emptyList()) { acc, i ->
            acc + (buttonPadding + i * (buttonSize + buttonPadding))
        }
        boardTop = buttonYs[3]
    }

    private fun setupStageBackground() {
        gameStage.solidRect(gameViewWidth, gameViewHeight, color = gameColors.stageBackground) {
            position(gameViewLeft, gameViewTop)
        }
        gameStage.roundRect(boardWidth, boardWidth, buttonRadius, fill = gameColors.buttonBackground) {
            position(boardLeft, boardTop)
        }
        gameStage.graphics {
            position(boardLeft, boardTop)
            fill(gameColors.cellBackground) {
                for (x in 0 until settings.boardWidth) {
                    for (y in 0 until settings.boardHeight) {
                        roundRect(cellMargin + (cellMargin + cellSize) * x, cellMargin + (cellMargin + cellSize) * y,
                                cellSize, cellSize, buttonRadius)
                    }
                }
            }
        }
    }

    private suspend fun setupAppBar() {
        val appBarTop = buttonYs[1]

        suspend fun button(icon: String, handler: () -> Unit): Container =
            barButton(icon, handler).apply {
                positionY(appBarTop)
            }

        appBarButtons = listOf(
            AppBarButtonsEnum.PLAY to button("play", presenter::onPlayClick),
            AppBarButtonsEnum.TO_START to button("skip_previous", presenter::onToStartClick),
            AppBarButtonsEnum.BACKWARDS to button("backwards", presenter::onBackwardsClick),
            AppBarButtonsEnum.STOP to button("stop", presenter::onStopClick),
            AppBarButtonsEnum.FORWARD to button("forward", presenter::onForwardClick),
            AppBarButtonsEnum.TO_CURRENT to button("skip_next", presenter::onToCurrentClick),

            AppBarButtonsEnum.APP_LOGO to RotatingLogo(this, buttonSize).apply { positionY(appBarTop) },
            AppBarButtonsEnum.WATCH to button("watch", presenter::onWatchClick),
            AppBarButtonsEnum.BOOKMARK to button("bookmark_border", presenter::onBookmarkClick),
            AppBarButtonsEnum.BOOKMARKED to button("bookmark", presenter::onBookmarkedClick),
            AppBarButtonsEnum.PAUSE to button("pause", presenter::onPauseClick),
            AppBarButtonsEnum.RESTART to button("restart", presenter::onRestartClick),
            AppBarButtonsEnum.UNDO to button("undo", presenter::onUndoClick),
            AppBarButtonsEnum.REDO to button("redo", presenter::onRedoClick),
            AppBarButtonsEnum.REDO_PLACEHOLDER to Container(),
            AppBarButtonsEnum.GAME_MENU to button("menu", presenter::onGameMenuClick),
        )
    }

    private suspend fun barButton(icon: String, handler: () -> Unit): Container = Container().apply {
        val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
        image(resourcesVfs["assets/$icon.png"].readBitmap()) {
            size(buttonSize * 0.6, buttonSize * 0.6)
            centerOn(background)
        }
        customOnClick { handler() }
    }

    fun showGameMenu() = showWindow("game_actions") {

        suspend fun button(buttonEnum: GameMenuButtonsEnum, yInd: Int, handler: () -> Unit) =
                barButton(buttonEnum.icon) {
                    handler()
                    window.removeFromParent()
                }.apply {
                    position(buttonXs[0], buttonYs[yInd])
                    addTo(window)
                    window.container {
                        text(stringResources.text(buttonEnum.labelKey), defaultTextSize, gameColors.labelText,
                                font, TextAlignment.MIDDLE_LEFT) {
                            position(buttonXs[1], buttonYs[yInd] + buttonSize / 2)
                            customOnClick {
                                handler()
                                window.removeFromParent()
                            }
                        }
                    }
                }

        button(GameMenuButtonsEnum.BOOKMARKS, 1, presenter::onBookmarksClick)
        button(GameMenuButtonsEnum.RESTORE, 2, presenter::onRestoreClick)
        button(GameMenuButtonsEnum.RESTART, 3, presenter::onRestartClick)
        button(GameMenuButtonsEnum.SHARE, 4, presenter::onShareClick)
        button(GameMenuButtonsEnum.LOAD, 5, presenter::onLoadClick)
        button(GameMenuButtonsEnum.HELP, 6, presenter::onHelpClick)
        button(GameMenuButtonsEnum.DELETE, 7, presenter::onDeleteGameClick)
    }

    /** Workaround for the bug: https://github.com/korlibs/korge-next/issues/56 */
    fun Container.customOnClick(handler: () -> Unit) {
        if (OS.isAndroid) {
            onOver {
                duplicateKeyPressFilter.onSwipeOrOver { myLog("onOver $pos") }
            }
            onDown {
                myLog("onDown $pos")
                buttonPointClicked = pos.copy()
            }
            onClick { myLog("onClick $pos}") }
            onUp {
                val clicked = buttonPointClicked == pos
                myLog("onUp $pos " + if(clicked) "- clicked" else "<- $buttonPointClicked")
                buttonPointClicked = pointNONE
                if (clicked) handler()
            }
        } else {
            onClick {
                handler()
            }
        }
    }

    private fun setupScoreBar() {
        val scoreButtonWidth = (boardWidth - 2 * buttonPadding) / 3
        val scoreButtonTop = buttonYs[2]
        val textYPadding = 28 * gameScale
        val scoreLabelSize = cellSize * 0.30
        val scoreTextSize = cellSize * 0.5

        gameTime = gameStage.text("00:00:00", scoreLabelSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
            positionX(boardLeft + scoreButtonWidth / 2)
            positionY(scoreButtonTop + textYPadding)
        }
        usersMoveNumber = gameStage.text("", scoreTextSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(gameTime)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }.addTo(gameStage)

        val bgScore = gameStage.roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
            position(boardLeft + (scoreButtonWidth + buttonPadding), scoreButtonTop)
        }
        gameStage.text(stringResources.text("score_upper"), scoreLabelSize, gameColors.buttonLabelText, font,
                TextAlignment.MIDDLE_CENTER) {
            positionX(bgScore.pos.x + scoreButtonWidth / 2)
            positionY(scoreButtonTop + textYPadding)
        }
        score = gameStage.text("", scoreTextSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(bgScore)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }

        val bgBest = gameStage.roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
            position(boardLeft + (scoreButtonWidth + buttonPadding) * 2, scoreButtonTop)
        }
        gameStage.text(stringResources.text("best"), scoreLabelSize, gameColors.buttonLabelText, font,
                TextAlignment.MIDDLE_CENTER) {
            positionX(bgBest.pos.x + scoreButtonWidth / 2)
            positionY(scoreButtonTop + textYPadding)
        }
        bestScore = gameStage.text("", scoreTextSize, gameColors.buttonText, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(bgBest)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }
    }

    private fun setupBoardControls(): SolidRect {
        val controlsArea = SolidRect(boardWidth, boardWidth + buttonSize + buttonPadding, gameColors.transparent)
                .addTo(gameStage).position(boardLeft, boardTop)

        controlsArea.onSwipe(20.0) {
            duplicateKeyPressFilter.onSwipeOrOver {
                presenter.onSwipe(it.direction)
            }
        }

        controlsArea.addUpdater {
            val ifKey = { key: Key, action: () -> Unit ->
                if (gameStage.views.input.keys[key]) {
                    duplicateKeyPressFilter.onPress(key, action)
                }
            }
            ifKey(Key.LEFT) { presenter.onSwipe(SwipeDirection.LEFT) }
            ifKey(Key.RIGHT) { presenter.onSwipe(SwipeDirection.RIGHT) }
            ifKey(Key.UP) { presenter.onSwipe(SwipeDirection.TOP) }
            ifKey(Key.DOWN) { presenter.onSwipe(SwipeDirection.BOTTOM) }
            ifKey(Key.SPACE) { presenter.onPauseClick() }
            ifKey(Key.M) { presenter.onGameMenuClick() }
            ifKey(Key.BACKSPACE) {
                presenter.onCloseGameWindowClick()
            }
        }

        return controlsArea
    }

    fun showControls(appBarButtonsToShow: List<AppBarButtonsEnum>, playSpeed: Int) {
        appBarButtons.filter { !appBarButtonsToShow.contains(it.enum) }
            .forEach { it.container.removeFromParent() }

        val toShow = appBarButtons.filter { appBarButtonsToShow.contains(it.enum) }
        val remainingPos = buttonXs
//            .filter { buttonPointClicked.y != appBarTop || it != buttonPointClicked.x }
            .toMutableList()
//        myLog("Last clicked:$buttonPointClicked, Button positions:${remainingPos} y:$appBarTop")

        // Left aligned buttons
        toShow.filter { it.enum.sortOrder < 0 }
            .sortedBy { it.enum.sortOrder }
            .forEach { eb ->
                remainingPos.firstOrNull()?.let {
                    eb.container.positionX(it)
                        .addTo(gameStage)
                    remainingPos.removeFirst()
                }
            }
        // Others are Right-aligned
        toShow.filter { it.enum.sortOrder >= 0 }
            .sortedByDescending { it.enum.sortOrder }
            .forEach { eb ->
                remainingPos.lastOrNull()?.let {
                    eb.container.positionX(it)
                        .addTo(gameStage)
                    remainingPos.removeLast()
                }
            }

        usersMoveNumber.text = presenter.model.usersMoveNumber.toString() +
                (if (playSpeed < 0) " «" else "") +
                (if (playSpeed > 0) " »" else "") +
                (if (playSpeed != 0) abs(playSpeed) else "")
        bestScore.text = presenter.bestScore.toString()
        score.text = presenter.score.toString()

        // Ensure the view is on the top to receive onSwipe events
        boardControls.addTo(gameStage)
    }

    fun showGameOver(): Container = gameStage.container {
        val window = this
        val format = TextFormat(gameColors.labelText, defaultTextSize.toInt(), font)
        val skin = TextSkin(
                normal = format,
                over = format.copy(gameColors.labelTextOver),
                down = format.copy(gameColors.labelTextDown)
        )

        position(boardLeft, boardTop)

        graphics {
            fill(gameColors.gameOverBackground) {
                roundRect(0.0, 0.0, boardWidth, boardWidth, buttonRadius)
            }
        }
        text(stringResources.text("game_over"), defaultTextSize, gameColors.labelText, font, TextAlignment.MIDDLE_CENTER) {
            position(boardWidth / 2, (boardWidth - textSize) / 2)
        }
        uiText(stringResources.text("try_again"), 120.0, 35.0, skin) {
            centerXBetween(0.0, boardWidth)
            positionY((boardWidth + textSize) / 2)
            customOnClick {
                window.removeFromParent()
                presenter.restart()
            }
        }

        addUpdater {
            duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                window.removeFromParent()
                presenter.restart()
            }
        }
    }

    fun showBookmarks(game: GameRecord) = showWindow("goto_bookmark") {
        val listTop = winTop + cellMargin + buttonSize + buttonPadding // To avoid unintentional click on the list after previous click
        val nItems = game.shortRecord.bookmarks.size + 1
        val itemHeight = buttonSize * 3 / 4
        val textWidth = winWidth * 2
        val textSize = defaultTextSize

        fun Container.rowText(value: String, xPosition: Double) = text(value, textSize,
                gameColors.buttonText, font, TextAlignment.MIDDLE_LEFT) {
            position(xPosition, itemHeight / 2)
        }

        fun Container.oneRow(index: Int, score: String, lastChanged: String, duration: String, action: () -> Unit) {
            container {
                roundRect(textWidth, itemHeight, buttonRadius, fill = gameColors.buttonBackground)
                var xPos = cellMargin
                rowText(score, xPos)
                xPos += itemHeight * 1.8
                rowText(lastChanged, xPos)
                xPos += itemHeight * 4.8
                rowText(duration, xPos)

                position(0.0, index * (itemHeight + cellMargin))
                customOnClick { action() }
            }
        }

        uiScrollableArea(config = {
            position(winLeft + cellMargin, listTop)
            buttonSize = itemHeight
            width = winWidth - cellMargin * 2
            contentWidth = textWidth
            height = winTop + winHeight - listTop - cellMargin
            contentHeight = max((itemHeight + cellMargin) * (nItems + 1), height)
        }) {
            oneRow(0, stringResources.text("score"), stringResources.text("last_changed"),
                    stringResources.text("duration")) {}
            with(game.shortRecord.finalBoard) {
                oneRow(1, score.toString(), timeString,
                        gameClock.playedSecondsString) {
                    window.removeFromParent()
                    presenter.onGoToBookmarkClick(this)
                }
            }
            game.shortRecord.bookmarks.reversed().forEachIndexed {index, board ->
                oneRow(index + 2, board.score.toString(), board.timeString,
                        board.gameClock.playedSecondsString) {
                    window.removeFromParent()
                    presenter.onGoToBookmarkClick(board)
                }
            }
        }

        addUpdater {
            duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                window.removeFromParent()
                presenter.showControls()
            }
        }
    }

    fun showGameHistory(prevGames: List<GameRecord.ShortRecord>) = showWindow("restore_game") {
        val listTop = winTop + cellMargin + buttonSize + buttonPadding // To avoid unintentional click on the list after previous click
        val nItems = prevGames.size
        val itemHeight = buttonSize * 3 / 4
        val textWidth = winWidth * 2
        val textSize = defaultTextSize

        fun Container.rowText(value: String, xPosition: Double) = text(value, textSize,
                gameColors.buttonText, font, TextAlignment.MIDDLE_LEFT) {
            position(xPosition, itemHeight / 2)
        }

        fun Container.oneRow(index: Int, score: String, lastChanged: String, duration: String, id: String,
                             note: String, action: () -> Unit) {
            container {
                roundRect(textWidth, itemHeight, buttonRadius, fill = gameColors.buttonBackground)
                var xPos = cellMargin
                rowText(score, xPos)
                xPos += itemHeight * 1.8
                rowText(lastChanged, xPos)
                xPos += itemHeight * 4.8
                rowText(duration, xPos)
                xPos += itemHeight * 2.4
                rowText(id, xPos)
                if (note.isNotBlank()) {
                    xPos += itemHeight * 1.2
                    rowText(note, xPos)
                }

                position(0.0, index * (itemHeight + cellMargin))
                customOnClick { action() }
            }
        }

        uiScrollableArea(config = {
            position(winLeft + cellMargin, listTop)
            buttonSize = itemHeight
            width = winWidth - cellMargin * 2
            contentWidth = textWidth
            height = winTop + winHeight - listTop - cellMargin
            contentHeight = max((itemHeight + cellMargin) * (nItems + 1), height)
        }) {
            oneRow(0, stringResources.text("score"), stringResources.text("last_changed"),
                    stringResources.text("duration"), stringResources.text("id"),
                    stringResources.text("note")) {}

            prevGames.sortedByDescending { it.finalBoard.dateTime }.forEachIndexed {index, game ->
                oneRow(index + 1, game.finalBoard.score.toString(), game.finalBoard.timeString,
                        game.finalBoard.gameClock.playedSecondsString, game.id.toString(), game.note) {
                    window.removeFromParent()
                    presenter.onHistoryItemClick(game.id)
                }
            }
        }
    }

    fun showHelp(): Container = showWindow("help_title") {
        text(stringResources.text("help"), defaultTextSize, gameColors.labelText, font, TextAlignment.TOP_LEFT) {
            position(winLeft + cellMargin, winTop + buttonSize + buttonPadding + cellMargin)
        }
    }

    class MyWindow(val gameView: GameView, val titleKey: String) : Container() {
        val window = this
        val winLeft = gameView.gameViewLeft.toDouble()
        val winTop = gameView.gameViewTop.toDouble()
        val winWidth = gameView.gameViewWidth.toDouble()
        val winHeight = gameView.gameViewHeight.toDouble()

        suspend fun Container.show() {
            roundRect(winWidth, winHeight, buttonRadius, stroke = gameView.gameColors.myWindowBorder, strokeThickness = 2.0,
                    fill = gameView.gameColors.myWindowBackground) {
                position(winLeft, winTop)
            }

            with(gameView) {
                val xPos = buttonXs[4]
                val yPos = listOf(buttonYs[0], buttonYs[1])
//                        .filter { buttonPointClicked.x != xPos || it != buttonPointClicked.y }
                        .first()
                barButton("close") {
                    window.removeFromParent()
                    presenter.onCloseMyWindowClick()
                }.apply {
                    position(xPos, yPos)
                }.addTo(window)

                if (titleKey.isNotEmpty()) {
                    text(stringResources.text(titleKey), defaultTextSize, gameColors.labelText, font,
                            TextAlignment.MIDDLE_CENTER) {
                        position((winLeft + xPos - cellMargin) / 2, winTop + cellMargin + buttonSize / 2)
                    }
                }

                addUpdater {
                    duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                        window.removeFromParent()
                        presenter.showControls()
                    }
                }
            }
        }
    }

    private fun showWindow(titleKey: String, action: suspend MyWindow.() -> Unit) =
            MyWindow(this, titleKey).apply {
                gameStage.launch {
                    show()
                    action()
                    addTo(gameStage)
                }
            }
}