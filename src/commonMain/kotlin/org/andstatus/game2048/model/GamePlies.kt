package org.andstatus.game2048.model

import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.serialization.json.toJson
import com.soywiz.korio.util.StrReader
import org.andstatus.game2048.Settings
import org.andstatus.game2048.compareAndSetFixed
import org.andstatus.game2048.initAtomicReference
import org.andstatus.game2048.myLog

private const val keyPlayersMoves = "playersMoves"
private const val keyPly = "ply"

/** @author yvolk@yurivolkov.com */
class GamePlies(private val shortRecord: ShortRecord, iPlies: List<Ply>?, private val reader: StrReader?) {

    constructor(shortRecord: ShortRecord, plies: List<Ply>) : this(shortRecord, plies, null) {
        load()
    }

    private val pliesRef: KorAtomicRef<List<Ply>?> = initAtomicReference(iPlies)
    private val plies: List<Ply> get() = pliesRef.value ?: emptyList()

    private fun plyPageKey(pageNumber: Int): String = "$keyPly${shortRecord.id}.$pageNumber".also {
        if (shortRecord.id < 1 || pageNumber < 1) throw IllegalArgumentException(it)
    }

    val notCompleted: Boolean get() = !pliesLoaded.isInitialized()
    private val pliesLoaded: Lazy<Boolean> = lazy {
        if (reader != null) {
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
            pliesRef.compareAndSetFixed(null, list)
        }
        true
    }

    fun load() = pliesLoaded.value

    val size: Int get() = plies.size

    operator fun get(index: Int): Ply = plies[index]

    operator fun plus(ply: Ply): GamePlies = GamePlies(shortRecord, plies + ply)

    fun take(n: Int): GamePlies = GamePlies(shortRecord, plies.take(n))

    fun drop(n: Int): GamePlies = GamePlies(shortRecord, plies.drop(n))

    fun isNotEmpty(): Boolean = notCompleted || plies.isNotEmpty()

    fun lastOrNull(): Ply? = plies.lastOrNull()

    fun toLongString(): String = plies.mapIndexed { ind, playerMove ->
        "\n" + (ind + 1).toString() + ":" + playerMove
    }.toString()

    companion object {
        fun StringBuilder.appendPlies(gamePlies: GamePlies): StringBuilder = apply {
            gamePlies.load()
            gamePlies.plies.forEach { ply ->
                ply.toMap().toJson()
                    .let { json -> append(json) }
            }
        }

        fun fromId(settings: Settings, shortRecord: ShortRecord): GamePlies {
            val json = settings.storage.getOrNull(keyGame + shortRecord.id) ?: return GamePlies(
                shortRecord,
                emptyList()
            )
            return fromSharedJson(json, shortRecord)
        }

        fun fromSharedJson(json: String, shortRecord: ShortRecord): GamePlies {
            val reader = StrReader(json)
            val aMap: Map<String, Any> = reader.asJsonMap()
            return if (aMap.containsKey(keyPlayersMoves))
            // TODO: For compatibility with previous versions
                (aMap[keyPlayersMoves]?.asJsonArray()
                    ?.mapNotNull { Ply.fromJson(shortRecord.board, it) }
                    ?: emptyList())
                    .let { GamePlies(shortRecord, it) }
            else {
                GamePlies(shortRecord, null, reader)
            }.also {
                if (it.size > shortRecord.finalPosition.plyNumber) {
                    // Fix for older versions, which didn't store move number
                    shortRecord.finalPosition.plyNumber = it.size
                }
            }
        }

    }
}
