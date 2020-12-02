package org.andstatus.game2048

import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.serialization.xml.Xml

class StringResources private constructor(private val strings: Map<String, String>,
                                          private val defaultStrings: Map<String, String>) {
    fun text(key: String): String = strings[key] ?: defaultStrings[key] ?: key
    val hasNonDefaultStrings: Boolean get() = strings.isNotEmpty()

    companion object {
        suspend fun load(lang: String): StringResources = StringResources(loadLang(lang), loadLang(""))
                .also {
                    myLog("Loaded ${it.strings.size} strings for '$lang' language")
                }

        private suspend fun loadLang(lang: String): Map<String, String> =
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