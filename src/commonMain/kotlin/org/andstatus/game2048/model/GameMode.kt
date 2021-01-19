package org.andstatus.game2048.model

import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import kotlin.math.abs

/** @author yvolk@yurivolkov.com */
class GameMode() {
    private data class GameModeData(val modeEnum: GameModeEnum, val speed: Int)

    val maxSpeed = 6
    private val data = KorAtomicRef(initialData())

    fun stop() {
        val old = data.value
        data.compareAndSet(old, GameModeData(GameModeEnum.STOP, 0))
    }

    val speed get() = data.value.speed
    val absSpeed get() = abs(data.value.speed)

    val autoPlaying get() = modeEnum == GameModeEnum.BACKWARDS ||
            modeEnum == GameModeEnum.FORWARD ||
            modeEnum == GameModeEnum.AI_PLAY

    var modeEnum : GameModeEnum
        get() = data.value.modeEnum
        set(value) {
            data.value = GameModeData(
                value,
                when(value) {
                    GameModeEnum.BACKWARDS -> -1
                    GameModeEnum.FORWARD -> 1
                    GameModeEnum.STOP, GameModeEnum.PLAY, GameModeEnum.AI_PLAY -> 0
                }
            )
        }

    private fun initialData() = GameModeData(GameModeEnum.STOP, 0)

    fun incrementSpeed() {
        val value = data.value
        if (value.speed < maxSpeed) {
            val newSpeed = value.speed + 1
            val newMode = when (newSpeed) {
                in Int.MIN_VALUE .. -1 -> GameModeEnum.BACKWARDS
                0 -> GameModeEnum.STOP
                else -> GameModeEnum.FORWARD
            }
            data.compareAndSet(value, GameModeData(newMode, newSpeed))
        }
    }

    fun decrementSpeed() {
        val value = data.value
        if (value.speed > -maxSpeed) {
            val newSpeed = value.speed - 1
            val newMode = when (newSpeed) {
                in Int.MIN_VALUE .. -1 -> GameModeEnum.BACKWARDS
                0 -> GameModeEnum.STOP
                else -> GameModeEnum.FORWARD
            }
            data.compareAndSet(value, GameModeData(newMode, newSpeed))
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