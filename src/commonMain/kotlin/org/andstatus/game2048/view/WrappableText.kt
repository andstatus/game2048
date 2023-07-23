package org.andstatus.game2048.view

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.font.DefaultTtfFont
import korlibs.image.font.Font
import korlibs.image.font.TextMetrics
import korlibs.image.font.getTextBounds
import korlibs.image.text.ITextRendererActions
import korlibs.image.text.TextAlignment
import korlibs.image.text.TextRenderer
import korlibs.io.lang.WStringReader
import korlibs.io.resources.Resourceable
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.ViewDslMarker
import korlibs.korge.view.addTo
import korlibs.math.geom.Rectangle
import org.andstatus.game2048.myLog

/** Based on https://forum.korge.org/topic/40/text-wrapping/3 */
enum class Gravity {
    LEFT, CENTER, RIGHT
}

inline fun Container.wrappableText(
        text: String,
        wrapWidth: Float,
        textSize: Float = 16.0f,
        color: RGBA = Colors.WHITE,
        font: Resourceable<out Font> = DefaultTtfFont,
        gravity: Gravity = Gravity.CENTER,
        callback: @ViewDslMarker WrappableText.() -> Unit = {}
) = WrappableText(
        text,
        textSize = textSize,
        color = color,
        font = font,
        wrapWidth = wrapWidth,
        renderer = WrappableTextRenderer(wrapWidth, gravity)
).addTo(this, callback)

class WrappableText(
        text: String, textSize: Float = DEFAULT_TEXT_SIZE,
        color: RGBA = Colors.WHITE, font: Resourceable<out Font> = DefaultTtfFont,
        alignment: TextAlignment = TextAlignment.TOP_LEFT,
        wrapWidth: Float,
        renderer: TextRenderer<String>,
        autoScaling: Boolean = true
) : Text(text, textSize, color, font, alignment, renderer, autoScaling) {
    init {
        val f = this.font.getOrNull()

        if (f is Font) {
            val metrics: TextMetrics = f.getTextBounds(textSize, text, renderer = renderer)
            Rectangle( 0.0f,0.0f, wrapWidth, metrics.bounds.height).let {
                setTextBounds(it)
            }
        }
    }
}

class WrappableTextRenderer(val wrapWidth: Float, val gravity: Gravity) : TextRenderer<String> {

    override fun ITextRendererActions.run(text: String, size: Float, defaultFont: Font) {
        myLog("To wrap at $wrapWidth: $text")
        val reader = WStringReader(text)

        reset()
        setFont(defaultFont, size)

        val lines = mutableListOf(Line())
        val spaceWidth = getGlyphMetrics(reader, ' '.code).xadvance + getKerning(' '.code, 'A'.code)

        for (wrapped in text.split('\n')) {
            var curX = 0.0f
            for (word in wrapped.split(' ')) {
                var wordWidth = 0.0f
                var curWord = ""
                for (n in word.indices) {
                    val c = word[n].code
                    val c1 = word.getOrElse(n + 1) { '\u0000' }.code

                    val g = getGlyphMetrics(reader, c)
                    val kerning = getKerning(c, c1)
                    val charWidth = g.xadvance + kerning

                    if (wordWidth + charWidth + spaceWidth > wrapWidth) {
                        // Wrap inside the word
                        val word1 = Word(curWord, wordWidth)
                        if (lines.last().words.isEmpty()) {
                            lines.last().words.add(word1)
                        } else {
                            lines.add(Line(mutableListOf(word1)))
                        }
                        curX = 0.0f
                        wordWidth = 0.0f
                        curWord = ""
                        lines.add(Line())
                    }

                    wordWidth += charWidth
                    curWord += c.toChar()
                }

                curX += wordWidth + spaceWidth

                if (curX > wrapWidth) {
                    lines.add(Line())
                    curX = wordWidth + spaceWidth
                }

                lines.last().words.add(Word(curWord, wordWidth))
            }
            lines.add(Line())
        }

        for (line in lines) {
            myLog("Line ${line.calculateWidth(spaceWidth)}: $line")
            // TODO: Figure out how to use "gravity" and if this is still needed...
            var start: Float =
                    when (gravity) {
                        Gravity.LEFT -> 0.0f
                        Gravity.CENTER -> (wrapWidth - line.calculateWidth(spaceWidth)) / 2
                        Gravity.RIGHT -> wrapWidth - line.calculateWidth(spaceWidth)
                    }

            for (word in line.words) {
                // No "x" anymore
                //x = start

                for (n in word.text.indices) {
                    val c = word.text[n].code
                    val c1 = word.text.getOrElse(n + 1) { '\u0000' }.code

                    val g = getGlyphMetrics(reader, c)
                    // No such method anymore:
                    //transform.identity()

                    val advance = g.xadvance + getKerning(c, c1)

                    put(reader, c)
                    advance(advance)
                }
                advance(spaceWidth)

                start += word.width + spaceWidth
            }

            newLine(lineHeight, false)
        }

        put(reader, 0)
    }

    private data class Line(
            val words: MutableList<Word> = mutableListOf()
    ) {
        fun calculateWidth(spaceWidth: Float): Float {
            return words.sumOf{ it.width.toDouble() }.toFloat() + (words.size - 1) * spaceWidth
        }
    }

    private data class Word(val text: String, val width: Float)
}
