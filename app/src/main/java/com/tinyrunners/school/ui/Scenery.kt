package com.tinyrunners.school.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The opening's scenery: cream-to-green sky, slowly drifting pixel clouds, one continuous band of stepped pixel
 * hills and a strip of ground. Shared by the opening and the menu screens so the whole app feels like one place.
 * The game screen has its own hometown backgrounds and does not use this.
 */
class ScenePainter {
    private val fill = Paint()
    private val sky = Paint()
    private var skyFor = -1f
    private val startNanos = System.nanoTime()

    /** Size of one scenery "pixel" for a view of this size; based on the short side so portrait looks the same. */
    fun unit(w: Int, h: Int): Float = max(2f, (min(w, h) / 90f).roundToInt().toFloat())

    /**
     * Draws everything above and below [groundY]. Returns nothing; callers draw their own content on top.
     * [hillHeight] is the height of the hill band above the ground.
     */
    fun draw(canvas: Canvas, w: Int, h: Int, groundY: Float, hillHeight: Float) {
        val px = unit(w, h)
        val hillBase = groundY - hillHeight

        if (skyFor != hillBase) {
            sky.shader = LinearGradient(0f, 0f, 0f, hillBase, SKY, SKY_LOW, Shader.TileMode.CLAMP)
            skyFor = hillBase
        }
        canvas.drawRect(0f, 0f, w.toFloat(), groundY, sky)

        drawClouds(canvas, w, hillBase, px)

        fill.color = HILL
        canvas.drawRect(0f, hillBase, w.toFloat(), groundY, fill)
        var x = 0f
        while (x < w) {
            val bump = ((sin(x / (w * 0.11f)) + 1f) * 0.5f * 7f + (sin(x / (w * 0.043f) + 1.3f) + 1f) * 2f).toInt()
            canvas.drawRect(x, hillBase - bump * px, x + px * 4, hillBase, fill)
            x += px * 4
        }
        fill.color = GROUND; canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), fill)
        fill.color = GRASS; canvas.drawRect(0f, groundY, w.toFloat(), groundY + px * 1.5f, fill)
    }

    /** Clouds live in the sky above [skyBottom]; each drifts right at its own slow speed and wraps around. */
    private fun drawClouds(canvas: Canvas, w: Int, skyBottom: Float, px: Float) {
        val seconds = (System.nanoTime() - startNanos) / 1_000_000_000f
        val cell = px * 2
        CLOUDS.forEach { (shape, yFrac, xFrac, speed) ->
            val cloudW = shape[0].length * cell
            val span = w + cloudW
            val left = ((xFrac * w + seconds * speed * px) % span) - cloudW
            val top = skyBottom * yFrac
            shape.forEachIndexed { row, line ->
                fill.color = if (row == shape.lastIndex) CLOUD_SHADE else CLOUD
                line.forEachIndexed { col, c ->
                    if (c == '#') canvas.drawRect(left + col * cell, top + row * cell, left + (col + 1) * cell, top + (row + 1) * cell, fill)
                }
            }
        }
    }

    private data class Cloud(val shape: List<String>, val yFrac: Float, val xFrac: Float, val speed: Float)

    companion object {
        /** A light 15 fps redraw is enough for the slow cloud drift. */
        const val FRAME_MS = 66L
        val SKY = Color.rgb(250, 246, 236)
        /** Sky colour just above the hills: halfway between cream and the hill green. */
        val SKY_LOW = Color.rgb(236, 240, 226)
        val CLOUD = Color.rgb(255, 255, 252)
        val CLOUD_SHADE = Color.rgb(228, 232, 226)
        val HILL = Color.rgb(221, 232, 214)
        val GROUND = Color.rgb(196, 170, 128)
        val GRASS = Color.rgb(122, 160, 110)
        private val CLOUDS = listOf(
            Cloud(listOf("...####.....", ".########...", "###########.", ".#########.."), 0.10f, 0.05f, 6f),
            Cloud(listOf("..###...", ".######.", "########"), 0.40f, 0.62f, 4f),
            Cloud(listOf("....###......", "..########...", ".###########.", "#############", ".###########."), 0.16f, 0.78f, 5f),
            Cloud(listOf(".####..", "#######", ".#####."), 0.52f, 0.30f, 3f),
            Cloud(listOf("..####...", "#########", ".#######."), 0.75f, 0.12f, 4f),
        )
    }
}

/**
 * Full-screen scenery placed behind menu screens. Hills and ground sit at the bottom; content scrolls over the top
 * while the scenery stays still, so text and cards stay readable on the light sky.
 */
class SceneryView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,   // needed when placed in an XML layout (activity_main.xml)
) : View(context, attrs) {
    private val painter = ScenePainter()

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        val w = width; val h = height
        val short = min(w, h).toFloat()
        val groundY = h - short * 0.07f
        painter.draw(canvas, w, h, groundY, short * 0.2f)
        postInvalidateDelayed(ScenePainter.FRAME_MS)
    }
}
