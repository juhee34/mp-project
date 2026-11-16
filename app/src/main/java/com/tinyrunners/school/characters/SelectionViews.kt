package com.tinyrunners.school.characters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import com.tinyrunners.school.story.BackgroundThumbs
import com.tinyrunners.school.ui.CardShelf
import kotlin.math.max

/**
 * The big stage of the friend selection: the chosen friend standing at home (the start of their running
 * background), large, breathing between their two idle frames with a soft shadow, inside a rounded frame in the
 * friend's colour.
 */
internal class ShowcaseStage(context: Context) : View(context) {
    private var friendId: String? = null
    private var tint = CardShelf.GREEN
    private var scene: Pair<Bitmap, Float>? = null
    private var sprites: CharacterSprites? = null
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val src = Rect()
    private val dst = RectF()
    private val clip = Path()
    private var frame = 0
    private val tick = object : Runnable {
        override fun run() { frame++; invalidate(); postDelayed(this, IDLE_MS) }
    }

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun show(id: String, color: Int) {
        if (id != friendId) {
            friendId = id
            scene = BackgroundThumbs.of(context, id)
            sprites = CharacterSprites.load(resources, id)
        }
        tint = color
        invalidate()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); postDelayed(tick, IDLE_MS) }
    override fun onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val d = resources.displayMetrics.density
        val r = 18 * d
        clip.reset(); clip.addRoundRect(0f, 0f, w, h, r, r, Path.Direction.CW)
        canvas.save(); canvas.clipPath(clip)
        var floor = h * 0.86f
        val art = scene
        if (art != null) {
            val (bitmap, floorFraction) = art
            // Fill the stage, cropping around the floor so the ground stays near the bottom.
            val scale = max(h / (bitmap.height * 0.8f), w / bitmap.width)
            val visibleW = (w / scale).coerceAtMost(bitmap.width.toFloat())
            val visibleH = (h / scale).coerceAtMost(bitmap.height.toFloat())
            val floorPx = bitmap.height * floorFraction
            val top = (floorPx - visibleH * 0.86f).coerceIn(0f, bitmap.height - visibleH)
            src.set(0, top.toInt(), visibleW.toInt(), (top + visibleH).toInt())
            dst.set(0f, 0f, w, h)
            canvas.drawBitmap(bitmap, src, dst, pixel)
            floor = (floorPx - top) * scale
        } else {
            paint.color = Color.rgb(225, 239, 218); canvas.drawRect(0f, 0f, w, h, paint)
        }
        sprites?.let {
            // Big: about 60% of the stage height, whole pixels only.
            val px = max(1f, Math.round(h * 0.6f / CharacterSprites.SIZE).toFloat())
            val size = CharacterSprites.SIZE * px
            val cx = w * 0.5f
            val bob = if (frame % 2 == 0) 0f else px * 1.5f
            paint.color = Color.argb(70, 30, 30, 20)
            canvas.drawOval(cx - size * 0.3f, floor - px * 3, cx + size * 0.3f, floor + px * 3, paint)
            val left = cx - CharacterSprites.CENTER_X * px
            val topY = floor - CharacterSprites.FOOT_EDGE_Y * px - bob
            dst.set(left, topY, left + size, topY + size)
            canvas.drawBitmap(it[if (frame % 2 == 0) SpriteFrame.IDLE_0 else SpriteFrame.IDLE_1], null, dst, pixel)
        }
        canvas.restore()
        // Frame in the friend's colour.
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 4 * d; paint.color = tint
        canvas.drawRoundRect(2 * d, 2 * d, w - 2 * d, h - 2 * d, r, r, paint)
        paint.style = Paint.Style.FILL
    }

    private companion object { const val IDLE_MS = 520L }
}

/** One round portrait in the roster: the friend's sprite on a circle of their colour; the chosen one has a green ring. */
@SuppressLint("ViewConstructor")
internal class RosterPortrait(context: Context, id: String, private val tint: Int, private val chosen: Boolean) : View(context) {
    private val sprites = CharacterSprites.load(resources, id)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()

    init { isClickable = true; isFocusable = true }

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height).toFloat()
        val d = resources.displayMetrics.density
        val cx = width / 2f; val cy = height / 2f
        paint.color = blend(tint, Color.rgb(255, 249, 235), if (chosen) 0.45f else 0.65f)
        canvas.drawCircle(cx, cy, s / 2f - 2 * d, paint)
        sprites?.let {
            dst.set(cx - s * 0.42f, cy - s * 0.42f, cx + s * 0.42f, cy + s * 0.42f)
            canvas.drawBitmap(it[SpriteFrame.IDLE_0], null, dst, pixel)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (if (chosen) 3.5f else 2f) * d
        paint.color = if (chosen) CardShelf.GREEN else Color.rgb(220, 192, 154)
        canvas.drawCircle(cx, cy, s / 2f - 2 * d, paint)
        paint.style = Paint.Style.FILL
        if (!chosen) alpha = 0.85f
    }

    private fun blend(a: Int, b: Int, t: Float) = Color.rgb(
        (Color.red(a) * (1 - t) + Color.red(b) * t).toInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).toInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt())
}
