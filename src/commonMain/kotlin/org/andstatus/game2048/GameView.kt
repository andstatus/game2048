package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korev.Key
import com.soywiz.korge.Korge
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
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korio.async.launch
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.vector.roundRect
import kotlin.math.max
import kotlin.properties.Delegates

private const val buttonPadding = 18.0
private const val appBarTop = buttonPadding
const val buttonRadius = 5.0

class GameView(val gameStage: Stage, val animateViews: Boolean = true) {
    private val bgColor = Colors["#b9aea0"]
    private var buttonSize : Double = 0.0

    var presenter: Presenter by Delegates.notNull()

    internal var gameTime: Text by Delegates.notNull()
    private var moveNumber: Text by Delegates.notNull()
    private var score: Text by Delegates.notNull()
    private var bestScore: Text by Delegates.notNull()

    private var buttonPointClicked = Point(0, 0)
    private var buttonXPositions: List<Double> by Delegates.notNull()
    private val duplicateKeyPressFilter = DuplicateKeyPressFilter()

    private var appLogo: Container by Delegates.notNull()
    private var playBackwardsButton: Container by Delegates.notNull()
    private var playButton: Container by Delegates.notNull()
    private var pauseButton: Container by Delegates.notNull()
    private var toStartButton: Container by Delegates.notNull()
    private var toCurrentButton: Container by Delegates.notNull()
    private var undoButton: Container by Delegates.notNull()
    private var redoButton: Container by Delegates.notNull()
    private var gameMenuButton: Container by Delegates.notNull()

    private var gameBarTop: Double by Delegates.notNull()
    private var gameMenu: Container by Delegates.notNull()
    private var deleteButton: Container by Delegates.notNull()
    private var restartButton: Container by Delegates.notNull()
    private var restoreButton: Container by Delegates.notNull()
    private var closeButton: Container by Delegates.notNull()

    private var boardControls: SolidRect by Delegates.notNull()

    companion object {
        suspend fun mainEntry() = Korge(width = 480, height = 680, title = "2048 Open Fun Game",
                gameId = "org.andstatus.game2048",
                bgcolor = Colors["#fdf7f0"]) {
            appEntry(this, true)
        }

        suspend fun appEntry(stage: Stage, animateViews: Boolean): GameView {
            loadSettings(stage)

            val view = GameView(stage, animateViews)
            font = resourcesVfs["assets/clear_sans.fnt"].readBitmapFont()
            val allCellMargins = cellMargin * (settings.boardWidth + 1)
            cellSize = (stage.views.virtualWidth - allCellMargins - 2 * buttonPadding) / settings.boardWidth
            view.buttonSize = (stage.views.virtualWidth - buttonPadding * 6) / 5
            boardWidth = cellSize * settings.boardWidth + allCellMargins
            boardLeft = (stage.views.virtualWidth - boardWidth) / 2
            boardTop = appBarTop + view.buttonSize + buttonPadding + view.buttonSize + buttonPadding
            view.buttonXPositions = (0 .. 4).fold(emptyList()) {acc, i ->
                acc + (boardLeft + i * (view.buttonSize + buttonPadding))
            }

            view.presenter = Presenter(view)
            view.setupAppBar()
            view.setupScoreBar()
            view.setupBoardBackground()
            view.boardControls = view.setupBoardControls()
            view.gameMenu = view.setupGameMenu()
            view.presenter.onAppEntry()
            return view
        }
    }

