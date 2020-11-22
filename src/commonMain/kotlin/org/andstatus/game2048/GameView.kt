package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korev.Key
import com.soywiz.korge.input.SwipeDirection
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onOver
import com.soywiz.korge.input.onSwipe
import com.soywiz.korge.ui.TextFormat
import com.soywiz.korge.ui.TextSkin
import com.soywiz.korge.ui.uiScrollableArea
import com.soywiz.korge.ui.uiText
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
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
    private val appBarTop: Double

    private val buttonSize : Double
    var font: Font by Delegates.notNull()

    var presenter: Presenter by Delegates.notNull()

    internal var gameTime: Text by Delegates.notNull()
    private var moveNumber: Text by Delegates.notNull()
    private var score: Text by Delegates.notNull()
    private var bestScore: Text by Delegates.notNull()

    private var buttonPointClicked = Point(0, 0)
    private val buttonXPositions: List<Double>
    private val duplicateKeyPressFilter = DuplicateKeyPressFilter()

    private var appBarButtons: Map<AppBarButtonsEnum, Container> by Delegates.notNull()
    private var gameMenu: Container by Delegates.notNull()
    private var boardControls: SolidRect by Delegates.notNull()

    companion object {
        suspend fun appEntry(stage: Stage, animateViews: Boolean): GameView {
            loadSettings(stage)
            val stringResources = StringResources.load(defaultLanguage)
            stage.gameWindow.title = stringResources.text("app_name")

            val view = GameView(stage, stringResources, animateViews)
            view.font = resourcesVfs["assets/clear_sans.fnt"].readBitmapFont()
            view.presenter = Presenter(view)
            view.setupStageBackground()
            view.setupAppBar()
            view.setupScoreBar()
            view.boardControls = view.setupBoardControls()
            view.gameMenu = view.setupGameMenu()
            view.presenter.onAppEntry()
            return view
        }
    }

    init {
        if (gameStage.views.virtualWidth < gameStage.views.virtualHeight) {
            gameViewWidth = gameStage.views.virtualWidth
            gameViewHeight = gameViewWidth * defaultGameWindowSize.height / defaultGameWindowSize.width
            gameViewLeft = 0
            gameViewTop = (gameStage.views.virtualHeight - gameViewHeight) / 2
        } else {
            gameViewWidth = gameStage.views.virtualHeight * defaultGameWindowSize.width / defaultGameWindowSize.height
            gameViewHeight = gameStage.views.virtualHeight
            gameViewLeft = (gameStage.views.virtualWidth - gameViewWidth) / 2
            gameViewTop = 0
        }
        Console.log("Window:${gameWindowSize}" +
                " -> Virtual:${gameStage.views.virtualWidth}x${gameStage.views.virtualHeight}" +
                " -> Game:${gameViewWidth}x$gameViewHeight")

        gameScale = gameViewWidth.toDouble() / defaultGameWindowSize.width
        buttonPadding = 27 * gameScale
        appBarTop = buttonPadding + (gameStage.views.virtualHeight - gameViewHeight) / 2

        cellMargin = 15 * gameScale
        buttonRadius = 8 * gameScale

        val allCellMargins = cellMargin * (settings.boardWidth + 1)
        cellSize = (gameViewWidth - allCellMargins - 2 * buttonPadding) / settings.boardWidth
        buttonSize = (gameViewWidth - buttonPadding * 6) / 5
        boardWidth = cellSize * settings.boardWidth + allCellMargins
        boardLeft = gameViewLeft + (gameViewWidth - boardWidth) / 2
        boardTop = appBarTop + buttonSize + buttonPadding + buttonSize + buttonPadding
        buttonXPositions = (0 .. 4).fold(emptyList()) {acc, i ->
            acc + (boardLeft + i * (buttonSize + buttonPadding))
        }
    }

    private fun setupStageBackground() {
        gameStage.solidRect(gameViewWidth, gameViewHeight, color = gameColors.stageBackground)
        gameStage.roundRect(boardWidth, boardWidth, buttonRadius, fill = gameColors.buttonBackground) {
            position(boardLeft, boardTop)
        }
        gameStage.graphics {
            position(boardLeft, boardTop)
            fill(Colors["#cec0b2"]) {
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
        val appLogo = Container().apply {
            roundRect(buttonSize, buttonSize, buttonRadius, fill = RGBA(237, 196, 3))
            text("2048", cellSize * 0.4, Colors.WHITE, font, TextAlignment.MIDDLE_CENTER) {
                position(buttonSize / 2, buttonSize / 2)
            }
            positionY(appBarTop)
        }

        val playButton = appBarButton("assets/play.png", presenter::onPlayClick)
        val toStartButton = appBarButton("assets/skip_previous.png", presenter::onToStartClick)
        val backwardsButton = appBarButton("assets/backwards.png", presenter::onBackwardsClick)
        val stopButton = appBarButton("assets/stop.png", presenter::onStopClick)
        val forwardButton = appBarButton("assets/forward.png", presenter::onForwardClick)
        val toCurrentButton = appBarButton("assets/skip_next.png", presenter::onToCurrentClick)

        val watchButton = appBarButton("assets/watch.png", presenter::onWatchClick)
        val pauseButton = appBarButton("assets/pause.png", presenter::onPauseClick)
        val restartButton = appBarButton("assets/restart.png", presenter::onRestartClick)
        val undoButton = appBarButton("assets/undo.png", presenter::onUndoClick)
        val redoButton = appBarButton("assets/redo.png", presenter::onRedoClick)
        val gameMenuButton = appBarButton("assets/menu.png", presenter::onGameMenuClick)

        appBarButtons = mapOf(
            AppBarButtonsEnum.PLAY to playButton,
            AppBarButtonsEnum.TO_START to toStartButton,
            AppBarButtonsEnum.BACKWARDS to backwardsButton,
            AppBarButtonsEnum.STOP to stopButton,
            AppBarButtonsEnum.FORWARD to forwardButton,
            AppBarButtonsEnum.TO_CURRENT to toCurrentButton,

            AppBarButtonsEnum.APP_LOGO to appLogo,
            AppBarButtonsEnum.WATCH to watchButton,
            AppBarButtonsEnum.PAUSE to pauseButton,
            AppBarButtonsEnum.RESTART to restartButton,
            AppBarButtonsEnum.UNDO to undoButton,
            AppBarButtonsEnum.REDO to redoButton,
            AppBarButtonsEnum.GAME_MENU to gameMenuButton,
        )
    }

    private suspend fun appBarButton(icon: String, handler: () -> Unit): Container = Container().apply {
        val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
        image(resourcesVfs[icon].readBitmap()) {
            size(buttonSize * 0.6, buttonSize * 0.6)
            centerOn(background)
        }
        positionY(appBarTop)
        customOnClick { handler() }
    }

    private suspend fun setupGameMenu(): Container = Container().apply {
        val window = this
        val winTop = appBarTop + buttonSize + buttonPadding // To avoid unintentional click on the list after previous click
        val winWidth = gameViewWidth.toDouble()
        val winHeight = winTop + (buttonSize + buttonPadding) * 2 - cellMargin

        addUpdater {
            duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                presenter.onCloseGameMenuClick()
                removeFromParent()
            }
        }

        roundRect(winWidth, winHeight, buttonRadius, stroke = Colors.BLACK, strokeThickness = 2.0, fill = Colors.WHITE) {
            position(gameViewLeft.toDouble(), cellMargin)
        }

        text(stringResources.text("game_actions"), defaultTextSize, Colors.BLACK, font, TextAlignment.MIDDLE_CENTER) {
            position(gameViewLeft + winWidth / 2,appBarTop + buttonSize / 2)
        }

        container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
            image(resourcesVfs["assets/delete.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonXPositions[0], winTop)
            customOnClick {
                presenter.onDeleteGameClick()
                window.removeFromParent()
            }
        }

        container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
            image(resourcesVfs["assets/restore.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonXPositions[2], winTop)
            customOnClick {
                presenter.onRestoreClick()
                window.removeFromParent()
            }
        }

        container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
            image(resourcesVfs["assets/restart.png"].readBitmap()) {
                size(buttonSize * 0.8, buttonSize * 0.8)
                centerOn(background)
            }
            position(buttonXPositions[3], winTop)
            customOnClick {
                presenter.onRestartClick()
                window.removeFromParent()
            }
        }

        container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
            image(resourcesVfs["assets/close.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonXPositions[4], winTop)
            customOnClick {
                presenter.onCloseGameMenuClick()
                window.removeFromParent()
            }
        }

        container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
            image(resourcesVfs["assets/share.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonXPositions[2], winTop + buttonPadding + buttonSize)
            customOnClick {
                presenter.onShareClick()
                window.removeFromParent()
            }
        }

        container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
            image(resourcesVfs["assets/load.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonXPositions[3], winTop + buttonPadding + buttonSize)
            customOnClick {
                presenter.onLoadClick()
                window.removeFromParent()
            }
        }
    }

    fun showGameMenu(gameRecord: GameRecord) {
        boardControls.removeFromParent()
        gameMenu.addTo(gameStage)
    }

    /** Workaround for the bug: https://github.com/korlibs/korge-next/issues/56 */
    private fun Container.customOnClick(handler: () -> Unit) {
        if (OS.isAndroid) {
            onOver {
                duplicateKeyPressFilter.onSwipeOrOver {
                    val pos1 = this.pos.copy()
                    Console.log("onOver ${buttonPointClicked} -> $pos1")
                    buttonPointClicked = pos1
                    handler()
                }
            }
        } else {
            onClick {
                handler()
            }
        }
    }

    private fun setupScoreBar() {
        val scoreButtonWidth = (boardWidth - 2 * buttonPadding) / 3
        val scoreButtonTop = appBarTop + buttonSize + buttonPadding
        val textYPadding = 28 * gameScale
        val scoreLabelSize = cellSize * 0.30
        val scoreTextSize = cellSize * 0.5

        gameTime = gameStage.text("00:00:00", scoreLabelSize, Colors.BLACK, font, TextAlignment.MIDDLE_CENTER) {
            positionX(boardLeft + scoreButtonWidth / 2)
            positionY(scoreButtonTop + textYPadding)
        }
        moveNumber = gameStage.text("", scoreTextSize, Colors.BLACK, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(gameTime)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }.addTo(gameStage)

        val bgScore = gameStage.roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
            position(boardLeft + (scoreButtonWidth + buttonPadding), scoreButtonTop)
        }
        gameStage.text(stringResources.text("score"), scoreLabelSize, RGBA(239, 226, 210), font,
                TextAlignment.MIDDLE_CENTER) {
            positionX(bgScore.pos.x + scoreButtonWidth / 2)
            positionY(scoreButtonTop + textYPadding)
        }
        score = gameStage.text("", scoreTextSize, Colors.WHITE, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(bgScore)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }

        val bgBest = gameStage.roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
            position(boardLeft + (scoreButtonWidth + buttonPadding) * 2, scoreButtonTop)
        }
        gameStage.text(stringResources.text("best"), scoreLabelSize, RGBA(239, 226, 210), font,
                TextAlignment.MIDDLE_CENTER) {
            positionX(bgBest.pos.x + scoreButtonWidth / 2)
            positionY(scoreButtonTop + textYPadding)
        }
        bestScore = gameStage.text("", scoreTextSize, Colors.WHITE, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(bgBest)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }
    }

    private fun setupBoardControls(): SolidRect {
        val boardView = SolidRect(boardWidth, boardWidth, Colors.TRANSPARENT_WHITE)
                .addTo(gameStage).position(boardLeft, boardTop)

        boardView.onSwipe(20.0) {
            duplicateKeyPressFilter.onSwipeOrOver {
                presenter.onSwipe(it.direction)
            }
        }

        boardView.addUpdater {
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
                Console.log("Closing game window...")
                gameStage.gameWindow.close()
            }
        }

        return boardView
    }

    fun showControls(appBarButtonsToShow: List<AppBarButtonsEnum>, playSpeed: Int) {
        gameMenu.removeFromParent()

        appBarButtons.filter { !appBarButtonsToShow.contains(it.key) }
            .values
            .forEach { it.removeFromParent() }

        val toShow = appBarButtons.filter { appBarButtonsToShow.contains(it.key) }

        val xPositions = buttonXPositions
            .filter { buttonPointClicked.y != appBarTop || it != buttonPointClicked.x }
            .let {
                if (it.size > toShow.size) {
                    val unusedX = it.filterNot { x -> toShow.keys.any { buttonXPositions[it.positionIndex] == x}}
                            .take(it.size - toShow.size)
                    //it.filterNot { x -> unusedX.contains(x)}
                    it.filterNot(unusedX::contains)
                }
                else it
            }

        Console.log("Last clicked:$buttonPointClicked, Button positions:${xPositions} y:$appBarTop")

        toShow.values.zip(xPositions)
        .forEach { (button, x) ->
            button.positionX(x)
            button.addTo(gameStage)
        }

        moveNumber.text = presenter.model.moveNumber.toString() +
                (if (playSpeed < 0) " «" else "") +
                (if (playSpeed > 0) " »" else "") +
                (if (playSpeed != 0) abs(playSpeed) else "")
        bestScore.text = presenter.bestScore.toString()
        score.text = presenter.score.toString()

        // Ensure the view is on the top to receive onSwipe events
        boardControls.addTo(gameStage)
    }

    fun showGameOver(): Container = gameStage.container {
        val format = TextFormat(RGBA(0, 0, 0), 40, font)
        val skin = TextSkin(
                normal = format,
                over = format.copy(RGBA(90, 90, 90)),
                down = format.copy(RGBA(120, 120, 120))
        )

        position(boardLeft, boardTop)

        graphics {
            fill(Colors.WHITE, 0.2) {
                roundRect(0.0, 0.0, boardWidth, boardWidth, buttonRadius)
            }
        }
        text(stringResources.text("game_over"), 50.0, Colors.BLACK, font, TextAlignment.MIDDLE_CENTER) {
            position(boardWidth / 2, (boardWidth - textSize) / 2)
        }
        uiText(stringResources.text("try_again"), 120.0, 35.0, skin) {
            centerXBetween(0.0, boardWidth)
            positionY((boardWidth + textSize) / 2)
            customOnClick {
                removeFromParent()
                presenter.restart()
            }
        }

        addUpdater {
            duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                removeFromParent()
                presenter.restart()
            }
        }
    }

    fun showGameHistory(prevGames: List<GameRecord.ShortRecord>) =
        gameStage.launch { setupGameHistory(prevGames).addTo(gameStage) }

    private suspend fun setupGameHistory(prevGames: List<GameRecord.ShortRecord>): Container = Container().apply {
        val window = this
        val winLeft = gameViewLeft.toDouble()
        val winTop = appBarTop + buttonSize + cellMargin
        val winWidth = gameViewWidth.toDouble()
        val winHeight = gameViewHeight.toDouble() - winTop

        roundRect(winWidth, winHeight, buttonRadius, stroke = Colors.BLACK, strokeThickness = 2.0, fill = Colors.WHITE) {
            position(winLeft, winTop)
        }

        val buttonCloseX = buttonXPositions[4]
        container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
            image(resourcesVfs["assets/close.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonCloseX, winTop + cellMargin)
            customOnClick {
                Console.log("Close clicked")
                window.removeFromParent()
                presenter.showControls()
            }
        }

        text(stringResources.text("restore_game"), defaultTextSize, Colors.BLACK, font,
                TextAlignment.MIDDLE_CENTER) {
            position((winLeft + buttonCloseX - cellMargin) / 2, winTop + cellMargin + buttonSize / 2)
        }

        val listTop = winTop + cellMargin + buttonSize + buttonPadding // To avoid unintentional click on the list after previous click

        val nItems = prevGames.size
        val itemHeight = buttonSize * 3 / 4
        val textWidth = winWidth * 2
        val textSize = defaultTextSize
        uiScrollableArea(config = {
            position(winLeft + cellMargin, listTop)
            buttonSize = itemHeight
            width = winWidth - cellMargin * 2
            contentWidth = textWidth
            height = winTop + winHeight - listTop - cellMargin
            contentHeight = max(itemHeight * nItems + itemHeight * 0.5, height)
        }) {
            prevGames.sortedByDescending { it.finalBoard.dateTime }.forEachIndexed {index, game ->
                container {
                    roundRect(textWidth, itemHeight, buttonRadius, fill = gameColors.buttonBackground)
                    var xPos = cellMargin
                    text(game.finalBoard.score.toString(), textSize, Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
                        position(xPos, itemHeight / 2)
                    }
                    xPos += itemHeight * 1.6
                    text(game.timeString, textSize, Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
                        position(xPos, itemHeight / 2)
                    }
                    xPos += itemHeight * 4.8
                    text(game.finalBoard.gameClock.playedSecondsString, textSize, Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
                        position(xPos, itemHeight / 2)
                    }
                    xPos += itemHeight * 2.4
                    text("id:${game.id}", textSize, Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
                        position(xPos, itemHeight / 2)
                    }
                    if (game.note.isNotBlank()) {
                        xPos += itemHeight * 1.2
                        text(game.note, textSize, Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
                            position(xPos, itemHeight / 2)
                        }
                    }

                    position(0.0, index * (itemHeight + cellMargin))
                    customOnClick {
                        window.removeFromParent()
                        presenter.onHistoryItemClick(game.id)
                    }
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

}