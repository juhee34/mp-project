package com.tinyrunners.school.quest

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.tinyrunners.school.R

/**
 * Draws a friend's item with a soft glow, so it reads as "pick me up", not an obstacle. Items with A's approved art
 * (docs/art/approved/: 햇빛 조각, 조개껍데기, 솔방울, 책갈피, 종이배) use it; 라일's fern leaf keeps its 8×8 pixel icon
 * until its art is separated from the grass (still pending in the handoff).
 */
object ItemArt {
    private val paint = Paint().apply { isAntiAlias = false }
    private val smooth = Paint(Paint.FILTER_BITMAP_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()
    private var art: Map<String, Pair<Bitmap, Rect>> = emptyMap()

    private val IDS = mapOf(
        "cheese" to R.drawable.item_cheese, "popo" to R.drawable.item_popo, "lumi" to R.drawable.item_lumi,
        "nunu" to R.drawable.item_nunu, "piko" to R.drawable.item_piko,
    )

    /** Decodes the approved item art once (called when the app starts). Missing art falls back to the pixel icons. */
    @Synchronized
    fun load(resources: Resources) {
        if (art.isNotEmpty()) return
        val options = BitmapFactory.Options().apply { inScaled = false }
        art = IDS.mapNotNull { (id, res) ->
            val bitmap = BitmapFactory.decodeResource(resources, res, options) ?: return@mapNotNull null
            val bounds = com.tinyrunners.school.game.ObstacleArt.opaqueBounds(bitmap) ?: return@mapNotNull null
            id to (bitmap to bounds)
        }.toMap()
    }

    fun draw(canvas: Canvas, item: QuestItem, left: Float, top: Float, size: Float, withGlow: Boolean = true) {
        if (withGlow) {
            glow.color = Color.argb(110, 255, 248, 200)
            canvas.drawCircle(left + size / 2f, top + size / 2f, size * 0.62f, glow)
        }
        art[item.friendId]?.let { (bitmap, bounds) ->
            // Fit the picture inside the square, keeping its shape.
            val k = size / maxOf(bounds.width(), bounds.height())
            val w = bounds.width() * k; val h = bounds.height() * k
            dst.set(left + (size - w) / 2f, top + (size - h) / 2f, left + (size + w) / 2f, top + (size + h) / 2f)
            canvas.drawBitmap(bitmap, bounds, dst, smooth)
            return
        }
        val px = size / 8f
        item.pixels.forEachIndexed { row, line ->
            line.forEachIndexed { col, c ->
                val color = PALETTE[c] ?: return@forEachIndexed
                paint.color = color
                canvas.drawRect(left + col * px, top + row * px, left + (col + 1) * px, top + (row + 1) * px, paint)
            }
        }
    }

    private val PALETTE = mapOf(
        'g' to Color.rgb(74, 140, 74), 'G' to Color.rgb(132, 196, 108), 's' to Color.rgb(110, 84, 50),
        'y' to Color.rgb(244, 196, 64), 'Y' to Color.rgb(255, 232, 128), 'w' to Color.rgb(255, 255, 250),
        'p' to Color.rgb(214, 128, 140), 'P' to Color.rgb(250, 204, 196),
        'b' to Color.rgb(120, 76, 40), 'B' to Color.rgb(176, 120, 64),
        'm' to Color.rgb(120, 100, 170), 'M' to Color.rgb(206, 196, 236), 'r' to Color.rgb(236, 120, 150),
        'W' to Color.rgb(230, 236, 246), 'k' to Color.rgb(92, 128, 176), 'K' to Color.rgb(196, 220, 244),
    )
}
