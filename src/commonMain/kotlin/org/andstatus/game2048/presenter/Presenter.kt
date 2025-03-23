package org.andstatus.game2048.presenter

import korlibs.io.concurrent.atomic.incrementAndGet
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.io.lang.use
import korlibs.korge.animate.Animator
import korlibs.korge.animate.animate
import korlibs.korge.animate.block
import korlibs.korge.animate.tween
import korlibs.korge.input.SwipeDirection
import korlibs.korge.tween.get
import korlibs.korge.view.Text
import korlibs.korge.view.onNextFrame
import korlibs.math.interpolation.Easing
import korlibs.time.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.andstatus.game2048.ai.AiAlgorithm
import org.andstatus.game2048.ai.AiPlayer
import org.andstatus.game2048.exitApp
import org.andstatus.game2048.gameIsLoading
import org.andstatus.game2048.gameStopWatch
import org.andstatus.game2048.isTestRun
import org.andstatus.game2048.loadJsonGameRecord
import org.andstatus.game2048.model.GameModeEnum
import org.andstatus.game2048.model.GamePosition
import org.andstatus.game2048.model.GameRecord
import org.andstatus.game2048.model.History
import org.andstatus.game2048.model.Model
import org.andstatus.game2048.model.PieceMoveDelay
import org.andstatus.game2048.model.PieceMoveLoad
import org.andstatus.game2048.model.PieceMoveMerge
import org.andstatus.game2048.model.PieceMoveOne
import org.andstatus.game2048.model.PieceMovePlace
import org.andstatus.game2048.model.PlacedPiece
import org.andstatus.game2048.model.Ply
import org.andstatus.game2048.model.PlyEnum
import org.andstatus.game2048.model.SequenceLineReader
import org.andstatus.game2048.model.Square
import org.andstatus.game2048.myLog
import org.andstatus.game2048.myLogInTest
import org.andstatus.game2048.myMeasured
import org.andstatus.game2048.shareText
import org.andstatus.game2048.view.AppBarButtonsEnum
import org.andstatus.game2048.view.BoardSizeEnum
import org.andstatus.game2048.view.ColorThemeEnum
import org.andstatus.game2048.view.ViewData
import org.andstatus.game2048.view.showBookmarks
import org.andstatus.game2048.view.showGameMenu
import org.andstatus.game2048.view.showHelp
import org.andstatus.game2048.view.showRecentGames

/** @author yvolk@yurivolkov.com */
class Presenter(val view: ViewData, history: History) {
    val model = Model(history)
    val aiPlayer: AiPlayer = AiPlayer(model.myContext)
    private val multithreadedScope: CoroutineScope get() = model.myContext.multithreadedScope
    val mainViewShown = korAtomic(false)
    val isPresenting = korAtomic(false)
    val presentedCounter = korAtomic(0L)
    val score get() = model.score
    val bestScore get() = model.bestScore
    val retries get() = model.gamePosition.retries
    var boardViews = BoardViews(view)
    val gameMode get() = model.gameMode
    var clickCounter = korAtomic(0)

    private fun CoroutineScope.presentGameClock(model: Model, textSupplier: () -> Text) = launch {
        while (true) {
            textSupplier().text = model.gameClock.playedSecondsString
            delay(1000)
        }
    }

    fun onAppEntry(): Unit = myMeasured("onAppEntry") {
        val game = model.history.currentGame
        view.korgeCoroutineScope.presentGameClock(model) { view.mainView.scoreBar.gameTime }
        if (game.isEmpty) {
            if (model.history.recentGames.isEmpty()) {
                myLog("Showing help...")
                view.showHelp().onNextFrame {
                    if (isTestRun.value) {
                        myLog("Closing help in tests")
                        this.removeFromParent()
                        onCloseHelpClick()
                    }
                }
            } else {
                onTryAgainClick()
            }
        } else {
            present {
                model.composerPly(game.shortRecord.finalPosition, true)
            }
            loadPlies()
        }
    }

    fun onNoMagicClick() {
        logClick("NoMagic")
        gameMode.aiEnabled = true
        asyncShowMainView()
    }

    fun onMagicClick() {
        logClick("Magic")
        gameMode.aiEnabled = false
        if (gameMode.modeEnum == GameModeEnum.AI_PLAY) {
            gameMode.modeEnum = GameModeEnum.PLAY
            pauseGame()
        }
        asyncShowMainView()
    }

