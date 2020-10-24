package org.andstatus.game2048

import com.soywiz.klock.seconds
import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korge.animate.Animator
import com.soywiz.korge.animate.animateSequence
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.Stage
import com.soywiz.korio.async.ObservableProperty
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.concurrent.atomic.KorAtomicBoolean
import com.soywiz.korma.interpolation.Easing

class Presenter(private val stage: Stage, private val animateViews: Boolean) {
    val model = Model()
    private val moveIsInProgress = KorAtomicBoolean(false)
    val score = ObservableProperty(-1)
    val bestScore = ObservableProperty(-1)
    var boardViews = BoardViews(stage, settings.boardWidth, settings.boardHeight)

    fun onAppEntry() = model.onAppEntry().present()

    fun computerMove() = model.computerMove().present()

    fun computerMove(placedPiece: PlacedPiece) = model.computerMove(placedPiece).present()

    fun canUndo(): Boolean = model.canUndo()

    fun canRedo(): Boolean = model.canRedo()

    fun undo() = model.undo().appendAll(model.undo()).presentReversed()

    fun redo() = model.redo().appendAll(model.redo()).present()

    fun composerMove(board: Board) = model.composerMove(board).present()

    fun restart() = model.restart().present()

    fun userMove(playerMoveEnum: PlayerMoveEnum) {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        if (model.noMoreMoves()) {
            boardViews = boardViews
                    .apply { gameOver?.removeFromParent() }
                    .copy()
                    .apply { gameOver = showGameOver(stage) }
            onPresentEnd()
        } else {
            model.userMove(playerMoveEnum).let{
                if (it.isEmpty()) it else it.appendAll(model.computerMove())
            }.present()
        }
    }

    private fun List<PlayerMove>.present(index: Int = 0) {
        if (index < size) {
            present(stage, this[index]) {
                present(index + 1)
            }
        } else {
            onPresentEnd()
        }
    }

    private fun onPresentEnd() {
        if (score.value != model.score) {
            score.update(model.score)
        }
        if (bestScore.value != model.bestScore) {
            bestScore.update(model.bestScore)
        }
        restoreControls(stage)
        moveIsInProgress.value = false
    }

    private fun present(stage: Stage, playerMove: PlayerMove, onEnd: () -> Unit) = stage.launchImmediately {
        stage.animateSequence {
            parallel {
                playerMove.moves.forEach { move ->
                    when(move) {
                        is MovePlace -> {
                            boardViews[move.first.square] = Block(move.first.piece).addTo(stage, move.first.square)
                        }
                        is MoveLoad -> {
                            boardViews.load(move.board)
                            restoreControls(stage)
                        }
                        is MoveOne -> {
                            val firstBlock = boardViews[move.first.square]
                            firstBlock?.move(this, move.first.square, move.destination)
                        }
                        is MoveMerge -> {
                            val firstBlock = boardViews[move.first.square]
                            val secondBlock = boardViews[move.second.square]
                            sequence {
                                parallel {
                                    firstBlock?.move(this, move.first.square, move.merged.square)
                                    secondBlock?.move(this, move.second.square, move.merged.square)
                                }
                                block {
                                    firstBlock?.removeFromParent()
                                    secondBlock?.removeFromParent()
                                    boardViews.addBlock(move.merged)
                                }
                                sequenceLazy {
                                    if (animateViews) boardViews[move.merged.square]
                                            ?.let { animateResultingBlock(this, it) }
                                }
                            }
                        }
                    }
                }
            }
            block {
                onEnd()
            }
        }
    }

    private fun List<PlayerMove>.presentReversed(index: Int = 0) {
        if (index < size) {
            presentReversed(stage, this[index]) {
                presentReversed(index + 1)
            }
        } else {
            onPresentEnd()
        }
    }

    private fun presentReversed(stage: Stage, playerMove: PlayerMove, onEnd: () -> Unit) = stage.launchImmediately {
        stage.animateSequence {
            parallel {
                playerMove.moves.asReversed().forEach { move ->
                    when(move) {
                        is MovePlace -> {
                            val firstBlock = boardViews[move.first.square]
                            if (firstBlock == null) {
                                Console.log("No Block at destination during undo: $move")
                            }
                            boardViews[move.first.square]?.removeFromParent()
                            boardViews[move.first.square] = null
                        }
                        is MoveLoad -> {
                            boardViews.load(move.board)
                            restoreControls(stage)
                        }
                        is MoveOne -> {
                            val firstBlock = boardViews[move.destination]
                            if (firstBlock == null) {
                                Console.log("No Block at destination during undo: $move")
                            }
                            firstBlock?.move(this, move.destination, move.first.square)
                        }
                        is MoveMerge -> {
                            val destination = move.merged.square
                            val effectiveBlock = boardViews[destination]
                            sequence {
                                if (effectiveBlock == null) {
                                    Console.log("No Block at destination during undo: $move")
                                }
                                block {
                                    effectiveBlock?.removeFromParent()
                                    boardViews[destination] = null
                                }
                                parallel {
                                    val secondBlock = boardViews.addBlock(PlacedPiece(move.second.piece, destination))
                                    val firstBlock = boardViews.addBlock(PlacedPiece(move.first.piece, destination))
                                    secondBlock.move(this, destination, move.second.square)
                                    firstBlock.move(this, destination, move.first.square)
                                }
                            }
                        }
                    }
                }
            }
            block {
                onEnd()
            }
        }
    }

    private fun Block.move(animator: Animator, from: Square, to: Square) {
        if (animateViews) animator.apply {
            this@move.moveTo(to.positionX(), to.positionY(), 0.15.seconds, Easing.LINEAR)
        }
        boardViews[from] = null
        boardViews[to] = this
    }

    private fun animateResultingBlock(animator: Animator, block: Block) {
        val x = block.x
        val y = block.y
        val scale = block.scale
        val scaleChange = 0.1
        val shift = block.scaledWidth * scaleChange / 2
        animator.tween(
                block::x[x - shift],
                block::y[y - shift],
                block::scale[scale + scaleChange],
                time = 0.1.seconds,
                easing = Easing.LINEAR
        )
        animator.tween(
                block::x[x],
                block::y[y],
                block::scale[scale],
                time = 0.1.seconds,
                easing = Easing.LINEAR
        )
    }
}
