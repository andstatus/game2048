package org.andstatus.game2048.model

import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.util.StrReader
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
    myLog("Error parsing '${this.toString().take(200)}': $e")
    emptyList()
}

fun Any.parseJsonMap(): Map<String, Any> =
    @Suppress("UNCHECKED_CAST")
    try {
        when (this) {
            is String -> Json.parse(this) as Map<String, Any>
            is StrReader -> Json.parse(this) as Map<String, Any>
            else -> this
        }
            .let {
                when (it) {
                    is Map<*, *> -> it.filterValues { value -> value != null } as Map<String, Any>
                    else -> emptyMap()
                }
            }
    } catch (e: Throwable) {
        myLog("Error parsing '${this.toString().take(200)}': $e")
        emptyMap()
    }
