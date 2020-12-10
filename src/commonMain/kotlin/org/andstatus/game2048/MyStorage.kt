package org.andstatus.game2048

import com.soywiz.klock.DateTime
import com.soywiz.korge.service.storage.NativeStorage
import com.soywiz.korge.service.storage.get
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.Views
import com.soywiz.korio.util.OS

private val keyOpened = "opened"

class MyStorage(views: Views) {
    val native = NativeStorage(views)

    operator fun get(key: String) = native.get(key)
    operator fun set(key: String, value: String) = native.set(key, value)
    fun getOrNull(key: String): String? = native.getOrNull(key)

    fun getBoolean(key: String, default: Boolean): Boolean = native.getOrNull(key)?.let {
        it.toBoolean()
    } ?: default

    override fun toString(): String =
        "Storage " + (native.getOrNull(keyOpened)?.let { "last opened: $it" } ?: "is new") +
                "; Storage keys: ${native.keys().sorted()}; Platform:${OS.platformName}"

    companion object {
        fun load(stage: Stage): MyStorage = myMeasuredIt("NativeStorage") {
            val storage = MyStorage(stage.views)
            storage[keyOpened] = DateTime.now().toString()
            storage
        }
    }
}