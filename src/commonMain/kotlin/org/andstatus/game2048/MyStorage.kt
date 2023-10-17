package org.andstatus.game2048

import korlibs.time.DateTime
import korlibs.korge.service.storage.NativeStorage
import korlibs.korge.service.storage.get
import korlibs.korge.view.Stage
import korlibs.korge.view.Views
import korlibs.io.lang.parseInt
import korlibs.platform.Platform

private val keyOpened = "opened"

class MyStorage(views: Views) {
    val native = NativeStorage(views)

    operator fun get(key: String) = native[key]
    operator fun set(key: String, value: String) = native.set(key, value)
    fun getOrNull(key: String): String? = native.getOrNull(key)

    fun getBoolean(key: String, default: Boolean): Boolean = native.getOrNull(key)?.let {
        it.toBoolean()
    } ?: default
    operator fun set(key: String, value: Boolean) = native.set(key, value.toString())

    fun getInt(key: String, default: Int): Int = try {
        native.getOrNull(key)?.parseInt() ?: default
    } catch (e: Exception) {
        default
    }
    operator fun set(key: String, value: Int) = native.set(key, value.toString())

    fun remove(key: String): Boolean = (native.keys().contains(key))
        .also {
            // Don't believe "contains", try to delete anyway
            native.remove(key)
        }

    override fun toString(): String =
        "Storage " + (native.getOrNull(keyOpened)?.let { "last opened: $it" } ?: "is new") +
                "; Storage keys: ${native.keys().sorted()}; Platform:${Platform.rawPlatformName}"

    companion object {
        fun load(stage: Stage): MyStorage = myMeasuredIt("NativeStorage") {
            val storage = MyStorage(stage.views)
            storage[keyOpened] = DateTime.now().toString()
            storage
        }
    }
}