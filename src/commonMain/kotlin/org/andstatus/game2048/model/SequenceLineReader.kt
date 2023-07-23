package org.andstatus.game2048.model

import korlibs.io.lang.Closeable
import korlibs.io.lang.substr
import korlibs.io.util.StrReader

val emptySequenceLineReader: SequenceLineReader = SequenceLineReader(emptySequence())

/** @author yvolk@yurivolkov.com */
class SequenceLineReader(private val sequence: Sequence<String>) : Closeable {
    private val iterator: Iterator<String> = sequence.iterator()
    private var previousRead: String? = null
    private var buffered: String = ""
    var posFromInput: Int = 0

    val isEmpty: Boolean = !iterator.hasNext()
    val hasMore: Boolean get() = buffered.isNotEmpty() || iterator.hasNext()

    fun readLine(): String? = nextLine()
        .also {
            previousRead = it
        }

    fun <T> readNext(parser: (StrReader) -> T): Result<T> {
        val toParse = nextLine() ?: ""
        val strReader = StrReader(toParse)
        return try {
            parser(strReader).let {
                previousRead = toParse.substr(0, strReader.pos)
                toParse.substr(strReader.pos).also {
                    if (it.isNotBlank()) {
                        buffered = it + "\n" + buffered
                    }
                }
                Result.success(it)
            }
        } catch (t: Throwable) {
            Result.failure(
                Exception(
                    "parseNext failed while parsing '$toParse' pos ${strReader.pos}," +
                        "\nprevious parsed: '$previousRead'", t
                )
            )
        }
    }

    override fun close() {
        if (sequence is Closeable) sequence.close()
    }

    fun unRead(): SequenceLineReader {
        if (previousRead != null) {
            buffered = previousRead + "\n" + buffered
            previousRead = null
        }
        return this
    }

    private fun nextLine(): String? {
        var line = buffered
        buffered = ""
        line.findAnyOf(listOf("\r\n", "\n", "\r"))?.let { pair ->
            buffered = line.substr(pair.first + pair.second.length)
            line.substr(0, pair.first).let {
                if (it.isNotBlank()) return it.trim()
                line = ""
            }
        }
        while (hasMore) {
            val str1 = if (buffered.isNotEmpty()) {
                val str2 = buffered
                buffered = ""
                str2
            } else {
                iterator.next()?.also {
                    posFromInput += it.length
                } ?: ""
            }
            val indexAndOccurrence = str1.findAnyOf(listOf("\r\n", "\n", "\r"))
            if (indexAndOccurrence == null) {
                line += str1
            } else {
                buffered = str1.substr(indexAndOccurrence.first + indexAndOccurrence.second.length)
                line += str1.substr(0, indexAndOccurrence.first)
                if (line.isNotBlank()) return line.trim()
                line = ""
            }
        }
        return if (line.isBlank() && buffered.isBlank()) {
            null
        } else {
            line.trim()
        }
    }
}