    fun onAiStartClick() {
        logClick("AiStart")
        gameMode.aiEnabled = true
        startAiPlay()
    }

    fun onAiStopClick() {
        logClick("AiStop")
        gameMode.modeEnum = GameModeEnum.PLAY
        pauseGame()
        asyncShowMainView()
    }

    fun onMoveButtonClick() {
        gameMode.incrementSpeed()
    }

    fun onAiForwardClick() {
        gameMode.incrementSpeed()
    }

    fun onAiAlgorithmSelect(selected: AiAlgorithm) {
        model.myContext.update {
            it.copy(aiAlgorithm = selected)
        }
        asyncShowMainView()
    }

    fun canUndo(): Boolean = model.canUndo()

    fun canRedo(): Boolean = model.canRedo()

    fun onUndoClick() = afterStop {
        logClick("Undo")
        pauseGame()
        undo()
    }

    private fun undo() = presentReversed {
        view.mainView.hideStatusBar()
        boardViews.hideGameOver()
        model.undo() + Ply.delay() + model.undo() + Ply.delay()
    }

    fun onRedoClick() = afterStop {
        logClick("Redo")
        redo()
    }

    private fun redo() = present {
        model.redo() + Ply.delay() + model.redo() + Ply.delay()
    }

    fun onTryAgainClick() = afterStop {
        logClick("TryAgain")
        showMainView()
        model.pauseGame()
        present {
            model.tryAgain()
        }
    }

    fun onSwipe(swipeDirection: SwipeDirection) {
        when (gameMode.modeEnum) {
            GameModeEnum.BACKWARDS, GameModeEnum.FORWARD, GameModeEnum.STOP -> {
                when (swipeDirection) {
                    SwipeDirection.LEFT -> startAutoReplay(GameModeEnum.BACKWARDS)
                    SwipeDirection.RIGHT -> startAutoReplay(GameModeEnum.FORWARD)
                    SwipeDirection.BOTTOM -> onStopClick()
                    else -> {
                    }
                }
            }

            GameModeEnum.PLAY -> {
                userMove(
                    when (swipeDirection) {
                        SwipeDirection.LEFT -> PlyEnum.LEFT
                        SwipeDirection.RIGHT -> PlyEnum.RIGHT
                        SwipeDirection.TOP -> PlyEnum.UP
                        SwipeDirection.BOTTOM -> PlyEnum.DOWN
                    }
                )
            }

            GameModeEnum.AI_PLAY -> {
                when (swipeDirection) {
                    SwipeDirection.LEFT -> {
                        gameMode.decrementSpeed()
                        asyncShowMainView()
                    }

                    SwipeDirection.RIGHT -> {
                        gameMode.incrementSpeed()
                        asyncShowMainView()
                    }

                    else -> {
                        onAiStopClick()
                    }
                }
            }
        }

    }

    fun onBookmarkClick() = afterStop {
        logClick("Bookmark")
        model.createBookmark()
        showMainView()
    }

    fun onBookmarkedClick() = afterStop {
        logClick("Bookmarked")
        model.deleteBookmark()
        showMainView()
    }

    fun onPauseClick() = afterStop {
        logClick("Pause")
        pauseGame()
        showMainView()
    }

    fun onPauseEvent() {
        logClick("onPauseEvent")
        pauseGame()
        asyncShowMainView()
    }

    fun onExitAppClick() {
        logClick("onExitApp")
        pauseGame()
        view.gameStage.gameWindow.close()
        view.gameStage.exitApp()
    }

    fun onWatchClick() = afterStop {
        logClick("Watch")
        gameMode.modeEnum = GameModeEnum.PLAY
        showMainView()
    }

    fun onPlayClick() = afterStop {
        logClick("Play")
        gameMode.modeEnum = GameModeEnum.STOP
        pauseGame()
        showMainView()
    }

    fun onBackwardsClick() {
        logClick("Backwards")
        startAutoReplay(GameModeEnum.BACKWARDS)
    }

    fun onStopClick() = afterStop {
        logClick("Stop")
        gameMode.modeEnum = GameModeEnum.STOP
        showMainView()
    }

