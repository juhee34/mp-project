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
