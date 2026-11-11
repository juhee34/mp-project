package com.tinyrunners.school.quest

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** Draws a friend's item as a small 8×8 pixel icon with a soft glow, so it reads as "pick me up", not an obstacle. */
object ItemArt {
    private val paint = Paint().apply { isAntiAlias = false }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas, item: QuestItem, left: Float, top: Float, size: Float, withGlow: Boolean = true) {
        if (withGlow) {
            glow.color = Color.argb(110, 255, 248, 200)
            canvas.drawCircle(left + size / 2f, top + size / 2f, size * 0.62f, glow)
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
