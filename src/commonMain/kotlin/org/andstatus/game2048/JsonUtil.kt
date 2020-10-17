package org.andstatus.game2048

import com.soywiz.korio.serialization.json.Json

fun Any.asJsonArray(): List<Any> = try {
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
    emptyList()
}

fun Any.asJsonMap(): Map<String, Any> = try {
    when (this) {
        is String -> Json.parse(this)
        else -> this
    }.let {
        when (it) {
            is Map<*, *> -> it.filterValues { value -> value != null }
            else -> emptyMap()
        }
    } as Map<String, Any>
} catch (e: Exception) {
    emptyMap()
}