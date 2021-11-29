package org.andstatus.game2048.model

import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.util.StrReader
import org.andstatus.game2048.myLog

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

fun Any.asJsonMap(): Map<String, Any> =
    @Suppress("UNCHECKED_CAST")
    when (this) {
        is String -> try {
            Json.parse(this) as Map<String, Any>
        } catch (e: Exception) {
            myLog("Error parsing $this\n$e")
            emptyMap()
        }
        is StrReader -> try {
            Json.parse(this) as Map<String, Any>
        } catch (e: Exception) {
            myLog("Error parsing $this\n$e")
            emptyMap()
        }
        else -> this
    }.let {
        when (it) {
            is Map<*, *> -> it.filterValues { value -> value != null } as Map<String, Any>
            else -> emptyMap()
        }
    }
