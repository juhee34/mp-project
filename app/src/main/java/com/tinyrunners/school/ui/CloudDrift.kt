package com.tinyrunners.school.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.SystemClock
import android.view.View

/**
 * Clouds drifting slowly across a [MockupFrame] background. The clouds were taken out of the background picture
 * into [cloudsRes] (the top band of the picture, transparent elsewhere); [skyRes] marks where that band is open sky,
 * so drifting clouds pass behind the tree, the title, the hills and the book's edge instead of over them. Both
 * pictures are as wide as the background (2400 px) and start at its top. The clouds go round inside [wrapLeft, wrapRight)
 * (background px); with [fade] > 0 they fade out and back in over that many px at both ends instead of being cut
 * (the menu's book page), otherwise they simply leave one edge of the screen and come back at the other.
 * Add it to the frame before the other parts so it sits behind them.
 */
@SuppressLint("ViewConstructor")
class CloudDrift(
    context: Context,
    private val frame: MockupFrame,
    cloudsRes: Int,
    skyRes: Int,
    private val wrapLeft: Int = 0,
    private val wrapRight: Int = 2400,
    private val fade: Int = 0,
) : View(context) {
    private val options = BitmapFactory.Options().apply { inScaled = false }
    private val clouds: Bitmap = BitmapFactory.decodeResource(resources, cloudsRes, options)
    private val sky: Bitmap = BitmapFactory.decodeResource(resources, skyRes, options)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val cut = Paint().apply { isFilterBitmap = false; xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    private val dst = RectF()
    private val src = android.graphics.Rect()
    private val edge = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    private var edgeFor = -1f
    private val start = SystemClock.uptimeMillis()
    private val tick = object : Runnable {
        override fun run() { invalidate(); postDelayed(this, FRAME_MS) }
    }

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); postDelayed(tick, FRAME_MS) }
    override fun onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        // Same placement as MockupFrame draws the background: the canvas height, centred.
        val h = MockupFrame.REF_H * frame.scale
        if (h <= 0f) return
        val k = h / BACKGROUND_H
        val w = BACKGROUND_W * k
        val left = (width - w) / 2f
        val top = (height - h) / 2f
        val bandW = clouds.width * k
        val bandH = clouds.height * k
        val span = wrapRight - wrapLeft
        val l0 = left + wrapLeft * k
        val l1 = left + wrapRight * k
        // Whole background pixels only, so the pixel art stays crisp.
        val shift = (((SystemClock.uptimeMillis() - start) / 1000f * SPEED) % span).toInt() * k
        canvas.save()
        canvas.clipRect(l0, top, l1, top + bandH)
        val layer = canvas.saveLayer(l0, top, l1, top + bandH, null)
        src.set(wrapLeft, 0, wrapRight, clouds.height)
        dst.set(l0 + shift, top, l1 + shift, top + bandH)
        canvas.drawBitmap(clouds, src, dst, pixel)
        dst.offset(-(l1 - l0), 0f)
        canvas.drawBitmap(clouds, src, dst, pixel)
        dst.set(left, top, left + bandW, top + bandH)
        canvas.drawBitmap(sky, null, dst, cut)
        if (fade > 0) {
            if (edgeFor != l0) {
                edgeFor = l0
                val f = fade.toFloat() / span
                edge.shader = android.graphics.LinearGradient(l0, 0f, l1, 0f,
                    intArrayOf(0, android.graphics.Color.BLACK, android.graphics.Color.BLACK, 0), floatArrayOf(0f, f, 1f - f, 1f),
                    android.graphics.Shader.TileMode.CLAMP)
            }
            canvas.drawRect(l0, top, l1, top + bandH, edge)
        }
        canvas.restoreToCount(layer)
        canvas.restore()
    }

    private companion object {
        const val BACKGROUND_W = 2400f
        const val BACKGROUND_H = 1080f
        /** Background pixels per second: the opening sky goes round in about 100 s, the menu page in about 75 s. */
        const val SPEED = 24f
        const val FRAME_MS = 50L
    }
}