    fun onForwardClick() {
        logClick("Forward")
        startAutoReplay(GameModeEnum.FORWARD)
    }

    fun onToStartClick() = afterStop {
        logClick("ToStart")
        boardViews.hideGameOver()
        present {
            model.undoToStart() + Ply.delay() + model.redo()
        }
    }

    fun onToCurrentClick() = afterStop {
        logClick("ToCurrent")
        present {
            model.redoToCurrent()
        }
    }

    fun onGameMenuClick() = afterStop {
        logClick("GameMenu")
        hideMainView()
        view.showGameMenu(model.gameMode, model.history.recentGames.size)
        pauseGame()
    }

    private fun hideMainView() {
        view.mainView.hideStatusBar()
        view.mainView.removeFromParent()
    }

    fun onCloseMyWindowClick() {
        logClick("CloseMyWindow")
        asyncShowMainView()
    }

    fun onCloseHelpClick() {
        logClick("CloseHelp")
        if (model.history.currentGame.isEmpty) {
            present {
                myLog("Trying again...")
                model.tryAgain()
            }
        } else {
            asyncShowMainView()
        }
    }

    fun onDeleteGameClick() = afterStop {
        logClick("DeleteGame")
        model.history.deleteCurrent()
        showMainView()
        if (model.history.currentGame.isEmpty) {
            present {
                model.tryAgain()
            }
        } else {
            openGame(model.history.currentGame.id)
        }
    }

    fun onBookmarksClick() = model.history.currentGame.also { game ->
        logClick("Bookmarks")
        view.showBookmarks(game)
    }

    fun onRecentClick() = afterStop {
        logClick("Recent")
        model.pauseGame()
        view.showRecentGames(model.history.recentGames)
    }

    fun onGoToBookmarkClick(position: GamePosition) = afterStop {
        logClick("GoTo${position.plyNumber}")
        showMainView()
        present {
            model.gotoBookmark(position)
        }
    }

    fun onHistoryItemClick(id: Int) = afterStop {
        logClick("History$id")
        showMainView()
        openGame(id)
    }

    private fun openGame(id: Int) {
        val openGamePlies = model.openGame(id)
        val newBoardSize = model.history.currentGame.boardSize
        if (newBoardSize == view.boardSize) {
            present {
                openGamePlies.also {
                    loadPlies()
                }
            }
        } else {
            model.myContext.update {
                it.copy(boardSize = newBoardSize)
            }
            myLog("Reinitializing: board size changed ${view.boardSize} -> $newBoardSize")
            view.reInitialize()
        }
    }

    private fun loadPlies() = model.history.currentGame.apply {
        if (!isReady) afterStop {
            multithreadedScope.launch {
                load()
                withContext(view.korgeCoroutineContext) {
                    showMainView()
                }
            }
        }
    }

    fun onShareClick() = model.history.currentGame.also { game ->
        afterStop {
            logClick("Share")
            shareText(
                view.stringResources.text("share"), game.shortRecord.jsonFileName,
                game.toSharedJsonSequence()
            )
        }
    }

    fun onLoadClick() = afterStop {
        logClick("Load")
        presentAnd({ model.tryAgain() }) {
            gameIsLoading.value = true
            view.gameStage.loadJsonGameRecord(model.history.myContext) { sequence ->
                loadSharedJson(sequence)
            }
        }
    }

    fun loadSharedJson(json: Sequence<String>) {
        gameIsLoading.value = true
        SequenceLineReader(json).use { reader ->
            GameRecord.fromSharedJson(model.history.myContext, reader, model.history.idForNewGame())
                ?.also {
                    it.load().save()
                    model.history.loadRecentGames()
                    present {
                        model.openGame(it.id).also {
                            gameIsLoading.value = false
                        }
                    }
                }
                ?: run {
                    gameIsLoading.value = false
                }
        }
    }

    fun onHelpClick() = afterStop {
        logClick("Help")
        view.showHelp()
    }

    fun onSelectColorTheme(colorThemeEnum: ColorThemeEnum) {
        logClick("onSelectColorTheme-$colorThemeEnum")
        if (colorThemeEnum == view.myContext.settings.colorThemeEnum) return

        view.myContext.update {
            it.copy(colorThemeEnum = colorThemeEnum)
        }
        pauseGame()
        view.reInitialize()
    }

