package org.andstatus.game2048.model

import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import kotlin.math.abs

/** @author yvolk@yurivolkov.com */
class GameMode() {
    private data class GameModeData(val modeEnum: GameModeEnum, val speed: Int, val aiEnabled: Boolean)

    val maxSpeed = 6
    private val data = KorAtomicRef(initialData())

    fun stop() {
        val old = data.value
        data.compareAndSet(old, GameModeData(GameModeEnum.STOP, 0, old.aiEnabled))
    }

    val speed get() = data.value.speed
    val absSpeed get() = abs(data.value.speed)

    val autoPlaying get() = modeEnum == GameModeEnum.BACKWARDS ||
            modeEnum == GameModeEnum.FORWARD ||
            modeEnum == GameModeEnum.AI_PLAY

    var modeEnum : GameModeEnum
        get() = data.value.modeEnum
        set(value) {
            val old = data.value
            data.value = GameModeData(
                value,
                when(value) {
                    GameModeEnum.BACKWARDS -> -1
                    GameModeEnum.FORWARD -> 1
                    GameModeEnum.STOP, GameModeEnum.PLAY, GameModeEnum.AI_PLAY -> 0
                },
                old.aiEnabled
            )
        }

    var aiEnabled: Boolean
        get() = data.value.aiEnabled
        set(value) {
            val old = data.value
            data.compareAndSet(old, GameModeData(old.modeEnum, old.speed, value))
        }

    private fun initialData() = GameModeData(GameModeEnum.STOP, 0, false)

    fun incrementSpeed() {
        val old = data.value
        if (old.speed < maxSpeed) {
            val newSpeed = old.speed + 1
            val newMode = newGameMode(newSpeed)
            data.compareAndSet(old, GameModeData(newMode, newSpeed, old.aiEnabled))
        }
    }

    fun decrementSpeed() {
        val old = data.value
        if (old.speed > -maxSpeed) {
            val newSpeed = old.speed - 1
            val newMode = newGameMode(newSpeed)
            data.compareAndSet(old, GameModeData(newMode, newSpeed, old.aiEnabled))
        }
    }

    private fun newGameMode(newSpeed: Int) = when (modeEnum) {
        GameModeEnum.AI_PLAY -> when (newSpeed) {
            in Int.MIN_VALUE..0 -> GameModeEnum.PLAY
            else -> modeEnum
        }
        else -> when (newSpeed) {
            in Int.MIN_VALUE..-1 -> GameModeEnum.BACKWARDS
            0 -> GameModeEnum.STOP
            else -> GameModeEnum.FORWARD
        }
    }

    val delayMs get() = when(absSpeed){
        in Int.MIN_VALUE .. 1 -> 500
        1 -> 250
        2 -> 125
        3 -> 64
        4 -> 32
        5 -> 16
        else -> 1
    }

    val moveMs get() = when(absSpeed){
        in Int.MIN_VALUE .. 1 -> 150
        1 -> 75
        2 -> 37
        3 -> 18
        4 -> 9
        5 -> 4
        else -> 1
    }

    val resultingBlockMs get() = when(absSpeed){
        in Int.MIN_VALUE .. 1 -> 100
        1 -> 50
        2 -> 28
        3 -> 15
        4 -> 8
        5 -> 4
        else -> 1
    }
}