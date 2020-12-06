package org.andstatus.game2048.view

import com.soywiz.klock.DateTime
import com.soywiz.korev.Key
import com.soywiz.korge.input.Input
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onDown
import com.soywiz.korge.input.onOver
import com.soywiz.korge.input.onUp
import com.soywiz.korge.view.Container
import com.soywiz.korio.util.OS
import com.soywiz.korma.geom.Point
import org.andstatus.game2048.myLog
import kotlin.math.abs

class DuplicateKeyPressFilter {
    private val pressed: MutableMap<Key, Long> = HashMap()
    private val minMillisBetweenPresses = 300
    private val pointNONE = Point(0, 0)
    private var buttonPointClicked = pointNONE

    fun ifWindowCloseKeyPressed(input: Input, action: () -> Unit) {
        val keys = input.keys
        if (keys[Key.ENTER] or keys[Key.SPACE] or keys[Key.BACKSPACE])
            onPress(Key.BACKSPACE, action)
    }

    fun onSwipeOrOver(action: () -> Unit) = onPress(Key.RIGHT, action)

    fun onPress(key: Key, action: () -> Unit) {
        val now = DateTime.nowUnixLong()
        pressed[key]?.let{
            if (abs(now - it) < minMillisBetweenPresses) return
        }
        pressed[key] = now
        action()
    }

    /** Workaround for the bug: https://github.com/korlibs/korge-next/issues/56 */
    fun Container.customOnClick(handler: () -> Unit) {
        if (OS.isAndroid) {
            onOver {
                onSwipeOrOver { myLog("onOver $pos") }
            }
            onDown {
                myLog("onDown $pos")
                buttonPointClicked = pos.copy()
            }
            onClick { myLog("onClick $pos}") }
            onUp {
                val clicked = buttonPointClicked == pos
                myLog("onUp $pos " + if (clicked) "- clicked" else "<- $buttonPointClicked")
                buttonPointClicked = pointNONE
                if (clicked) handler()
            }
        } else {
            onClick {
                handler()
            }
        }
    }

}