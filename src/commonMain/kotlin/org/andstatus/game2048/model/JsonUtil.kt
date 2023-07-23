package org.andstatus.game2048.model

import korlibs.io.serialization.json.Json
import korlibs.io.util.StrReader
import org.andstatus.game2048.myLog

fun Any.parseJsonArray(): List<Any> = try {
    when (this) {
        is String -> Json.parse(this)
        else -> this
    }.let {
        when (it) {
            is List<*> -> it.filterNotNull()
            else -> emptyList()
        }
    }
} catch (e: Exception) {
    myLog("Error parseJsonArray '${this.toString().take(400)}': $e")
    emptyList()
}

fun Any.parseJsonMap(): Map<String, Any> =
    @Suppress("UNCHECKED_CAST")
    try {
        when (this) {
            is String -> Json.parse(this) as Map<String, Any>
            is StrReader -> Json.parse(this) as Map<String, Any>
            is SequenceLineReader -> this.readNext(StrReader::parseJsonMap).onFailure {
                myLog("Error parseJsonMap '${this.toString().take(400)}': $it")
                return emptyMap()
            }.getOrNull()
            else -> this
        }
            .let {
                when (it) {
                    is Map<*, *> -> it.filterValues { value -> value != null } as Map<String, Any>
                    else -> emptyMap()
                }
            }
    } catch (e: Throwable) {
        myLog("Error parseJsonMap '${this.toString().take(400)}': $e")
        emptyMap()
    }
