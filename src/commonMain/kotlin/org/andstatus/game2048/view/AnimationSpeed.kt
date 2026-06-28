package org.andstatus.game2048.view

const val minAnimationSpeed: Int = 8
const val defaultAnimationSpeed: Int = 10
const val maxAnimationSpeed: Int = 12

class AnimationSpeed private constructor(
    private val relativeSpeed: Int
) {
    val scale: Double = when (relativeSpeed) {
        minAnimationSpeed -> 0.3
        9 -> 0.6
        defaultAnimationSpeed -> 1.0
        11 -> 1.5
        maxAnimationSpeed -> 2.0
        else -> 1.0
    }

    fun save(): Int = relativeSpeed

    val up: AnimationSpeed get() = if (relativeSpeed < maxAnimationSpeed) {
        AnimationSpeed(relativeSpeed + 1)
    } else {
        this
    }

    val down: AnimationSpeed get() = if (relativeSpeed > minAnimationSpeed) {
        AnimationSpeed(relativeSpeed - 1)
    } else {
        this
    }

    /** circle all speeds */
    val change: AnimationSpeed get() = up.let {
       if (it == this) {
           AnimationSpeed(minAnimationSpeed)
       } else {
           it
       }
    }

    override fun toString(): String = relativeSpeed.toString()

    companion object {
        fun load(relativeSpeedIn: Int?): AnimationSpeed {
            val relativeSpeedFixed: Int = relativeSpeedIn
                ?.takeIf { it >= minAnimationSpeed && it <= maxAnimationSpeed }
                ?: defaultAnimationSpeed
            return AnimationSpeed(relativeSpeedFixed)
        }
    }
}
