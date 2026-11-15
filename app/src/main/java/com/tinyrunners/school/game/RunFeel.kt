package com.tinyrunners.school.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Weight for the runner: pixel dust kicked up at the feet on each jump and landing, and a tiny camera shake (one or
 * two art pixels, under a fifth of a second) when the feet hit the ground. Looks only; the physics never change.
 */
class RunFeel {
    private class Mote(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val maxLife: Float, val size: Float, val color: Int)

    private val motes = ArrayList<Mote>()
    private val random = Random(7)
    private val paint = Paint()
    private var shakeLeft = 0f
    private var shakeAmp = 0f

    /** Dust puffing back and out from the feet at ([x], [y]) in screen pixels; [px] is one art pixel. */
    fun jump(x: Float, y: Float, px: Float, color: Int) = puff(x, y, px, color, count = 6, spread = 0.7f)

    /** A wider puff and a short shake; [hard] (a fall from high up) shakes two art pixels instead of one. */
    fun land(x: Float, y: Float, px: Float, color: Int, hard: Boolean) {
        puff(x, y, px, color, count = if (hard) 10 else 7, spread = 1f)
        shakeAmp = px * if (hard) 2f else 1f
        shakeLeft = SHAKE_SECONDS
    }

    private fun puff(x: Float, y: Float, px: Float, color: Int, count: Int, spread: Float) {
        repeat(count) {
            val side = if (it % 2 == 0) -1f else 1f
            val life = 0.25f + random.nextFloat() * 0.2f
            motes += Mote(x + side * random.nextFloat() * 4f * px, y - random.nextFloat() * px,
                side * (10f + random.nextFloat() * 26f) * px * spread - 12f * px, -(6f + random.nextFloat() * 14f) * px,
                life, life, px * (if (random.nextFloat() < 0.35f) 2f else 1f), color)
        }
        while (motes.size > MAX_MOTES) motes.removeAt(0)
    }

    fun update(dt: Float) {
        val it = motes.iterator()
        while (it.hasNext()) {
            val m = it.next()
            m.life -= dt
            if (m.life <= 0f) { it.remove(); continue }
            m.x += m.vx * dt; m.y += m.vy * dt
            m.vy += 60f * m.size * dt
            m.vx *= 0.92f
        }
        if (shakeLeft > 0f) shakeLeft = (shakeLeft - dt).coerceAtLeast(0f)
    }

    /** The camera offset for this frame: a whole-pixel jolt that settles, or (0, 0). */
    fun shakeX(): Float = if (shakeLeft <= 0f) 0f else (if ((shakeLeft * 60f).toInt() % 2 == 0) 1f else -1f) * shakeAmp * (shakeLeft / SHAKE_SECONDS)
    fun shakeY(): Float = if (shakeLeft <= 0f) 0f else shakeAmp * (shakeLeft / SHAKE_SECONDS)

    fun draw(canvas: Canvas) {
        for (m in motes) {
            paint.color = m.color
            paint.alpha = (230 * (m.life / m.maxLife)).toInt().coerceIn(0, 255)
            val x = m.x.roundToInt().toFloat(); val y = m.y.roundToInt().toFloat()
            canvas.drawRect(x, y, x + m.size, y + m.size, paint)
        }
    }

    fun clear() { motes.clear(); shakeLeft = 0f }

    private companion object {
        const val SHAKE_SECONDS = 0.16f
        const val MAX_MOTES = 60
    }
}

/**
 * Draws a running story without stopping the run: the friend's line in a speech bubble above their head, the
 * narrator's line as a subtitle under the top bar, and the crossroads question with its two routes.
 */
