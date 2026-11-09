package com.tinyrunners.school.characters

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import kotlin.math.min

/** A replaceable portrait slot for characters without finished art. */
internal class CharacterPortrait(color: String) : Drawable() {
    private val accent = Color.parseColor(color)
    private val paint = Paint()
    private var opacity = 255

    override fun draw(canvas: Canvas) {
        val size = min(bounds.width(), bounds.height()).toFloat()
        canvas.save()
        canvas.translate(bounds.exactCenterX() - size / 2, bounds.exactCenterY() - size / 2)
        canvas.scale(size / 16, size / 16)
        block(canvas, 0, 0, 16, 16, accent)
        block(canvas, 2, 2, 14, 14, CREAM)
        block(canvas, 4, 5, 12, 12, INK)
        block(canvas, 5, 4, 11, 13, INK)
        block(canvas, 5, 5, 11, 12, accent)
        block(canvas, 6, 7, 7, 8, INK)
        block(canvas, 9, 7, 10, 8, INK)
        block(canvas, 7, 9, 9, 10, INK)
        canvas.restore()
    }

    private fun block(canvas: Canvas, left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        paint.color = color
        paint.alpha = opacity
        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
    }

    override fun setAlpha(alpha: Int) {
        opacity = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Required by Drawable")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        val CREAM = Color.rgb(255, 248, 231)
        val INK = Color.rgb(43, 59, 47)
    }
}
