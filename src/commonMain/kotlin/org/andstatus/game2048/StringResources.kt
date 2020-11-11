package org.andstatus.game2048

import com.soywiz.klogger.Console
import com.soywiz.klogger.log
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.serialization.xml.Xml

class StringResources private constructor(private val strings: Map<String, String>,
                                          private val defaultStrings: Map<String, String>) {
    fun text(key: String): String = strings[key] ?: defaultStrings[key] ?: key

    companion object {
        suspend fun load(lang: String): StringResources = StringResources(loadLang(lang), loadLang(""))
                .also {
                    Console.log("Loaded ${it.strings.size} strings for '$lang' language")
                }

        private suspend fun loadLang(lang: String): Map<String, String> =
            "res/values${if (lang.isEmpty()) "" else "-"}$lang/strings.xml"
            .let { path -> resourcesVfs[path] }
            .let { if (it.exists()) it.readString() else ""}
            .let { Xml(it) }
            .allChildren
            .mapNotNull { xml -> xml.attribute("name")?.let { it to xml.text } }
            .toMap()
    }
}