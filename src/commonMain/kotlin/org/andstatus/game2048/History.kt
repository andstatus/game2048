package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log

class History(from: String?, private val onUpdate: (History) -> Unit) {
    var historyIndex = -1

    data class Element(val pieceIds: IntArray, val score: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Element

            if (!pieceIds.contentEquals(other.pieceIds)) return false
            if (score != other.score) return false

            return true
        }

        override fun hashCode(): Int {
            var result = pieceIds.contentHashCode()
            result = 31 * result + score
            return result
        }
    }

    private val elements = mutableListOf<Element>()
    val currentElement: Element?
        get() = when {
            elements.isEmpty() -> null
            historyIndex < 0 -> elements.last()
            else -> elements[historyIndex]
        }

    init {
        from?.split(';')?.forEach {
            elementFromString(it)?.let{
                elements.add(it)
            }
        }
    }

    private fun elementFromString(string: String): Element? {
        if (string.isEmpty()) {
            Console.log("elementFromString: String is empty")
            return null
        }
        val numbers = string.split(',').map { it.toInt() }
        if (numbers.size != 17) {
            Console.log("elementFromString: Invalid history '$string'")
            return null
        }
        return Element(IntArray(16) { numbers[it] }, numbers[16])
    }

    fun add(pieceIds: IntArray, score: Int) {
        val element = Element(pieceIds, score)
        while (historyIndex >= 0 && (historyIndex < elements.size - 1)) {
            elements.removeAt(elements.size - 1)
        }
        historyIndex = -1

        if (elements.isEmpty() || elements.last() != element) {
            elements.add(element)
        }
        onUpdate(this)
    }

    fun canUndo(): Boolean {
        return settings.allowUndo && elements.size > 1 && historyIndex != 0
    }

    fun undo(): Element? {
        if (canUndo()) {
            if (historyIndex < 0) historyIndex = elements.size - 2 else historyIndex--
        }
        return currentElement
    }

    fun canRedo(): Boolean {
        return historyIndex >= 0 && historyIndex < elements.size - 1
    }

    fun redo(): Element? {
        if (canRedo()) historyIndex++
        return currentElement
    }

    fun clear() {
        elements.clear()
        onUpdate(this)
    }

    override fun toString(): String {
        return elements.joinToString(";") {
            it.pieceIds.joinToString(",") + "," + it.score
        }
    }
}