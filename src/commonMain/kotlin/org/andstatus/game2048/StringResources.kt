package org.andstatus.game2048

class StringResources(val lang: String) {
    private val strings: Lazy<Map<String, String>> = lazy {
        initialize(lang)
    }
    private val defaultStrings: Lazy<Map<String, String>> = lazy {
        initialize("")
    }

    private fun initialize(lang: String): Map<String, String> {
        TODO()
    }

    fun text(key: String): String = strings.value.get(key) ?: defaultStrings.value.get(key) ?: key
}