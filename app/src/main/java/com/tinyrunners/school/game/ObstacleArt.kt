package com.tinyrunners.school.game

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.tinyrunners.school.R
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A's hometown props (docs/art/OBSTACLE_HANDOFF.md): three 48x80 sprites per friend. Visual only: the engine decides
 * where obstacles are and how big they are, identically for every friend; this class just picks and draws art.
 */
internal class ObstacleArt private constructor(private val props: List<Prop>) {
    /** One sprite and the part of it that is actually drawn (its opaque bounds). */
    class Prop(val bitmap: Bitmap, val opaque: Rect) {
        val aspect: Float get() = opaque.height().toFloat() / opaque.width()
    }

    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false; isDither = false }
    private val dst = RectF()
    private val towerProp: Prop = pickByAspect(TOWER_PIECE_ASPECT)

    /** Fills a box with the prop whose proportions are closest, so stretching stays small. */
    fun drawBox(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val prop = pickByAspect((bottom - top) / (right - left))
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

    private fun pickByAspect(target: Float): Prop =
        props.minBy { abs(ln(it.aspect / target)) }

    companion object {
        /** Squarish pieces read as stacked crates. */
        private const val TOWER_PIECE_ASPECT = 0.9f
        private val cache = mutableMapOf<String, ObstacleArt>()

        private val IDS = mapOf(
            "mint" to intArrayOf(R.drawable.obstacle_mint_fern_log, R.drawable.obstacle_mint_ribbon_rock, R.drawable.obstacle_mint_map_crate),
            "cheese" to intArrayOf(R.drawable.obstacle_cheese_plant_pot, R.drawable.obstacle_cheese_cushions, R.drawable.obstacle_cheese_sun_tin),
            "popo" to intArrayOf(R.drawable.obstacle_popo_snow_rock, R.drawable.obstacle_popo_shell_basket, R.drawable.obstacle_popo_harbor_crate),
            "lumi" to intArrayOf(R.drawable.obstacle_lumi_moss_stump, R.drawable.obstacle_lumi_pinecone_basket, R.drawable.obstacle_lumi_music_box),
            "nunu" to intArrayOf(R.drawable.obstacle_nunu_books, R.drawable.obstacle_nunu_return_crate, R.drawable.obstacle_nunu_letter_basket),
            "piko" to intArrayOf(R.drawable.obstacle_piko_paper_crate, R.drawable.obstacle_piko_rope_spool, R.drawable.obstacle_piko_toolbox),
        )

        /** Decoded once per friend and reused across runs; null (plain crates) if anything is missing. */
        @Synchronized
        fun load(resources: Resources, characterId: String): ObstacleArt? {
            cache[characterId]?.let { return it }
            val ids = IDS[characterId] ?: return null
            val options = BitmapFactory.Options().apply { inScaled = false }
            val props = ids.map { id ->
                val bitmap = BitmapFactory.decodeResource(resources, id, options) ?: return null
                val bounds = opaqueBounds(bitmap) ?: return null
                Prop(bitmap, bounds)
            }
            return ObstacleArt(props).also { cache[characterId] = it }
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
