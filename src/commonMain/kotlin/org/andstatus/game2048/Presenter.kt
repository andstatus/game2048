package org.andstatus.game2048

import com.soywiz.klock.seconds
import com.soywiz.korge.animate.Animator
import com.soywiz.korge.animate.animateSequence
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.Stage
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.concurrent.atomic.KorAtomicBoolean
import com.soywiz.korma.interpolation.Easing
import kotlin.random.Random

class Presenter(val stage: Stage) {
    private val moveIsInProgress = KorAtomicBoolean(false)
    var boardViews = BoardViews(stage, settings.boardWidth, settings.boardHeight)

    fun placeRandomBlock() {
        val newBoard = board.copy()
        val square = newBoard.getRandomFreeSquare() ?: return

        val piece = if (Random.nextDouble() < 0.9) Piece.N2 else Piece.N4
        newBoard[square] = piece
        boardViews[square] = Block(piece).addTo(stage, square)
        board = newBoard
    }

    fun moveBlocksTo(direction: Direction) {
        if (!moveIsInProgress.compareAndSet(expect = false, update = true)) return

        if (board.noMoreMoves()) {
            showGameOver(stage) {
                restart()
                computersMove(stage)
                moveIsInProgress.value = false
            }
        } else {
            val (newBoard, moves) = moveBlocksOnTheBoard(board, direction)
            if (moves.isEmpty() && !settings.allowUsersMoveWithoutBlockMoves) {
                moveIsInProgress.value = false
            } else {
                board = newBoard
                animateMoves(stage, moves) {
                    val points = moves.fold(0) { acc, move ->
                        acc + (move.second?.piece?.value ?: 0)
                    }
                    score.update(score.value + points)
                    computersMove(stage)
                    moveIsInProgress.value = false
                }
            }
        }
    }

    private fun animateMoves(stage: Stage, moves: List<Move>, onEnd: () -> Unit) = stage.launchImmediately {
        stage.animateSequence {
            parallel {
                moves.forEach { move ->
                    val firstBlock = boardViews.get(move.first.square)
                    if (move.second == null) {
                        firstBlock?.move(this, move.first.square, move.destination)
                    } else {
                        val secondBlock = boardViews.get(move.second.square)
                        sequence {
                            parallel {
                                firstBlock?.move(this, move.first.square, move.destination)
                                secondBlock?.move(this, move.second.square, move.destination)
                            }
                            block {
                                firstBlock?.removeFromParent()
                                secondBlock?.removeFromParent()
                                board[move.destination]?.let { boardViews.addBlock(PlacedPiece(it, move.destination)) }
                            }
                            sequenceLazy {
                                boardViews[move.destination]?.let { animateResultingBlock(this, it) }
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
        animator.apply {
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
