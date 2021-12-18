package org.andstatus.game2048.model

import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.serialization.json.toJson
import com.soywiz.korio.util.StrReader
import org.andstatus.game2048.compareAndSetFixed
import org.andstatus.game2048.initAtomicReference
import org.andstatus.game2048.myLog

private const val keyPageNumber = "page"
private const val keyFirstPlyNumber = "first"
private const val keyCount = "count"
private const val keyPlies = "plies"

class PliesPage(
    val shortRecord: ShortRecord,
    val pageNumber: Int,
    val firstPlyNumber: Int,
    val count: Int,
    iPlies: List<Ply>?
) {

    val nextPageFirstPlyNumber get() = firstPlyNumber + size

    private val keyPliesPage: String = keyPliesPage(shortRecord.id, pageNumber).also {
        if (shortRecord.id < 1 || pageNumber < 1) throw IllegalArgumentException(it)
    }

    val loaded get() = pliesRef.value != null
    val saved get() = savedRef.value
    private val savedRef = korAtomic(false)

    private val pliesRef: KorAtomicRef<List<Ply>?> = initAtomicReference(iPlies)
    val plies: List<Ply> get() = pliesRef.value ?: emptyList()

    private val pliesLoaded: Lazy<Boolean> = lazy {
        if (pliesRef.value == null) {
            val reader = shortRecord.settings.storage.getOrNull(keyPliesPage)?.let { StrReader(it) }
            readPlies(shortRecord, pageNumber, reader, true).let {
                savedRef.value = true
                pliesRef.compareAndSetFixed(null, it)
            }
        }
        true
    }

    fun load() = pliesLoaded.value.let { this }

    val size: Int get() = if (loaded) plies.size else count

    operator fun get(index: Int): Ply = plies[index]

    operator fun plus(ply: Ply): PliesPage = PliesPage(
        shortRecord, pageNumber, firstPlyNumber, plies.size + 1,
        plies + ply
    )

    fun take(n: Int): PliesPage = with(load()) {
        PliesPage(shortRecord, pageNumber, firstPlyNumber, n, plies.take(n))
    }

    fun toLongString(): String = plies.mapIndexed { ind, playerMove ->
        "\n" + (ind + 1).toString() + ":" + playerMove
    }.toString()

    fun save() {
        if (shortRecord.isStub) return

        if (loaded && savedRef.compareAndSet(false, true)) {
            if (count == 0) {
                shortRecord.settings.storage.remove(keyPliesPage)
            } else {
                shortRecord.settings.storage[keyPliesPage] = toJson()
            }
        }
    }

    fun toHeaderMap(): Map<String, Any> = mapOf(
        keyPageNumber to pageNumber,
        keyFirstPlyNumber to firstPlyNumber,
        keyCount to count
    )

    fun toJson(): String = StringBuilder().also { stringBuilder ->
        load().plies.forEach { ply ->
            ply.toMap().toJson()
                .let { json -> stringBuilder.append(json) }
        }
    }.toString()

    companion object {
        fun keyPliesPage(gameId: Int, pageNumber: Int) = "$keyPlies$gameId.$pageNumber"

        fun fromSharedJson(shortRecord: ShortRecord, pageNumber: Int, plyNumber: Int, reader: StrReader?): PliesPage =
            readPlies(shortRecord, pageNumber, reader, false).let {
                PliesPage(shortRecord, pageNumber, plyNumber, it.size, it)
            }.apply { load() }

        fun fromId(shortRecord: ShortRecord, pageNumberIn: Int, jsonHeader: Any): PliesPage =
            jsonHeader.parseJsonMap().let {
                val pageNumber: Int = it[keyPageNumber] as Int? ?: 0
                val plyNumber: Int = it[keyFirstPlyNumber] as Int? ?: 0
                val count: Int = it[keyCount] as Int? ?: 0
                if (pageNumber != pageNumberIn) throw IllegalArgumentException("Wrong page number stored: $pageNumber at $pageNumberIn position")
                PliesPage(shortRecord, pageNumber, plyNumber, count, null)
            }

        private fun readPlies(
            shortRecord: ShortRecord,
            pageNumber: Int,
            reader: StrReader?,
            readAll: Boolean
        ): List<Ply> {
            val list: MutableList<Ply> = ArrayList()
            if (reader != null && reader.hasMore) {
                try {
                    while (reader.hasMore && (readAll || list.size < shortRecord.settings.pliesPageSize)) {
                        Json.parse(reader)
                            ?.let { Ply.fromJson(shortRecord.board, it) }
                            ?.let { list.add(it) }
                    }
                    myLog("Loaded ${list.size} plies into page $pageNumber")
                } catch (e: Throwable) {
                    myLog("Failed to load ply ${list.size + 1}, at pos:${reader.pos}: $e")
                    reader.skip(reader.available)
                }
            }
            return list
        }

    }
}
