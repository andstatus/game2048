package org.andstatus.game2048.view

import korlibs.time.DateTime
import korlibs.event.Key
import korlibs.korge.input.Input
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
        val now = DateTime.nowUnixMillisLong()
        pressed[key]?.let{
            if (abs(now - it) < minMillisBetweenPresses) return
        }
        pressed[key] = now
        action()
    }

}