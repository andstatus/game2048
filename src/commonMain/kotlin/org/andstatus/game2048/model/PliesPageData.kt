package org.andstatus.game2048.model

import korlibs.io.concurrent.atomic.KorAtomicRef
import korlibs.io.serialization.json.Json
import korlibs.io.serialization.json.toJson
import org.andstatus.game2048.MyContext
import org.andstatus.game2048.initAtomicReference
import org.andstatus.game2048.myLog
import org.andstatus.game2048.update
import kotlin.math.abs

private const val keyPlies = "plies"

class PliesPageData(val myContext: MyContext) {
    init {
        pliesMapRef.value = emptyMap()
    }

    fun readPlies(
        shortRecord: ShortRecord,
        pageNumber: Int,
        readerIn: SequenceLineReader,
        readAll: Boolean
    ): List<Ply> {
        if (shortRecord.isStub) return emptyList()

        val list: MutableList<Ply> = ArrayList()
        val storageKey = storageKey(shortRecord.id, pageNumber)
        val reader: SequenceLineReader = if (readerIn.isEmpty) {
            myContext.storage.getOrNull(storageKey)
                ?.let { SequenceLineReader(sequenceOf(it)) } ?: emptySequenceLineReader
        } else readerIn
        reader.readNext { strReader ->
            while (strReader.hasMore && (readAll || list.size < myContext.pliesPageSize)) {
                Json.parse(strReader)
                    ?.let { Ply.fromJson(shortRecord.board, it) }
                    ?.let { list.add(it) }
            }
        }.onFailure {
            myLog("Failed to parse page $pageNumber, loaded ${list.size} plies, pos:${reader.posFromInput}: $it")
        }.onSuccess {
            myLog(
                "Loaded ${list.size} plies of $storageKey from " +
                    if (readerIn.isEmpty) "storage" else readerIn::class.simpleName
            )
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
                map.keys.maxByOrNull { key -> abs(key - mapKey) }?.let { keyToDelete ->
                    myLog("Removing key $keyToDelete to store $mapKey")
                    map.remove(keyToDelete)
                }
            }
            map
        }
    }

    fun save(shortRecord: ShortRecord, pageNumber: Int) {
        if (shortRecord.isStub) return

        if (isLoaded(shortRecord.id, pageNumber)) {
            val plies = getPlies(shortRecord.id, pageNumber)
            if (plies.isEmpty()) {
                shortRecord.myContext.storage.remove(storageKey(shortRecord.id, pageNumber))
            } else {
                shortRecord.myContext.storage[storageKey(shortRecord.id, pageNumber)] = toJson(plies)
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
        return myContext.storage.remove(storageKey).also {
            val old = myContext.storage.getOrNull(storageKey)
            if (old != null) {
                myLog("Still stored after removal $storageKey: $old")
            }
        }
    }

    companion object {
        private const val maxPagesStored = 5
        private val pliesMapRef: KorAtomicRef<Map<Int, List<Ply>>> = initAtomicReference(emptyMap())

        fun isLoaded(gameId: Int, pageNumber: Int): Boolean = pliesMapRef.value[mapKey(gameId, pageNumber)] != null

        fun getPlies(gameId: Int, pageNumber: Int): List<Ply> =
            pliesMapRef.value[mapKey(gameId, pageNumber)] ?: emptyList()

        private fun storageKey(gameId: Int, pageNumber: Int): String = "$keyPlies${gameId}.$pageNumber"

        private fun mapKey(gameId: Int, pageNumber: Int): Int = gameId * 1000 + pageNumber

        fun toJson(plies: List<Ply>): String = StringBuilder().also { stringBuilder ->
            plies.forEach { ply ->
                ply.toMap().toJson()
                    .let { json -> stringBuilder.append(json) }
            }
        }.toString()
    }
}
