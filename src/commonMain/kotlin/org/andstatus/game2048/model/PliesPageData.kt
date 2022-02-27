package org.andstatus.game2048.model

import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.serialization.json.toJson
import com.soywiz.korio.util.StrReader
import org.andstatus.game2048.Settings
import org.andstatus.game2048.initAtomicReference
import org.andstatus.game2048.myLog
import org.andstatus.game2048.stubGameId
import org.andstatus.game2048.update
import kotlin.math.abs

private const val keyPlies = "plies"

class PliesPageData(val settings: Settings) {
    init {
        pliesMapRef.value = emptyMap()
    }

    fun readPlies(
        shortRecord: ShortRecord,
        pageNumber: Int,
        readerIn: StrReader?,
        readAll: Boolean
    ): List<Ply> {
        if (shortRecord.isStub) return emptyList()

        val list: MutableList<Ply> = ArrayList()
        val storageKey = storageKey(shortRecord.id, pageNumber)
        val reader: StrReader = readerIn
            ?: settings.storage.getOrNull(storageKey)
                ?.let { StrReader(it) }
            ?: StrReader("")

        try {
            while (reader.hasMore && (readAll || list.size < settings.pliesPageSize)) {
                Json.parse(reader)
                    ?.let { Ply.fromJson(shortRecord.board, it) }
                    ?.let { list.add(it) }
            }
            myLog("Loaded ${list.size} plies from $storageKey")
        } catch (e: Throwable) {
            myLog("Failed to load ply ${list.size + 1}, at pos:${reader.pos}: $e")
            reader.skip(reader.available)
        }
        update(shortRecord, pageNumber, list)
        return list
    }

    fun update(
        shortRecord: ShortRecord,
        pageNumber: Int,
        list: List<Ply>
    ) {
        pliesMapRef.update {
            val map: MutableMap<Int, List<Ply>> = HashMap(it)
            val mapKey = mapKey(shortRecord.id, pageNumber)
            map[mapKey] = list
            if (map.size > maxPagesStored) {
                val pageToDelete = map.keys.maxOf { key -> abs(key - mapKey) }
                map.remove(pageToDelete)
            }
            map
        }
    }

    fun save(shortRecord: ShortRecord, pageNumber: Int) {
        if (shortRecord.isStub) return

        if (isLoaded(shortRecord.id, pageNumber)) {
            val plies = getPlies(shortRecord.id, pageNumber)
            if (plies.isEmpty()) {
                shortRecord.settings.storage.remove(storageKey(shortRecord.id, pageNumber))
            } else {
                shortRecord.settings.storage[storageKey(shortRecord.id, pageNumber)] = toJson(plies)
            }
        }
    }

    /**
     * @return true if the page was found in storage
     */
    fun remove(gameId: Int, pageNumber: Int): Boolean {
        pliesMapRef.update {
            val map: MutableMap<Int, List<Ply>> = HashMap(it)
            map.remove(mapKey(gameId, pageNumber))
            map
        }
        val storageKey = storageKey(gameId, pageNumber)
        return settings.storage.remove(storageKey).also {
            val old = settings.storage.getOrNull(storageKey)
            if (old != null) {
                myLog("Still stored after removal $storageKey: $old")
            }
        }
    }

    companion object {
        private const val maxPagesStored = 5
        private val pliesMapRef: KorAtomicRef<Map<Int, List<Ply>>> = initAtomicReference(emptyMap())

        fun isLoaded(gameId: Int, pageNumber: Int): Boolean = pliesMapRef.value[mapKey(gameId, pageNumber)] != null

        fun getPlies(gameId: Int, pageNumber: Int): List<Ply> = pliesMapRef.value[mapKey(gameId, pageNumber)] ?: emptyList()

        private fun storageKey(gameId: Int, pageNumber: Int): String = "$keyPlies${gameId}.$pageNumber"

        private fun mapKey(gameId: Int, pageNumber: Int): Int = gameId * stubGameId + pageNumber

        fun toJson(plies: List<Ply>): String = StringBuilder().also { stringBuilder ->
            plies.forEach { ply ->
                ply.toMap().toJson()
                    .let { json -> stringBuilder.append(json) }
            }
        }.toString()
    }
}