    fun onSelectBoardSize(boardSize: BoardSizeEnum) {
        logClick("onSelectBoardSize-$boardSize")
        if (boardSize == view.boardSize) return

        view.myContext.update {
            it.copy(boardSize = boardSize)
        }
        model.tryAgain()
        pauseGame()
        view.reInitialize()
    }

    private fun pauseGame() {
        clickCounter.incrementAndGet()
        model.pauseGame()
    }

    private fun startAutoReplay(newMode: GameModeEnum) {
        if (gameMode.modeEnum == newMode) {
            if (newMode == GameModeEnum.BACKWARDS) gameMode.decrementSpeed() else gameMode.incrementSpeed()
            asyncShowMainView()
        } else if (if (newMode == GameModeEnum.BACKWARDS) canUndo() else canRedo()) {
            afterStop {
                val startCount = clickCounter.incrementAndGet()
                gameMode.modeEnum = newMode
                showMainView()
                multithreadedScope.launch {
                    while (startCount == clickCounter.value &&
                        if (gameMode.modeEnum == GameModeEnum.BACKWARDS) canUndo() else canRedo()
                    ) {
                        delayWhilePresenting()
                        if (gameMode.speed != 0) {
                            if (gameMode.modeEnum == GameModeEnum.BACKWARDS) undo() else redo()
                        }
                    }
                    gameMode.stop()
                    withContext(view.korgeCoroutineContext) {
                        showMainView()
                    }
                }
            }
        }
    }

    private fun startAiPlay() = afterStop {
        val startCount = clickCounter.incrementAndGet()
        gameMode.modeEnum = GameModeEnum.AI_PLAY
        showMainView()
        multithreadedScope.aiPlayLoop(this, startCount)
    }

    private fun logClick(buttonName: String) {
        myLog("$buttonName-${view.id} clicked:${clickCounter.value}, mode:${gameMode.modeEnum}")
        gameStopWatch.start()
    }

    private fun afterStop(action: suspend () -> Unit) {
        view.korgeCoroutineScope.launch {
            val startCount = clickCounter.incrementAndGet()
            while (gameMode.autoPlaying && startCount == clickCounter.value) {
                delay(100)
            }
            delayWhilePresenting()
            action()
        }
    }

    fun composerMove(position: GamePosition) = present {
        model.composerPly(position, false)
    }

    fun computerMove() = present {
        model.randomComputerMove()
    }

    fun computerMove(placedPiece: PlacedPiece) = present {
        model.computerMove(placedPiece)
    }

    fun userMove(plyEnum: PlyEnum) {
        present {
            model.userMove(plyEnum).let {
                if (it.isEmpty()) it else it + model.randomComputerMove()
            }
        }
    }

    private fun presentAnd(block: () -> List<Ply>, next: () -> Unit) = present(next, block)

    private fun present(next: () -> Unit = {}, block: () -> List<Ply>) {
        if (isPresenting.compareAndSet(expect = false, update = true)) {
            presentedCounter.incrementAndGet().let {
                myLogInTest { "present ${presentedCounter.value} started" }
                block()
            }.let { plies ->
                view.korgeCoroutineScope.launch {
                    presentFrom(plies, 0)
                    if (gameMode.modeEnum != GameModeEnum.AI_PLAY || gameMode.speed == 0) {
                        // TODO: This is to hide AI tip. Invent explicit way for that
                        view.mainView.hideStatusBar()
                    }
                    showMainView()
                    if (model.noMoreMoves()) {
                        boardViews = boardViews
                            .hideGameOver()
                            .copy()
                            .apply {
                                view.mainView.boardView.showGameOver()
                                pauseGame()
                            }
                    }
                    myLogInTest { "present ${presentedCounter.value} ended" }
                    isPresenting.value = false
                    next()
                }
            }
        } else {
            myLog { "present ${presentedCounter.value} is in progress, skipped" }
        }
    }

    private suspend fun presentFrom(plies: List<Ply>, from: Int) {
        if (from < plies.size) {
            presentPly(plies[from])
            presentFrom(plies, from + 1)
        }
    }

    fun asyncShowMainView() {
        view.korgeCoroutineScope.launch {
            showMainView()
        }
    }

