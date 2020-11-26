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
    private var usersMoveNumber: Text by Delegates.notNull()
    private var score: Text by Delegates.notNull()
    private var bestScore: Text by Delegates.notNull()

    private var buttonPointClicked = Point(0, 0)
    private val buttonXs: List<Double>
    private val buttonYs: List<Double>
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
        buttonXs = (0 .. 4).fold(emptyList()) { acc, i ->
            acc + (boardLeft + i * (buttonSize + buttonPadding))
        }
        buttonYs = (0 .. 4).fold(emptyList()) { acc, i ->
            acc + (appBarTop + i * (buttonSize + buttonPadding))
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

        val playButton = appBarButton("play", presenter::onPlayClick)
        val toStartButton = appBarButton("skip_previous", presenter::onToStartClick)
        val backwardsButton = appBarButton("backwards", presenter::onBackwardsClick)
        val stopButton = appBarButton("stop", presenter::onStopClick)
        val forwardButton = appBarButton("forward", presenter::onForwardClick)
        val toCurrentButton = appBarButton("skip_next", presenter::onToCurrentClick)

        val watchButton = appBarButton("watch", presenter::onWatchClick)
        val bookmarkButton = appBarButton("bookmark_border", presenter::onBookmarkClick)
        val bookmarkedButton = appBarButton("bookmark", presenter::onBookmarkedClick)
        val pauseButton = appBarButton("pause", presenter::onPauseClick)
        val restartButton = appBarButton("restart", presenter::onRestartClick)
        val undoButton = appBarButton("undo", presenter::onUndoClick)
        val redoButton = appBarButton("redo", presenter::onRedoClick)
        val gameMenuButton = appBarButton("menu", presenter::onGameMenuClick)

        appBarButtons = mapOf(
            AppBarButtonsEnum.PLAY to playButton,
            AppBarButtonsEnum.TO_START to toStartButton,
            AppBarButtonsEnum.BACKWARDS to backwardsButton,
            AppBarButtonsEnum.STOP to stopButton,
            AppBarButtonsEnum.FORWARD to forwardButton,
            AppBarButtonsEnum.TO_CURRENT to toCurrentButton,

            AppBarButtonsEnum.APP_LOGO to appLogo,
            AppBarButtonsEnum.WATCH to watchButton,
            AppBarButtonsEnum.BOOKMARK to bookmarkButton,
            AppBarButtonsEnum.BOOKMARKED to bookmarkedButton,
            AppBarButtonsEnum.PAUSE to pauseButton,
            AppBarButtonsEnum.RESTART to restartButton,
            AppBarButtonsEnum.UNDO to undoButton,
            AppBarButtonsEnum.REDO to redoButton,
            AppBarButtonsEnum.GAME_MENU to gameMenuButton,
        )
    }

    private suspend fun appBarButton(icon: String, handler: () -> Unit): Container =
        barButton(icon, handler).apply {
            positionY(appBarTop)
        }

    private suspend fun barButton(icon: String, handler: () -> Unit): Container = Container().apply {
        val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
        image(resourcesVfs["assets/$icon.png"].readBitmap()) {
            size(buttonSize * 0.6, buttonSize * 0.6)
            centerOn(background)
        }
        customOnClick { handler() }
    }

    private suspend fun setupGameMenu(): Container = Container().apply {
        val window = this
        val winTop = gameViewTop.toDouble()
        val winLeft = gameViewLeft.toDouble()
        val winWidth = gameViewWidth.toDouble()
        val winHeight = winTop + (buttonSize + buttonPadding) * 4 + cellMargin

        addUpdater {
            duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                presenter.onCloseGameMenuClick()
                removeFromParent()
            }
        }

        roundRect(winWidth, winHeight, buttonRadius, stroke = Colors.BLACK, strokeThickness = 2.0, fill = Colors.WHITE) {
            position(winLeft, winTop)
        }

        text(stringResources.text("game_actions"), defaultTextSize, Colors.BLACK, font, TextAlignment.MIDDLE_CENTER) {
            position(winLeft + winWidth / 2,winTop + buttonSize / 2)
        }

        // Place buttons starting from the second row to avoid unintentional click after previous click
        addButton("bookmarks", buttonXs[0], buttonYs[1]) {
            presenter.onBookmarksClick()
            window.removeFromParent()
        }

        addButton("restore", buttonXs[1], buttonYs[1]) {
            presenter.onRestoreClick()
            window.removeFromParent()
        }

        addButton("restart", buttonXs[3], buttonYs[1]) {
            presenter.onRestartClick()
            window.removeFromParent()
        }

        addButton("close", buttonXs[4], buttonYs[1]) {
            presenter.onCloseGameMenuClick()
            window.removeFromParent()
        }

        addButton("share", buttonXs[2], buttonYs[2]) {
            presenter.onShareClick()
            window.removeFromParent()
        }

        addButton("load", buttonXs[3], buttonYs[2]) {
            presenter.onLoadClick()
            window.removeFromParent()
        }

        addButton("delete", buttonXs[0], buttonYs[3]) {
            presenter.onDeleteGameClick()
            window.removeFromParent()
        }

        addButton("help", buttonXs[4], buttonYs[3]) {
            presenter.onHelpClick()
            window.removeFromParent()
        }
    }

    private suspend fun Container.addButton(icon: String, x: Double, y: Double, handler: () -> Unit): Container =
            barButton(icon, handler).apply {
                position(x, y)
                addTo(this@addButton)
            }

    fun showGameMenu() {
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
        usersMoveNumber = gameStage.text("", scoreTextSize, Colors.BLACK, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(gameTime)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }.addTo(gameStage)

        val bgScore = gameStage.roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = gameColors.buttonBackground) {
            position(boardLeft + (scoreButtonWidth + buttonPadding), scoreButtonTop)
        }
        gameStage.text(stringResources.text("score_upper"), scoreLabelSize, RGBA(239, 226, 210), font,
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

        val xPositions = buttonXs
            .filter { buttonPointClicked.y != appBarTop || it != buttonPointClicked.x }
            .let {
                if (it.size > toShow.size) {
                    val unusedX = it.filterNot { x -> toShow.keys.any { buttonXs[it.positionIndex] == x}}
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
        val format = TextFormat(RGBA(0, 0, 0), defaultTextSize.toInt(), font)
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
        text(stringResources.text("game_over"), defaultTextSize, Colors.BLACK, font, TextAlignment.MIDDLE_CENTER) {
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

    fun showBookmarks(game: GameRecord)  = gameStage.launch { gameStage.container {
        val window = this
        val winLeft = gameViewLeft.toDouble()
        val winTop = gameViewTop.toDouble()
        val winWidth = gameViewWidth.toDouble()
        val winHeight = gameViewHeight.toDouble()

        roundRect(winWidth, winHeight, buttonRadius, stroke = Colors.BLACK, strokeThickness = 2.0, fill = Colors.WHITE) {
            position(winLeft, winTop)
        }

        val buttonCloseX = buttonXs[4]
        container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
            image(resourcesVfs["assets/close.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonCloseX, buttonYs[0])
            customOnClick {
                Console.log("Close clicked")
                window.removeFromParent()
                presenter.showControls()
            }
        }

        text(stringResources.text("goto_bookmark"), defaultTextSize, Colors.BLACK, font,
                TextAlignment.MIDDLE_CENTER) {
            position((winLeft + buttonCloseX - cellMargin) / 2, winTop + cellMargin + buttonSize / 2)
        }

        val listTop = winTop + cellMargin + buttonSize + buttonPadding // To avoid unintentional click on the list after previous click
        val nItems = game.shortRecord.bookmarks.size + 1
        val itemHeight = buttonSize * 3 / 4
        val textWidth = winWidth * 2
        val textSize = defaultTextSize

        fun Container.rowText(value: String, xPosition: Double) = text(value, textSize,
                Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
            position(xPosition, itemHeight / 2)
        }

        fun Container.oneRow(index: Int, score: String, lastChanged: String, duration: String, action: () -> Unit) {
            container {
                roundRect(textWidth, itemHeight, buttonRadius, fill = gameColors.buttonBackground)
                var xPos = cellMargin
                rowText(score, xPos)
                xPos += itemHeight * 1.6
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
    }}

    fun showGameHistory(prevGames: List<GameRecord.ShortRecord>) = gameStage.launch { gameStage.container {
        val window = this
        val winLeft = gameViewLeft.toDouble()
        val winTop = gameViewTop.toDouble()
        val winWidth = gameViewWidth.toDouble()
        val winHeight = gameViewHeight.toDouble()

        roundRect(winWidth, winHeight, buttonRadius, stroke = Colors.BLACK, strokeThickness = 2.0, fill = Colors.WHITE) {
            position(winLeft, winTop)
        }

        val buttonCloseX = buttonXs[4]
        container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = gameColors.buttonBackground)
            image(resourcesVfs["assets/close.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonCloseX, buttonYs[0])
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

        fun Container.rowText(value: String, xPosition: Double) = text(value, textSize,
                Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
            position(xPosition, itemHeight / 2)
        }

        fun Container.oneRow(index: Int, score: String, lastChanged: String, duration: String, id: String,
                             note: String, action: () -> Unit) {
            container {
                roundRect(textWidth, itemHeight, buttonRadius, fill = gameColors.buttonBackground)
                var xPos = cellMargin
                rowText(score, xPos)
                xPos += itemHeight * 1.6
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

        addUpdater {
            duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                window.removeFromParent()
                presenter.showControls()
            }
        }
    }}

    fun showHelp(): Container = gameStage.container {
        val window = this
        position(gameViewLeft, gameViewTop)

        graphics {
            fill(Colors.WHITE) {
                roundRect(0, 0, gameViewWidth, gameViewHeight, buttonRadius.toInt())
            }
        }
        text(stringResources.text("help"), defaultTextSize, Colors.BLACK, font, TextAlignment.TOP_LEFT) {
            position(cellMargin, cellMargin)
        }

        addUpdater {
            duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                presenter.onHelpOkClick()
                window.removeFromParent()
            }
        }

        gameStage.launch {
            addButton("close", buttonXs[4], gameViewHeight - buttonSize - cellMargin) {
                presenter.onHelpOkClick()
                window.removeFromParent()
            }
        }
    }

}