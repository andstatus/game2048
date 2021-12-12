package org.andstatus.game2048.model

import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.util.StrReader
import org.andstatus.game2048.Settings
import org.andstatus.game2048.initAtomicReference
import org.andstatus.game2048.model.PliesPage.Companion.keyPliesPage
import org.andstatus.game2048.myLog

private const val keyPliesHead = "pliesHead"
private const val keyPlayersMoves = "playersMoves"

/** @author yvolk@yurivolkov.com */
class GamePlies(private val shortRecord: ShortRecord, private val reader: StrReader? = null) {

    private constructor(shortRecord: ShortRecord, pages: List<PliesPage>) : this(shortRecord, null) {
        if (pages.isNotEmpty()) {
            pagesRef.value = pages
            load()
        }
    }

    private val emptyFirstPage = PliesPage(shortRecord, 1, 1, 0, null)
    private val pagesRef: KorAtomicRef<List<PliesPage>> = initAtomicReference(listOf(emptyFirstPage))
    private val pages: List<PliesPage> get() = pagesRef.value
    val lastPage get() = pages.last()

    val notCompleted: Boolean get() = !pliesLoaded.isInitialized()
    private val pliesLoaded: Lazy<Boolean> = lazy {
        if (reader != null) {
            var pageNumber = 1
            var plyNumber = 1
            while (reader.hasMore) {
                val page = PliesPage.fromSharedJson(shortRecord, pageNumber, plyNumber, reader)
                pagesRef.value = if (pageNumber == 1) listOf(page) else pagesRef.value + page
                pageNumber += 1
                plyNumber += page.size
            }
        }
        true
    }

    fun load() = pliesLoaded.value

    val size: Int get() = lastPage.nextPageFirstPlyNumber - 1

    operator fun get(num: Int): Ply {
        pages.forEach {
            if (num < it.nextPageFirstPlyNumber) {
                return it.plies[num - it.firstPlyNumber]  // Numbers start with plyNumber
            }
        }
        throw IllegalArgumentException("No ply with index:$num found. " + toLongString())
    }

    operator fun plus(ply: Ply): GamePlies {
        with(lastPage) {
            val pagesNew = if (plies.size < shortRecord.settings.pliesPageSize) {
                pages.take(pages.size - 1) + plus(ply)
            } else {
                pages + PliesPage(shortRecord, pageNumber + 1, nextPageFirstPlyNumber, 1, listOf(ply))
            }
            return GamePlies(shortRecord, pagesNew)
        }
    }

    fun take(n: Int): GamePlies = when {
        (n < 1) -> GamePlies(shortRecord, listOf(emptyFirstPage))
        else -> pages.fold(this) { acc, it ->
            if (n >= it.firstPlyNumber &&  n < it.nextPageFirstPlyNumber) {
                GamePlies(shortRecord, pages.take(it.pageNumber - 1) + it.take(n - it.firstPlyNumber))
            } else acc
        }
    }. also { newGamePlies ->
        deletePagesStartingWith(shortRecord.settings, shortRecord.id, newGamePlies.pages.size + 1)
    }

    fun lastOrNull(): Ply? = lastPage.plies.lastOrNull()

    fun toLongString(): String = toShortString() + " " + lastPage.toLongString()

    fun save() {
        shortRecord.settings.storage[keyHead(shortRecord.id)] =
            pages.map { it.toHeaderMap() }.asJsonArray().toString()
        pages.forEach { it.save() }
    }

    fun toSharedJson(): String = StringBuilder().also { stringBuilder ->
        pages.forEach {
            it.toJson().let { json -> stringBuilder.append(json) }
        }
    }.toString()

    fun toShortString(): String {
        return "${size} plies in ${pages.size} pages" + if (notCompleted) ", loading..." else ""
    }

    companion object {

        fun fromId(shortRecord: ShortRecord): GamePlies =
            shortRecord.settings.storage.getOrNull(keyHead(shortRecord.id))?.asJsonArray()
                ?.mapIndexed { index, headerJson ->
                    PliesPage.fromId(shortRecord, index + 1, headerJson as String)
                }
                ?.let { GamePlies(shortRecord, it) }
                ?: shortRecord.settings.storage.getOrNull(keyGame + shortRecord.id)?.let {
                    fromSharedJson(shortRecord, it)
                }
                ?: GamePlies(shortRecord, null)

        fun fromPlies(shortRecord: ShortRecord, plies: List<Ply>): GamePlies {
            var pages: List<PliesPage> = emptyList()
            var index = 0
            var firstPlyNumber = 1
            while(index < plies.size) {
                when {
                    plies.size - index <= shortRecord.settings.pliesPageSize -> {
                        PliesPage(shortRecord, pages.size + 1, firstPlyNumber,
                            plies.size - index, plies.drop(index))
                    }
                    else -> {
                        PliesPage(shortRecord, pages.size + 1, firstPlyNumber,
                            shortRecord.settings.pliesPageSize, plies.drop(index).take(shortRecord.settings.pliesPageSize))
                    }
                }.also {
                    firstPlyNumber += it.size
                    index += it.size
                    it.save()
                    pages = pages + it
                }
            }
            return GamePlies(shortRecord, pages).let { gamePlies1 ->
                myLog("Loaded ${gamePlies1.toShortString()}")
                gamePlies1.save()
                when{
                    (gamePlies1.pages.size > 1) ->
                            // Effectively free memory of previous pages
                            fromId(shortRecord).also { gamePlies ->
                                myLog("Reloaded multipage ${gamePlies.toShortString()}")
                            }
                    else -> gamePlies1
                }
            }
        }

        fun fromSharedJson(shortRecord: ShortRecord, json: String): GamePlies {
            val reader = StrReader(json)
            val aMap: Map<String, Any> = reader.asJsonMap()
            return if (aMap.containsKey(keyPlayersMoves)) {
                // TODO: For compatibility with previous versions
                (aMap[keyPlayersMoves]?.asJsonArray()
                    ?.mapNotNull { Ply.fromJson(shortRecord.board, it) }
                    ?: emptyList())
                    .let { fromPlies(shortRecord, it) }
            }
            else {
                GamePlies(shortRecord, reader)
            }.also {
                if (it.size > shortRecord.finalPosition.plyNumber) {
                    // Fix for older versions, which didn't store move number
                    shortRecord.finalPosition.plyNumber = it.size
                }
            }
        }

        fun delete(settings: Settings, id: Int) {
            if (settings.storage.remove(keyHead(id))) {
                deletePagesStartingWith(settings, id, 1)
            }
        }

        private fun deletePagesStartingWith(settings: Settings, id: Int, pageNumber: Int) {
            var pageNumber1 = pageNumber
            while (settings.storage.remove(keyPliesPage(id, pageNumber1))) {
                pageNumber1 += 1
            }
        }

        private fun keyHead(id: Int) = keyPliesHead + id

    }
}
