package org.andstatus.game2048.model

import com.soywiz.korio.concurrent.atomic.KorAtomicRef
import com.soywiz.korio.util.StrReader
import org.andstatus.game2048.Settings
import org.andstatus.game2048.initAtomicReference

private const val keyPlayersMoves = "playersMoves"

/** @author yvolk@yurivolkov.com */
class GamePlies(private val shortRecord: ShortRecord, private val reader: StrReader? = null) {

    private constructor(shortRecord: ShortRecord, pages: List<PliesPage>) : this(shortRecord, null) {
        pagesRef.value = pages
        load()
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
        pages.forEach { it.load() }
        true
    }

    fun load() = pliesLoaded.value

    val size: Int get() = pages.sumOf { it.size }

    operator fun get(index: Int): Ply {
        pages.forEach {
            if (index < it.nextPagePlyNumber) {
                return it.plies[index - it.plyNumber]
            }
        }
        throw IllegalArgumentException("No ply with index:$index found. " + toLongString())
    }

    operator fun plus(ply: Ply): GamePlies {
        with(lastPage) {
            val pagesNew = if (plies.size < shortRecord.settings.pliesPageSize) {
                pages.take(pages.size - 1) + plus(ply)
            } else {
                pages + PliesPage(shortRecord, pageNumber + 1, nextPagePlyNumber, 1, listOf(ply))
            }
            return GamePlies(shortRecord, pagesNew)
        }
    }

    fun take(n: Int): GamePlies {
        pages.forEach {
            if (n < it.nextPagePlyNumber) {
                return GamePlies(shortRecord, pages.take(it.pageNumber - 1) + it.take(n - it.plyNumber))
            }
        }
        return this
    }

    fun isNotEmpty(): Boolean = notCompleted || lastPage.plies.isNotEmpty()

    fun lastOrNull(): Ply? = lastPage.plies.lastOrNull()

    fun toLongString(): String = (if(pages.size > 1) "${pages.size} pages ..." else "") + lastPage.toLongString()

    fun save() = pages.forEach { it.save() }

    fun toSharedJson(): String = StringBuilder().also { stringBuilder ->
        pages.forEach {
            it.toJson().let { json -> stringBuilder.append(json) }
        }
    }.toString()

    companion object {

        fun fromId(settings: Settings, shortRecord: ShortRecord): GamePlies {
            val json = settings.storage.getOrNull(keyGame + shortRecord.id)
                ?: return GamePlies(shortRecord, null)
            return fromSharedJson(json, shortRecord)
        }

        fun fromPlies(shortRecord: ShortRecord, plies: List<Ply>): GamePlies =
            GamePlies(shortRecord, listOf(PliesPage(shortRecord, 1, 1, plies.size, plies)))

        fun fromSharedJson(json: String, shortRecord: ShortRecord): GamePlies {
            val reader = StrReader(json)
            val aMap: Map<String, Any> = reader.asJsonMap()
            return if (aMap.containsKey(keyPlayersMoves))
            // TODO: For compatibility with previous versions
                (aMap[keyPlayersMoves]?.asJsonArray()
                    ?.mapNotNull { Ply.fromJson(shortRecord.board, it) }
                    ?: emptyList())
                    .let { fromPlies(shortRecord, it) }
            else {
                GamePlies(shortRecord, reader)
            }.also {
                if (it.size > shortRecord.finalPosition.plyNumber) {
                    // Fix for older versions, which didn't store move number
                    shortRecord.finalPosition.plyNumber = it.size
                }
            }
        }

    }
}