    private suspend fun setupAppBar() {
        appLogo = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = RGBA(237, 196, 3))
            text("2048", cellSize * 0.4, Colors.WHITE, font, TextAlignment.MIDDLE_CENTER) {
                centerOn(background)
            }
            positionY(appBarTop)
            customOnClick { presenter.onLogoClick() }
        }

        playBackwardsButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/play_backwards.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            positionY(appBarTop)
            customOnClick { presenter.onPlayBackwardsClick() }
        }

        pauseButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/pause.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            positionY(appBarTop)
            customOnClick { presenter.onPauseClick() }
        }

        playButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/play.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            positionY(appBarTop)
            customOnClick { presenter.onPlayClick() }
        }

        toStartButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/skip_previous.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            positionY(appBarTop)
            customOnClick { presenter.onToStartClick() }
        }

        toCurrentButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/skip_next.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            positionY(appBarTop)
            customOnClick { presenter.onToCurrentClick() }
        }

        undoButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/undo.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            positionY(appBarTop)
            customOnClick { presenter.onUndoClick() }
        }

        redoButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/redo.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            positionY(appBarTop)
            customOnClick { presenter.onRedoClick() }
        }

        gameMenuButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/menu.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            positionY(appBarTop)
            customOnClick { presenter.onGameMenuClick() }
        }
    }

    private suspend fun setupGameMenu(): Container {
        gameBarTop = appBarTop + buttonSize + buttonPadding // To avoid unintentional click on the list after previous click
        val winWidth = gameStage.views.virtualWidth.toDouble()
        val winHeight = gameBarTop + (buttonSize + buttonPadding) * 2

        val window = Container().apply {
            addUpdater {
                duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                    removeFromParent()
                    presenter.showControls()
                }
            }
        }

        window.graphics {
            fill(Colors.WHITE) {
                roundRect(0.0, 0.0, winWidth, winHeight, buttonRadius)
            }
        }

        window.text("Choose game action", 40.0, Colors.BLACK, font, TextAlignment.MIDDLE_CENTER) {
            position(winWidth / 2,appBarTop + buttonSize / 2)
        }

        deleteButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/delete.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonXPositions[0], gameBarTop)
            customOnClick {
                presenter.onDeleteGameClick()
                window.removeFromParent()
            }
            addTo(window)
        }

        restoreButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/restore.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonXPositions[2], gameBarTop)
            customOnClick {
                presenter.onRestoreClick()
                window.removeFromParent()
            }
            addTo(window)
        }

        restartButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/restart.png"].readBitmap()) {
                size(buttonSize * 0.8, buttonSize * 0.8)
                centerOn(background)
            }
            position(buttonXPositions[3], gameBarTop)
            customOnClick {
                presenter.onRestartClick()
                window.removeFromParent()
            }
            addTo(window)
        }

        closeButton = Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/close.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonXPositions[4], gameBarTop)
            customOnClick {
                Console.log("Close clicked")
                window.removeFromParent()
            }
            addTo(window)
        }

        Container().apply {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/share.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonXPositions[2], gameBarTop + buttonPadding + buttonSize)
            customOnClick {
                presenter.onShareClick()
                window.removeFromParent()
            }
            addTo(window)
        }

        return window
    }

    fun showGameMenu(gameRecord: GameRecord) {
        boardControls.removeFromParent()
        gameMenu.addTo(gameStage)
    }

    /** Workaround for the bug: https://github.com/korlibs/korge-next/issues/56 */
    private fun Container.customOnClick(handler: () -> Unit) {
        if (OS.isAndroid) {
            onOver {
                Console.log("onOver ${this.pos}")
                buttonPointClicked = this.pos
                handler()
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
        val textYPadding = 15.0
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

        val bgScore = gameStage.roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = bgColor) {
            position(boardLeft + (scoreButtonWidth + buttonPadding), scoreButtonTop)
        }
        gameStage.text("SCORE", scoreLabelSize, RGBA(239, 226, 210), font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(bgScore)
            positionY(scoreButtonTop + textYPadding)
        }
        score = gameStage.text("", scoreTextSize, Colors.WHITE, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(bgScore)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }

        val bgBest = gameStage.roundRect(scoreButtonWidth, buttonSize, buttonRadius, fill = bgColor) {
            position(boardLeft + (scoreButtonWidth + buttonPadding) * 2, scoreButtonTop)
        }
        gameStage.text("BEST", scoreLabelSize, RGBA(239, 226, 210), font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(bgBest)
            positionY(scoreButtonTop + textYPadding)
        }
        bestScore = gameStage.text("", scoreTextSize, Colors.WHITE, font, TextAlignment.MIDDLE_CENTER) {
            centerXOn(bgBest)
            positionY(scoreButtonTop + scoreLabelSize + textYPadding)
        }
    }

    private fun setupBoardBackground() {
        gameStage.roundRect(boardWidth, boardWidth, buttonRadius, fill = bgColor) {
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

    private fun setupBoardControls(): SolidRect {
        val boardView = SolidRect(boardWidth, boardWidth, Colors.TRANSPARENT_WHITE)
                .addTo(gameStage).position(boardLeft, boardTop)

        boardView.onSwipe(20.0) {
            when (it.direction) {
                SwipeDirection.LEFT -> presenter.userMove(PlayerMoveEnum.LEFT)
                SwipeDirection.RIGHT -> presenter.userMove(PlayerMoveEnum.RIGHT)
                SwipeDirection.TOP -> presenter.userMove(PlayerMoveEnum.UP)
                SwipeDirection.BOTTOM -> presenter.userMove(PlayerMoveEnum.DOWN)
            }
        }

        boardView.addUpdater {
            val keys = gameStage.views.input.keys
            if (keys[Key.LEFT]) { presenter.userMove(PlayerMoveEnum.LEFT) }
            if (keys[Key.RIGHT]) { presenter.userMove(PlayerMoveEnum.RIGHT) }
            if (keys[Key.UP]) { presenter.userMove(PlayerMoveEnum.UP) }
            if (keys[Key.DOWN]) { presenter.userMove(PlayerMoveEnum.DOWN) }
            if (keys[Key.SPACE]) { presenter.onPauseClick() }
            if (keys[Key.M]) { presenter.onGameMenuClick() }
            if (keys[Key.BACKSPACE]) duplicateKeyPressFilter.onPress(Key.BACKSPACE) {
                Console.log("Closing game window...")
                gameStage.gameWindow.close()
            }
        }

        return boardView
    }

    fun showControls(appBarButtonsToShow: List<AppBarButtonsEnum>) {
        gameMenu.removeFromParent()

        Console.log("Last clicked:$buttonPointClicked, Button positions:$buttonXPositions")
        val show = showButton(
                buttonXPositions.filter { buttonPointClicked.y != appBarTop ||  it != buttonPointClicked.x },
                appBarButtonsToShow)
        show(appLogo, AppBarButtonsEnum.APP_LOGO)
        show(playBackwardsButton, AppBarButtonsEnum.PLAY_BACKWARDS)
        show(playButton, AppBarButtonsEnum.PLAY)
        show(pauseButton, AppBarButtonsEnum.PAUSE)
        show(toStartButton, AppBarButtonsEnum.TO_START)
        show(toCurrentButton, AppBarButtonsEnum.TO_CURRENT)
        show(undoButton, AppBarButtonsEnum.UNDO)
        show(redoButton, AppBarButtonsEnum.REDO)
        show(gameMenuButton, AppBarButtonsEnum.GAME_MENU)

        moveNumber.text = presenter.model.moveNumber.toString()
        bestScore.text = presenter.bestScore.toString()
        score.text = presenter.score.toString()

        // Ensure the view is on the top to receive onSwipe events
        boardControls.addTo(gameStage)
    }

    private fun showButton(xPositions: List<Double>, appBarButtonsToShow: List<AppBarButtonsEnum>) = { button: Container, tag: AppBarButtonsEnum ->
        if (appBarButtonsToShow.contains(tag)) {
            val x = xPositions[tag.positionIndex.let { if (tag.positionIndex < 2 || xPositions.size > 4) it else it - 1} ]
            button.positionX(x)
            button.addTo(gameStage)
        } else {
            button.removeFromParent()
        }
    }

    fun showGameOver(): Container = gameStage.container {
        val format = TextFormat(RGBA(0, 0, 0), 40, font)
        val skin = TextSkin(
                normal = format,
                over = format.copy(RGBA(90, 90, 90)),
                down = format.copy(RGBA(120, 120, 120))
        )

        fun close() {
            removeFromParent()
            presenter.restart()
        }

        position(boardLeft, boardTop)

        val bgGameOver = graphics {
            fill(Colors.WHITE, 0.2) {
                roundRect(0.0, 0.0, boardWidth, boardWidth, buttonRadius)
            }
        }
        text("Game Over", 60.0, Colors.BLACK, font, TextAlignment.MIDDLE_CENTER) {
            centerOn(bgGameOver)
        }
        uiText("Try again", 120.0, 35.0, skin) {
            centerBetween(0.0, 0.0, boardWidth, boardWidth)
            y += 40
            customOnClick { close() }
        }

        addUpdater {
            duplicateKeyPressFilter.ifWindowCloseKeyPressed(gameStage.views.input) {
                close()
            }
        }
    }

    fun showGameHistory(prevGames: List<GameRecord.ShortRecord>) =
        gameStage.launch { internalShowGameHistory(prevGames) }

    private suspend fun internalShowGameHistory(prevGames: List<GameRecord.ShortRecord>): Container = gameStage.container {
        val window = this
        val winWidth = gameStage.views.virtualWidth.toDouble()
        val winHeight = gameStage.views.virtualHeight.toDouble()

        graphics {
            fill(Colors.WHITE) {
                roundRect(0.0, 0.0, winWidth, winHeight, buttonRadius)
            }
        }

        val buttonCloseX = buttonXPositions[4]
        container {
            val background = roundRect(buttonSize, buttonSize, buttonRadius, fill = bgColor)
            image(resourcesVfs["assets/close.png"].readBitmap()) {
                size(buttonSize * 0.6, buttonSize * 0.6)
                centerOn(background)
            }
            position(buttonCloseX, gameBarTop)
            customOnClick {
                Console.log("Close clicked")
                window.removeFromParent()
                presenter.showControls()
            }
        }

        text("Select game to restore", 40.0, Colors.BLACK, font, TextAlignment.MIDDLE_CENTER) {
            position((buttonCloseX - cellMargin) / 2, gameBarTop + buttonSize / 2)
        }

        val listTop = gameBarTop + buttonSize + buttonPadding // To avoid unintentional click on the list after click onLogo

        val nItems = prevGames.size
        val itemHeight = buttonSize
        val textWidth = winWidth * 2
        uiScrollableArea(config = {
            position(cellMargin, listTop)
            width = winWidth - cellMargin * 2
            contentWidth = textWidth
            height = winHeight - listTop - cellMargin
            contentHeight = max(itemHeight * nItems, height)
        }) {
            prevGames.sortedByDescending { it.finalBoard.dateTime }.forEachIndexed {index, game ->
                val button = Container().apply {
                    val background = roundRect(textWidth, itemHeight, buttonRadius, fill = bgColor)
                    var xPos = cellMargin
                    text(game.finalBoard.score.toString(), itemHeight * 0.6, Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
                        positionX(xPos)
                        centerYOn(background)
                    }
                    xPos += itemHeight * 1.5
                    text(game.timeString, itemHeight * 0.6, Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
                        positionX(xPos)
                        centerYOn(background)
                    }
                    xPos += itemHeight * 4
                    text(game.finalBoard.gameClock.playedSecondsString, itemHeight * 0.6, Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
                        positionX(xPos)
                        centerYOn(background)
                    }
                    xPos += itemHeight * 2
                    text("id:${game.id}", itemHeight * 0.6, Colors.WHITE, font, TextAlignment.MIDDLE_LEFT) {
                        positionX(xPos)
                        centerYOn(background)
                    }
                    position(0.0, index * (itemHeight + cellMargin))
                    customOnClick {
                        window.removeFromParent()
                        presenter.onHistoryItemClick(game.id)
                    }
                    addTo(this@uiScrollableArea)
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