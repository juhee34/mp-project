package com.tinyrunners.school.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.tinyrunners.school.R

/**
 * The shared paper-and-bench scenery from the approved story-collection design (A art, docs/art/ui-screens).
 * Sits behind the village stories, the story collection, the character screens and the records so they read as
 * one place. The picture (bg_common_paper_wide, 2400×1080) has a frame drawn round its edge, trees at both sides
 * and a bench along the bottom, so it is always shown whole in height: it is scaled to the view's height, its left
 * and right ends (trees and frame corners) keep their shape, and only the plain middle (paper, bench, grass) is
 * stretched or squeezed to fill the width. That way the frame and the trees are never cut off, also on screens
 * that show the status bar. It uses the clean copy (bg_common_paper_clean_wide: the left trees, cut off square in A's
 * picture where a card once stood, replaced by the right bushes mirrored), the same picture the story screens use.
 * The hub and login use their own approved pictures (MockupFrame).
 */
class PaperBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,   // needed when placed in an XML layout (activity_main.xml)
) : View(context, attrs) {
    private val art: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg_common_paper_clean_wide,
        BitmapFactory.Options().apply { inScaled = false })
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val src = Rect()
    private val dst = RectF()

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        val iw = art.width; val ih = art.height
        if (iw <= 0 || ih <= 0 || width == 0 || height == 0) return
        val k = height.toFloat() / ih
        // Ends drawn at the same scale; the middle takes whatever width is left.
        val sideL = (SIDE_L * k).coerceAtMost(width / 2f)
        val sideR = ((iw - SIDE_R) * k).coerceAtMost(width / 2f)
        val h = height.toFloat()
        src.set(0, 0, SIDE_L, ih); dst.set(0f, 0f, sideL, h)
        canvas.drawBitmap(art, src, dst, paint)
        src.set(SIDE_L, 0, SIDE_R, ih); dst.set(sideL, 0f, width - sideR, h)
        canvas.drawBitmap(art, src, dst, paint)
        src.set(SIDE_R, 0, iw, ih); dst.set(width - sideR, 0f, width.toFloat(), h)
        canvas.drawBitmap(art, src, dst, paint)
    }

    private companion object {
        /** The stretchable middle of the picture (picture px): plain paper, the bench and the grass only. */
        const val SIDE_L = 700
        const val SIDE_R = 1700
    }
}
