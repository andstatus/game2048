package org.andstatus.game2048.view

import com.soywiz.klock.DateTime
import com.soywiz.korev.Key
import com.soywiz.korge.input.Input
import kotlin.math.abs

class DuplicateKeyPressFilter {
    private val pressed: MutableMap<Key, Long> = HashMap()
    private val minMillisBetweenPresses = 300

    fun ifWindowCloseKeyPressed(input: Input, action: () -> Unit) {
        val keys = input.keys
        if (keys[Key.ENTER] or keys[Key.SPACE] or keys[Key.BACKSPACE])
            onPress(Key.BACKSPACE, action)
    }

    fun onPress(key: Key, action: () -> Unit) {
        val now = DateTime.nowUnixLong()
        pressed[key]?.let{
            if (abs(now - it) < minMillisBetweenPresses) return
        }
        pressed[key] = now
        action()
    }

}