    private val showMainViewStarted = korAtomic(false)
    private fun showMainView() {
        if (showMainViewStarted.compareAndSet(false, true)) {
            try {
                myLogInTest { "showMainView started" }
                if (!view.closed) {
                    view.mainView.show(buttonsToShow(), gameMode.speed)
                    if (gameMode.isPlaying && gameMode.aiEnabled && gameMode.speed == 0) {
                        multithreadedScope.showAiTip(this@Presenter)
                    }
                }
                mainViewShown.value = true
                myLogInTest { "showMainView ended" }
            } finally {
                showMainViewStarted.value = false
            }
        } else {
            myLogInTest { "showMainView already started" }
        }
    }

    private fun buttonsToShow(): List<AppBarButtonsEnum> {
        val list = ArrayList<AppBarButtonsEnum>()
        when (gameMode.modeEnum) {
            GameModeEnum.PLAY -> {
                list.add(AppBarButtonsEnum.PLAY)
                list.add(
                    if (gameMode.aiEnabled) AppBarButtonsEnum.AI_ON else AppBarButtonsEnum.AI_OFF
                )
                if (model.gameClock.started) {
                    list.add(AppBarButtonsEnum.PAUSE)
                } else {
                    list.add(AppBarButtonsEnum.APP_LOGO)
                    if (!canRedo()) {
                        list.add(AppBarButtonsEnum.TRY_AGAIN)
                    }
                }
                list.add(
                    if (model.isBookmarked) AppBarButtonsEnum.BOOKMARKED else AppBarButtonsEnum.BOOKMARK
                )
                if (gameMode.aiEnabled) {
                    list.add(AppBarButtonsEnum.AI_START)
                }

                if (canUndo()) {
                    list.add(AppBarButtonsEnum.UNDO)
                }
                list.add(if (canRedo()) AppBarButtonsEnum.REDO else AppBarButtonsEnum.REDO_PLACEHOLDER)

                list.add(AppBarButtonsEnum.GAME_MENU)
            }

            GameModeEnum.AI_PLAY -> {
                list.add(AppBarButtonsEnum.PLAY)
                list.add(AppBarButtonsEnum.AI_ON)
                list.add(AppBarButtonsEnum.BOOKMARK_PLACEHOLDER)
                list.add(AppBarButtonsEnum.AI_STOP)
                list.add(AppBarButtonsEnum.AI_FORWARD)
            }

            else -> {
                list.add(AppBarButtonsEnum.WATCH)
                if (model.moveNumber > 1 && model.isBookmarked) {
                    list.add(AppBarButtonsEnum.BOOKMARKED)
                } else {
                    list.add(AppBarButtonsEnum.BOOKMARK_PLACEHOLDER)
                }
                if (canUndo()) {
                    if (gameMode.speed == -gameMode.maxSpeed) {
                        list.add(AppBarButtonsEnum.TO_START)
                    } else {
                        list.add(AppBarButtonsEnum.BACKWARDS)
                    }
                }
                if (gameMode.modeEnum == GameModeEnum.STOP) {
                    list.add(AppBarButtonsEnum.APP_LOGO)
                    list.add(AppBarButtonsEnum.STOP_PLACEHOLDER)
                } else {
                    list.add(AppBarButtonsEnum.STOP)
                }
                if (canRedo()) {
                    if (gameMode.speed == gameMode.maxSpeed) {
                        list.add(AppBarButtonsEnum.TO_CURRENT)
                    } else {
                        list.add(AppBarButtonsEnum.FORWARD)
                    }
                } else {
                    list.add(AppBarButtonsEnum.FORWARD_PLACEHOLDER)
                }
                if (gameMode.modeEnum == GameModeEnum.STOP) {
                    list.add(AppBarButtonsEnum.GAME_MENU)
                }
            }
        }
        return list
    }

