package org.andstatus.game2048.model

import com.soywiz.korio.concurrent.atomic.korAtomic
import com.soywiz.korio.util.StrReader

private const val keyPageNumber = "page"
private const val keyFirstPlyNumber = "first"
private const val keyCount = "count"

class PliesPage(
    val shortRecord: ShortRecord,
    val pageNumber: Int,
    val firstPlyNumber: Int,
    val count: Int,
    iPlies: List<Ply>?,
    val isFirstEmpty: Boolean = false
) {
    init {
        iPlies?.let {
            if (!isFirstEmpty) {
                shortRecord.settings.pliesPageData.update(shortRecord, pageNumber, it)
            }
        }
    }

    val nextPageFirstPlyNumber get() = firstPlyNumber + size

    val loaded get() = isFirstEmpty || PliesPageData.isLoaded(shortRecord.id, pageNumber)
    val saved get() = savedRef.value
    private val savedRef = korAtomic(isFirstEmpty)

    val plies: List<Ply> get() = if (isFirstEmpty) emptyList() else PliesPageData.getPlies(shortRecord.id, pageNumber)

    fun load(): PliesPage {
        if (!isFirstEmpty && !loaded) {
            shortRecord.settings.pliesPageData.readPlies(shortRecord, pageNumber, null, true)
            savedRef.value = true
        }
        return this
    }

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

    fun save(): PliesPage {
        if (loaded && savedRef.compareAndSet(false, true)) {
            shortRecord.settings.pliesPageData.save(shortRecord, pageNumber)
        }
        return this
    }

    fun toHeaderMap(): Map<String, Any> = mapOf(
        keyPageNumber to pageNumber,
        keyFirstPlyNumber to firstPlyNumber,
        keyCount to count
    )

    fun toJson(): String = PliesPageData.toJson(PliesPageData.getPlies(shortRecord.id, pageNumber))

    companion object {
        fun fromSharedJson(shortRecord: ShortRecord, pageNumber: Int, plyNumber: Int, reader: StrReader?): PliesPage =
            shortRecord.settings.pliesPageData.readPlies(shortRecord, pageNumber, reader, false).let {
                PliesPage(shortRecord, pageNumber, plyNumber, it.size, it)
            }

        fun fromId(shortRecord: ShortRecord, pageNumberIn: Int, jsonHeader: Any): PliesPage =
            jsonHeader.parseJsonMap().let {
                val pageNumber: Int = it[keyPageNumber] as Int? ?: 0
                val plyNumber: Int = it[keyFirstPlyNumber] as Int? ?: 0
                val count: Int = it[keyCount] as Int? ?: 0
                if (pageNumber != pageNumberIn) throw IllegalArgumentException("Wrong page number stored: $pageNumber at $pageNumberIn position")
                PliesPage(shortRecord, pageNumber, plyNumber, count, null)
            }
    }
}
