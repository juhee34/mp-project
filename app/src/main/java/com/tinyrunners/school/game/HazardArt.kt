package com.tinyrunners.school.game

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.tinyrunners.school.R
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A's approved hazard art (docs/art/approved/HANDOFF.md): per friend, a thorny branch for floating bars, a strip of
 * ground spikes and a landing plank. Visual only — the engine's hit boxes are unchanged, and the art's outline is
 * never used for collision. The plank art's flat light top is drawn at the plank's landing height.
 */
internal class HazardArt private constructor(
    private val air: Piece,
    private val spikes: Piece,
    private val platform: Piece,
) {
    /** A bitmap and its visible part. */
    class Piece(val bitmap: Bitmap, val opaque: Rect) {
        val aspect: Float get() = opaque.width().toFloat() / opaque.height()
    }

    // The art is painted pixel art at roughly 3–4× size; smoothing keeps it from shimmering when scaled down.
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val src = Rect()
    private val dst = RectF()

    /**
     * Floating bar: the branch across the bar's width, as tall as its proportions allow but kept close to the hit
     * box (between 1.2× and 1.7× its height) so what looks dangerous is about what is.
     */
    fun drawAir(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val boxH = bottom - top
        val h = ((right - left) / air.aspect).coerceIn(boxH * 1.2f, boxH * 1.7f)
        val cy = (top + bottom) / 2f
        dst.set(left, cy - h / 2f, right, cy + h / 2f)
        canvas.drawBitmap(air.bitmap, air.opaque, dst, paint)
    }

    /** Ground spikes: the strip repeated along the field, bottoms on the ground, a little taller than the hit box. */
    fun drawSpikes(canvas: Canvas, left: Float, ground: Float, right: Float, tip: Float) {
        val h = (ground - tip) * 1.25f
        val natural = h * spikes.aspect
        val count = max(1, ((right - left) / natural).roundToInt())
        val each = (right - left) / count
        for (i in 0 until count) {
            dst.set(left + i * each, ground - h, left + (i + 1) * each, ground)
            canvas.drawBitmap(spikes.bitmap, spikes.opaque, dst, paint)
        }
    }

    /**
     * The landing plank from [left] to [right] with its top edge at [top], [height] tall. Long planks keep the art's
     * ends and its centre ornament at their natural shape and repeat a plain stretch of board in between.
     */
    fun drawPlatform(canvas: Canvas, left: Float, top: Float, right: Float, height: Float) {
        val o = platform.opaque
        val scale = height / o.height()
        val naturalW = o.width() * scale
        val width = right - left
        if (width <= naturalW * 1.05f) {
            dst.set(left, top, right, top + height)
            canvas.drawBitmap(platform.bitmap, o, dst, paint)
            return
        }
        fun x(f: Float) = o.left + (o.width() * f).roundToInt()
        // Parts of the source: left end, plain board, centre ornament, plain board, right end.
        val leftEnd = x(0f) to x(0.15f)
        val plainL = x(0.15f) to x(0.28f)
        val centre = x(0.32f) to x(0.68f)
        val plainR = x(0.72f) to x(0.85f)
        val rightEnd = x(0.85f) to x(1f)
        fun part(p: Pair<Int, Int>, at: Float, w: Float = (p.second - p.first) * scale) {
            src.set(p.first, o.top, p.second, o.bottom)
            dst.set(at, top, at + w, top + height)
            canvas.drawBitmap(platform.bitmap, src, dst, paint)
        }
        fun w(p: Pair<Int, Int>) = (p.second - p.first) * scale
        part(leftEnd, left)
        part(rightEnd, right - w(rightEnd))
        val centreX = (left + right) / 2f - w(centre) / 2f
        // Fill each side with whole repeats of plain board, stretched slightly to meet exactly.
        fun fill(p: Pair<Int, Int>, from: Float, to: Float) {
            if (to <= from) return
            val count = max(1, ((to - from) / w(p)).roundToInt())
            val each = (to - from) / count
            for (i in 0 until count) part(p, from + i * each, each)
        }
        fill(plainL, left + w(leftEnd), centreX)
        fill(plainR, centreX + w(centre), right - w(rightEnd))
        part(centre, centreX)
    }

    companion object {
        private val cache = mutableMapOf<String, HazardArt>()

        private val IDS = mapOf(
            "mint" to intArrayOf(R.drawable.hazard_air_mint, R.drawable.hazard_spikes_mint, R.drawable.hazard_platform_mint),
            "cheese" to intArrayOf(R.drawable.hazard_air_cheese, R.drawable.hazard_spikes_cheese, R.drawable.hazard_platform_cheese),
            "popo" to intArrayOf(R.drawable.hazard_air_popo, R.drawable.hazard_spikes_popo, R.drawable.hazard_platform_popo),
            "lumi" to intArrayOf(R.drawable.hazard_air_lumi, R.drawable.hazard_spikes_lumi, R.drawable.hazard_platform_lumi),
            "nunu" to intArrayOf(R.drawable.hazard_air_nunu, R.drawable.hazard_spikes_nunu, R.drawable.hazard_platform_nunu),
            "piko" to intArrayOf(R.drawable.hazard_air_piko, R.drawable.hazard_spikes_piko, R.drawable.hazard_platform_piko),
        )

        /** Decoded once per friend; null (the drawn look) if anything is missing. */
        @Synchronized
        fun load(resources: Resources, characterId: String): HazardArt? {
            cache[characterId]?.let { return it }
            val ids = IDS[characterId] ?: return null
            val options = BitmapFactory.Options().apply { inScaled = false }
            val pieces = ids.map { id ->
                val bitmap = BitmapFactory.decodeResource(resources, id, options) ?: return null
                Piece(bitmap, ObstacleArt.opaqueBounds(bitmap) ?: return null)
            }
            return HazardArt(pieces[0], pieces[1], pieces[2]).also { cache[characterId] = it }
        }
    }
}
