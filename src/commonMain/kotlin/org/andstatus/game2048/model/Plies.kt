package org.andstatus.game2048.model

import com.soywiz.korio.serialization.json.toJson

/** @author yvolk@yurivolkov.com */
class Plies(private val plies: List<Ply>) {
    val size: Int get() = plies.size

    operator fun get(index: Int): Ply = plies[index]

    operator fun plus(ply: Ply): Plies = Plies(plies + ply)

    fun take(n: Int): Plies = Plies(plies.take(n))

    fun drop(n: Int): Plies = Plies(plies.drop(n))

    fun isNotEmpty(): Boolean = plies.isNotEmpty()

    fun lastOrNull(): Ply? = plies.lastOrNull()

    fun toLongString(): String = plies.mapIndexed { ind, playerMove ->
        "\n" + (ind + 1).toString() + ":" + playerMove
    }.toString()

    companion object {
        fun StringBuilder.appendLastPlies(plies: Plies): StringBuilder = apply {
            plies.plies.forEach { ply ->
                ply.toMap().toJson()
                    .let { json -> append(json) }
            }
        }
    }
}