package org.andstatus.game2048

class History(from: String?, private val onUpdate: (History) -> Unit) {

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
    val currentElement: Element? get() = if (elements.isEmpty()) null else elements.last()

    init {
        from?.split(';')?.forEach {
            val element = elementFromString(it)
            elements.add(element)
        }
    }

    private fun elementFromString(string: String): Element {
        val numbers = string.split(',').map { it.toInt() }
        if (numbers.size != 17) throw IllegalArgumentException("Incorrect history")
        return Element(IntArray(16) { numbers[it] }, numbers[16])
    }

    fun add(pieceIds: IntArray, score: Int) {
        val element = Element(pieceIds, score)
        if (elements.isEmpty() || currentElement != element) {
            elements.add(element)
            onUpdate(this)
        }
    }

    fun undo(): Element {
        if (elements.size > 1) {
            elements.removeAt(elements.size - 1)
            onUpdate(this)
        }
        return elements.last()
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