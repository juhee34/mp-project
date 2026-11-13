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
 * pictures are as wide as the background (2400 px) and start at its top; the layer wraps around.
 * Add it to the frame before the other parts so it sits behind them.
 */
@SuppressLint("ViewConstructor")
class CloudDrift(context: Context, private val frame: MockupFrame, cloudsRes: Int, skyRes: Int) : View(context) {
    private val options = BitmapFactory.Options().apply { inScaled = false }
    private val clouds: Bitmap = BitmapFactory.decodeResource(resources, cloudsRes, options)
    private val sky: Bitmap = BitmapFactory.decodeResource(resources, skyRes, options)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val cut = Paint().apply { isFilterBitmap = false; xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    private val dst = RectF()
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
        // Whole background pixels only, so the pixel art stays crisp.
        val shift = (((SystemClock.uptimeMillis() - start) / 1000f * SPEED) % clouds.width).toInt() * k
        canvas.save()
        canvas.clipRect(left, top, left + bandW, top + bandH)
        val layer = canvas.saveLayer(left, top, left + bandW, top + bandH, null)
        dst.set(left + shift, top, left + shift + bandW, top + bandH)
        canvas.drawBitmap(clouds, null, dst, pixel)
        dst.offset(-bandW, 0f)
        canvas.drawBitmap(clouds, null, dst, pixel)
        dst.set(left, top, left + bandW, top + bandH)
        canvas.drawBitmap(sky, null, dst, cut)
        canvas.restoreToCount(layer)
        canvas.restore()
    }

    private companion object {
        const val BACKGROUND_W = 2400f
        const val BACKGROUND_H = 1080f
        /** Background pixels per second: a full lap takes a little under three minutes. */
        const val SPEED = 14f
        const val FRAME_MS = 50L
    }
}
