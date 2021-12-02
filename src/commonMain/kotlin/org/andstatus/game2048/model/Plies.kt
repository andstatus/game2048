package org.andstatus.game2048.model

import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.serialization.json.toJson
import com.soywiz.korio.util.StrReader
import org.andstatus.game2048.myLog

/** @author yvolk@yurivolkov.com */
class Plies(iPlies: List<Ply>?, private val shortRecord: GameRecord.ShortRecord?, private val reader: StrReader?) {

    constructor(plies: List<Ply>): this(plies, null, null) {
        load()
    }

    private val pliesRef: KorAtomicRef<List<Ply>?> = korAtomic(iPlies)
    private val plies: List<Ply> get() = pliesRef.value ?: emptyList()

    val notCompleted: Boolean get() = !pliesLoaded.isInitialized()
    private val pliesLoaded: Lazy<Boolean> = lazy {
        if (shortRecord != null && reader != null) {
            val list: MutableList<Ply> = ArrayList()
            try {
                while (reader.hasMore) {
                    Json.parse(reader)
                        ?.let { Ply.fromJson(shortRecord.board, it) }
                        ?.let { list.add(it) }
                }
                myLog("Loaded ${list.size} plies")
            } catch (e: Throwable) {
                myLog("Failed to load ply ${list.size + 1}, at pos:${reader.pos}: $e")
            }
            pliesRef.compareAndSet(null, list)
        }
        true
    }

    fun load() = pliesLoaded.value

    val size: Int get() = plies.size

    operator fun get(index: Int): Ply = plies[index]

    operator fun plus(ply: Ply): Plies = Plies(plies + ply)

    fun take(n: Int): Plies = Plies(plies.take(n))

    fun drop(n: Int): Plies = Plies(plies.drop(n))

    fun isNotEmpty(): Boolean = notCompleted || plies.isNotEmpty()

    fun lastOrNull(): Ply? = plies.lastOrNull()

    fun toLongString(): String = plies.mapIndexed { ind, playerMove ->
        "\n" + (ind + 1).toString() + ":" + playerMove
    }.toString()

    companion object {
        fun StringBuilder.appendPlies(plies: Plies): StringBuilder = apply {
            plies.load()
            plies.plies.forEach { ply ->
                ply.toMap().toJson()
                    .let { json -> append(json) }
            }
        }

    }
}
