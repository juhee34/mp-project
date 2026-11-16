package com.tinyrunners.school.game

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A's hometown props (docs/art/OBSTACLE_HANDOFF.md), 48x80 sprites. Every run has the plain ones (a wooden crate, a
 * snowy rock, a mossy stump, a fern log); the pretty ones join them once bought in the shop (ShopCatalog.roadProps).
 * Visual only: the engine decides where obstacles are and how big they are, identically for every friend and whatever
 * is owned; this class just picks and draws art.
 */
internal class ObstacleArt private constructor(private val props: List<Prop>) {
    /** One sprite and the part of it that is actually drawn (its opaque bounds). */
    class Prop(val bitmap: Bitmap, val opaque: Rect) {
        val aspect: Float get() = opaque.height().toFloat() / opaque.width()
    }

    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false; isDither = false }
    private val dst = RectF()
    /** Towers stack the plain crate (the first prop). */
    private val towerProp: Prop = props.first()

    /**
     * Fills a box with one of the props whose proportions are close (so stretching stays small); [seed] picks among
     * them and stays the same for one obstacle, so it keeps its picture while it crosses the screen.
     */
    fun drawBox(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, seed: Int = 0) {
        val prop = props[PropPick.choose(props.map { it.aspect }, (bottom - top) / (right - left), seed)]
        dst.set(left, top, right, bottom)
        canvas.drawBitmap(prop.bitmap, prop.opaque, dst, paint)
    }

    /** A tower is a stack of the friend's most crate-like prop, filling the tower's exact bounds. */
    fun drawTower(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val prop = towerProp
        val pieceNatural = (right - left) * prop.aspect
        val pieces = max(2, ((bottom - top) / pieceNatural).roundToInt())
        val pieceHeight = (bottom - top) / pieces
        for (i in 0 until pieces) {
            val pb = bottom - i * pieceHeight
            dst.set(left, pb - pieceHeight, right, pb)
            canvas.drawBitmap(prop.bitmap, prop.opaque, dst, paint)
        }
    }

    companion object {
        private val decoded = mutableMapOf<Int, Prop>()

        /**
         * The art for a run from these drawables (ShopCatalog.roadProps: plain first, then the owned pretty ones). Each
         * picture is decoded once and kept; null (plain drawn crates) if none can be read.
         */
        @Synchronized
        fun load(resources: Resources, ids: List<Int>): ObstacleArt? {
            val options = BitmapFactory.Options().apply { inScaled = false }
            val props = ids.mapNotNull { id ->
                decoded[id] ?: run {
                    val bitmap = BitmapFactory.decodeResource(resources, id, options) ?: return@mapNotNull null
                    val bounds = opaqueBounds(bitmap) ?: return@mapNotNull null
                    Prop(bitmap, bounds).also { decoded[id] = it }
                }
            }
            return if (props.isEmpty()) null else ObstacleArt(props)
        }

        /** Smallest rectangle containing every visible pixel (alpha > 0). */
        fun opaqueBounds(bitmap: Bitmap): Rect? {
            val w = bitmap.width; val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            var l = w; var t = h; var r = -1; var b = -1
            for (y in 0 until h) for (x in 0 until w) {
                if (pixels[y * w + x] ushr 24 != 0) {
                    if (x < l) l = x; if (x > r) r = x
                    if (y < t) t = y; if (y > b) b = y
                }
            }
            return if (r < 0) null else Rect(l, t, r + 1, b + 1)
        }
    }
}
