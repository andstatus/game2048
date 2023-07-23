package org.andstatus.game2048.view

import korlibs.io.file.std.resourcesVfs
import korlibs.io.lang.FileNotFoundException
import korlibs.io.lang.indexOfOrNull
import korlibs.io.serialization.xml.Xml
import org.andstatus.game2048.myLog

class StringResources private constructor(val lang: String, val strings: Map<String, String>,
                                          private val defaultStrings: Map<String, String>) {

    fun text(key: String): String = strings[key] ?: defaultStrings[key] ?: key
    val hasNonDefaultStrings: Boolean get() = strings.isNotEmpty()

    companion object {
        val existingLangCodes = listOf("", "ru", "si", "zh")

        suspend fun load(lang: String): StringResources {
            val pairLang = loadLang(lang)
            val pairDefault = if (pairLang.first.isEmpty()) pairLang else loadLang("")
            return StringResources(pairLang.first, pairLang.second , pairDefault.second)
                .also {
                    myLog("Loaded ${it.strings.size} strings for '${it.lang}' language and ${it.defaultStrings.size} default")
                }
        }

        private suspend fun loadLang(lang: String): Pair<String, Map<String, String>> {
            val exact = loadLangFile(lang)
            if (exact.isNotEmpty() || lang.isEmpty()) {
                return lang to exact
            }
            return lang.indexOfOrNull('-')?.let {
                lang.substring(0, it) to loadLangFile(lang.substring(0, it))
            } ?: (lang to exact)
        }

        private suspend fun loadLangFile(lang: String): Map<String, String> =
            "res/values${if (lang.isEmpty()) "" else "-"}$lang/strings.xml"
            .let { path -> resourcesVfs[path] }
            // In Android it.exists() always returns false, so we have to catch exception instead of below:
            //    .let { if (it.exists()) it.readString() else ""}
            .let {
                try {
                    it.readString()
                } catch (e: FileNotFoundException) {
                    ""
                }
            }
            .let { Xml(it) }
            .allChildren
            .mapNotNull { xml -> xml.attribute("name")?.let { it to xml.text } }
            .toMap()
    }
}