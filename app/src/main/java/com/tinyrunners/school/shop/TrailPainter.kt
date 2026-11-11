package com.tinyrunners.school.shop

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sin

/**
 * Draws a decoration trail as small pixel shapes floating out behind a friend. Pure drawing: it has no effect on
 * the run. [phase] should advance with the distance run so the trail stops when the run is paused.
 */
object TrailPainter {
    private val paint = Paint().apply { isAntiAlias = false }

    /**
     * [backX] is the friend's back edge, [midY] the middle of their body, [px] one art pixel on screen.
     */
    fun draw(canvas: Canvas, style: TrailStyle, backX: Float, midY: Float, px: Float, phase: Float) {
        if (style == TrailStyle.NONE) return
        if (style == TrailStyle.RAINBOW) { rainbow(canvas, backX, midY, px, phase); return }
        val shape = SHAPES.getValue(style)
        val gap = px * 9f
        val shift = (phase % 1f) * gap
        for (i in 0 until COUNT) {
            val x = backX - px * 2f - i * gap + shift - gap
            val y = midY + sin(i * 1.7f + phase * 2.4f) * px * 3f - shape.size * px / 2f
            // Nearest shapes are fully opaque; farther ones fade but stay visible on busy backgrounds.
            val fade = 1f - i.toFloat() / (COUNT + 1)
            drawShape(canvas, shape, x, y, px, (255 * fade).toInt())
        }
    }

    private fun drawShape(canvas: Canvas, shape: List<String>, left: Float, top: Float, px: Float, alpha: Int) {
        // A soft dark outline one pixel wide keeps light shapes readable over detailed scenery.
        paint.color = OUTLINE
        paint.alpha = alpha * 3 / 5
        shape.forEachIndexed { row, line ->
            line.forEachIndexed { col, c ->
                if (PALETTE[c] == null) return@forEachIndexed
                canvas.drawRect(left + (col - 1) * px + px * 0.5f, top + (row - 1) * px + px * 0.5f,
                    left + (col + 2) * px - px * 0.5f, top + (row + 2) * px - px * 0.5f, paint)
            }
        }
        shape.forEachIndexed { row, line ->
            line.forEachIndexed { col, c ->
                val color = PALETTE[c] ?: return@forEachIndexed
                paint.color = color
                paint.alpha = alpha
                canvas.drawRect(left + col * px, top + row * px, left + (col + 1) * px, top + (row + 1) * px, paint)
            }
        }
    }

    /** Three soft stripes that wave a little behind the friend. */
    private fun rainbow(canvas: Canvas, backX: Float, midY: Float, px: Float, phase: Float) {
        val colors = intArrayOf(Color.rgb(240, 140, 150), Color.rgb(246, 210, 110), Color.rgb(140, 200, 170), Color.rgb(140, 170, 230))
        val length = 7
        for (seg in 0 until length) {
            val x = backX - px * 2f - (seg + 1) * px * 4f
            val wave = sin(seg * 0.9f + phase * 3f) * px * 1.5f
            val alpha = (230 * (1f - seg.toFloat() / (length + 1))).toInt()
            colors.forEachIndexed { band, color ->
                paint.color = color
                paint.alpha = alpha
                val top = midY - px * 4f + band * px * 2f + wave
                canvas.drawRect(x, top, x + px * 4f, top + px * 2f, paint)
            }
        }
    }

    private const val COUNT = 5
    private val OUTLINE = Color.rgb(60, 50, 70)
    private val PALETTE = mapOf(
        'y' to Color.rgb(250, 214, 92), 'Y' to Color.rgb(255, 245, 190),
        'p' to Color.rgb(240, 120, 150), 'P' to Color.rgb(255, 200, 215),
        'b' to Color.rgb(120, 180, 230), 'B' to Color.rgb(225, 245, 255),
        'k' to Color.rgb(70, 60, 90),
    )
    private val SHAPES = mapOf(
        TrailStyle.STARS to listOf("..y..", ".yYy.", "yYYYy", ".yYy.", "..y.."),
        TrailStyle.HEARTS to listOf(".pp.pp.", "pPPpppp", "ppppppp", ".ppppp.", "..ppp..", "...p..."),
        TrailStyle.PETALS to listOf(".PP", "PpP", "PP."),
        TrailStyle.BUBBLES to listOf(".bb.", "bBBb", "bBbb", ".bb."),
        TrailStyle.NOTES to listOf("..kk", "..k.", "..k.", "kkk.", "kk.."),
    )
}