class RunStoryPainter(private val density: Float) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }
    private val path = Path()

    /**
     * A cream bubble with a tail pointing at ([headX], [headY]); stays on screen horizontally and never above
     * [minTop] (the top bar), so a high jump does not push it over the score.
     */
    fun bubble(canvas: Canvas, line: String, headX: Float, headY: Float, screenW: Float, minTop: Float) {
        text.textSize = 17f * density; text.color = INK
        val maxW = (screenW * 0.36f).toInt()
        val layout = layout(line, maxW)
        val w = layout.width + 28f * density; val h = layout.height + 20f * density
        val left = (headX - w * 0.3f).coerceIn(8f * density, screenW - w - 8f * density)
        val bottom = maxOf(headY - 14f * density, minTop + h)
        val box = RectF(left, bottom - h, left + w, bottom)
        fill.color = Color.argb(70, 40, 30, 20); canvas.drawRoundRect(box.left + 3f * density, box.top + 4f * density, box.right + 3f * density, box.bottom + 4f * density, 14f * density, 14f * density, fill)
        fill.color = CREAM; canvas.drawRoundRect(box, 14f * density, 14f * density, fill)
        path.reset(); path.moveTo(headX - 8f * density, bottom - 1f); path.lineTo(headX + 6f * density, bottom - 1f); path.lineTo(headX - 2f * density, headY - 2f * density); path.close()
        canvas.drawPath(path, fill)
        fill.color = HONEY; fill.style = Paint.Style.STROKE; fill.strokeWidth = 2f * density
        canvas.drawRoundRect(box, 14f * density, 14f * density, fill); fill.style = Paint.Style.FILL
        canvas.save(); canvas.translate(box.left + 14f * density, box.top + 10f * density); layout.draw(canvas); canvas.restore()
    }

    /** The narrator: a soft dark band under the top bar with white words. */
    fun subtitle(canvas: Canvas, line: String, top: Float, screenW: Float) {
        text.textSize = 17f * density; text.color = Color.WHITE
        val layout = layout(line, (screenW * 0.6f).toInt(), center = true)
        val w = layout.width + 40f * density; val h = layout.height + 16f * density
        val box = RectF((screenW - w) / 2f, top, (screenW + w) / 2f, top + h)
        fill.color = Color.argb(150, 30, 34, 40); canvas.drawRoundRect(box, h / 2f, h / 2f, fill)
        canvas.save(); canvas.translate(box.left + 20f * density, box.top + 8f * density); layout.draw(canvas); canvas.restore()
    }

    /** The crossroads: the question, and how to choose each route (jump = up, keep running = down). */
    fun question(canvas: Canvas, fork: RunFork, top: Float, screenW: Float, upLabel: String, downLabel: String) {
        val w = screenW * 0.62f
        text.textSize = 18f * density
        val q = layout(fork.question, (w - 32f * density).toInt(), center = true)
        text.textSize = 14.5f * density
        val up = layout("$upLabel  ${fork.up}", (w - 32f * density).toInt())
        val down = layout("$downLabel  ${fork.down}", (w - 32f * density).toInt())
        val h = q.height + up.height + down.height + 40f * density
        val box = RectF((screenW - w) / 2f, top, (screenW + w) / 2f, top + h)
        fill.color = Color.argb(230, 255, 248, 231); canvas.drawRoundRect(box, 18f * density, 18f * density, fill)
        fill.color = LAVENDER; fill.style = Paint.Style.STROKE; fill.strokeWidth = 3f * density
        canvas.drawRoundRect(box, 18f * density, 18f * density, fill); fill.style = Paint.Style.FILL
        var y = box.top + 12f * density
        text.textSize = 18f * density; text.color = GREEN
        q.drawAt(canvas, box.left + 16f * density, y); y += q.height + 8f * density
        text.textSize = 14.5f * density; text.color = INK
        up.drawAt(canvas, box.left + 16f * density, y); y += up.height + 6f * density
        down.drawAt(canvas, box.left + 16f * density, y)
    }

    private fun StaticLayout.drawAt(canvas: Canvas, x: Float, y: Float) { canvas.save(); canvas.translate(x, y); draw(canvas); canvas.restore() }

    private fun layout(s: String, maxW: Int, center: Boolean = false): StaticLayout {
        val width = minOf(maxW, text.measureText(s).toInt() + 2).coerceAtLeast(1)
        return StaticLayout.Builder.obtain(s, 0, s.length, TextPaint(text), width)
            .setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.05f).setMaxLines(3).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
    }

    private companion object {
        val INK = Color.parseColor("#3D3550")
        val GREEN = Color.parseColor("#1E5A2A")
        val CREAM = Color.parseColor("#FFF8E7")
        val HONEY = Color.parseColor("#C9A26A")
        val LAVENDER = Color.parseColor("#9B7BD0")
    }
}