    private suspend fun presentPly(ply: Ply) = view.gameStage.animate {
        parallel {
            ply.pieceMoves.forEach { move ->
                when (move) {
                    is PieceMovePlace -> boardViews.addBlock(move.first)
                    is PieceMoveLoad -> boardViews.load(move.position)
                    is PieceMoveOne -> boardViews[move.first]?.move(this, move.destination)
                    is PieceMoveMerge -> {
                        val firstBlock = boardViews[move.first]
                        val secondBlock = boardViews[move.second]
                        sequence {
                            parallel {
                                firstBlock?.move(this, move.merged.square)
                                secondBlock?.move(this, move.merged.square)
                            }
                            block {
                                firstBlock?.remove()
                                secondBlock?.remove()
                                boardViews.addBlock(move.merged)
                            }
                            sequenceLazy {
                                boardViews[move.merged]
                                    ?.let { animateBlock(this, it) }
                            }
                        }
                    }

                    is PieceMoveDelay -> with(view) {
                        boardViews.blocks.lastOrNull()?.also {
                            moveTo(it.block, it.square, gameMode.delayMs.milliseconds, Easing.LINEAR)
                        }
                    }
                }
            }
        }
    }

    private fun presentReversed(block: () -> List<Ply>) {
        if (isPresenting.compareAndSet(expect = false, update = true)) {
            presentedCounter.incrementAndGet().let {
                myLogInTest { "presentReversed ${presentedCounter.value} started" }
                block()
            }.let { plies ->
                view.korgeCoroutineScope.launch {
                    presentFromReversed(plies, 0)
                    showMainView()
                    myLogInTest { "presentReversed ${presentedCounter.value} ended" }
                    isPresenting.value = false
                }
            }
        } else {
            myLog { "presentReversed ${presentedCounter.value} is in progress, skipped" }
        }
    }

    private suspend fun presentFromReversed(plies: List<Ply>, from: Int) {
        if (from < plies.size) {
            presentPlyReversed(plies[from])
            presentFromReversed(plies, from + 1)
        }
    }

    private suspend fun presentPlyReversed(ply: Ply) = view.gameStage.animate {
        parallel {
            ply.pieceMoves.asReversed().forEach { move ->
                when (move) {
                    is PieceMovePlace -> boardViews[move.first]
                        ?.also { block ->
                            sequence {
                                sequenceLazy {
                                    animateBlock(this, block)
                                }
                                block {
                                    block.remove()
                                }
                            }
                        }
                        ?: myLog("No Block at destination during undo: $move")

                    is PieceMoveLoad -> boardViews.load(move.position)
                    is PieceMoveOne -> boardViews[PlacedPiece(move.first.piece, move.destination)]
                        ?.move(this, move.first.square)
                        ?: myLog("No Block at destination during undo: $move")

                    is PieceMoveMerge -> {
                        val destination = move.merged.square
                        val effectiveBlock = boardViews[move.merged]
                        sequence {
                            sequenceLazy {
                                effectiveBlock?.let {
                                    animateBlock(this, it)
                                }
                            }
                            block {
                                effectiveBlock?.remove()
                                    ?: myLog("No Block at destination during undo: $move")
                                parallel {
                                    val secondBlock =
                                        boardViews.addBlock(PlacedPiece(move.second.piece, destination))
                                    val firstBlock = boardViews.addBlock(PlacedPiece(move.first.piece, destination))
                                    secondBlock.move(this, move.second.square)
                                    firstBlock.move(this, move.first.square)
                                }
                            }
                        }
                    }

                    is PieceMoveDelay -> with(view) {
                        boardViews.blocks.lastOrNull()?.also {
                            moveTo(it.block, it.square, gameMode.delayMs.milliseconds, Easing.LINEAR)
                        }
                    }
                }
            }
        }
    }

    private fun Block.move(animator: Animator, to: Square) {
        with(view) {
            animator.moveTo(this@move, to, gameMode.moveMs.milliseconds, Easing.LINEAR)
        }
        boardViews.move(PlacedPiece(piece, to), this)
    }

    private fun Block.remove() {
        boardViews.removeBlock(this)
    }

    private fun animateBlock(animator: Animator, block: Block) {
        val x = block.x
        val y = block.y
        val scale: Double = block.scale
        val scaleChange = 0.1f
        val shift = block.scaledWidth * scaleChange / 2
        animator.tween(
            block::x[x - shift],
            block::y[y - shift],
            block::scale[scale + scaleChange],
            time = gameMode.resultingBlockMs.milliseconds,
            easing = Easing.LINEAR
        )
        animator.tween(
            block::x[x],
            block::y[y],
            block::scale[scale],
            time = gameMode.resultingBlockMs.milliseconds,
            easing = Easing.LINEAR
        )
    }

    suspend fun delayWhilePresenting() {
        while (isPresenting.value) delay(20)
    }
}
