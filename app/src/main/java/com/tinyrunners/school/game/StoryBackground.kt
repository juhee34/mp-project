package com.tinyrunners.school.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import org.json.JSONObject
import kotlin.math.roundToInt

/** Keeps the approved pixels intact; decodes only once per game view. */
internal class StoryBackground private constructor(
    private val bitmap: Bitmap,
    private val region: Rect,
    private val floorY: Int,
    private val loopBitmap: Bitmap?,
) {
    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val source = Rect()
    private val destination = RectF()
    private val loopRegion = loopBitmap?.let { Rect(0, 0, it.width, it.height) }

    fun draw(canvas: Canvas, width: Float, height: Float, groundY: Float, distance: Float, worldScale: Float) {
        val scale = BackgroundMotion.scale(width, groundY, region.width(), floorY)
        val artWidth = region.width() * scale
        val travel = (distance * worldScale * 0.25f).coerceAtLeast(0f)
        val loop = loopBitmap
        if (loop == null) {
            // Missing optional art must never bring the home back during this run.
            val left = -travel.coerceAtMost((artWidth - width).coerceAtLeast(0f))
            drawPart(canvas, bitmap, region, left, artWidth, scale, height, groundY)
            return
        }
        val loopWidth = loop.width * scale
        val loopCrop = checkNotNull(loopRegion)
        for (part in RunningBackgroundMotion.parts(travel, artWidth, loopWidth, width)) {
            if (part.isLoop) drawPart(canvas, loop, loopCrop, part.left, loopWidth, scale, height, groundY)
            else drawPart(canvas, bitmap, region, part.left, artWidth, scale, height, groundY)
        }
    }

    private fun drawPart(canvas: Canvas, image: Bitmap, crop: Rect, left: Float, artWidth: Float, scale: Float, height: Float, groundY: Float) {
        val l = left.roundToInt().toFloat()
        val r = (left + artWidth).roundToInt().toFloat()
        source.set(crop.left, crop.top, crop.right, crop.top + floorY)
        destination.set(l, groundY - floorY * scale, r, groundY)
        canvas.drawBitmap(image, source, destination, paint)
        source.set(crop.left, crop.top + floorY, crop.right, crop.bottom)
        destination.set(l, groundY, r, height)
        canvas.drawBitmap(image, source, destination, paint)
    }

    companion object {
        fun load(context: Context, characterId: String): StoryBackground? {
            return try {
                val manifest = context.assets.open("backgrounds/manifest.json").bufferedReader().use { JSONObject(it.readText()) }
                val entries = manifest.getJSONArray("characters")
                val entry = (0 until entries.length()).map { entries.getJSONObject(it) }
                    .firstOrNull { it.getString("id") == characterId } ?: return null
                val r = entry.getJSONArray("rect")
                val region = Rect(r.getInt(0), r.getInt(1), r.getInt(0) + r.getInt(2), r.getInt(1) + r.getInt(3))
                val floor = entry.getInt("floorY")
                val bitmap = context.assets.open("backgrounds/" + entry.getString("source")).use {
                    BitmapFactory.decodeStream(it)
                } ?: return null
                require(region.left >= 0 && region.top >= 0 && region.right <= bitmap.width && region.bottom <= bitmap.height)
                require(region.width() > 0 && floor > 0 && floor < region.height())
                val loopSource = entry.optString("loopSource")
                val loop = if (loopSource.isNotBlank()) context.assets.open("backgrounds/" + loopSource).use {
                    BitmapFactory.decodeStream(it)
                } else null
                require(loop == null || (loop.width > 0 && loop.height == region.height()))
                StoryBackground(bitmap, region, floor, loop)
            } catch (e: Exception) {
                Log.w("StoryBackground", "Background unavailable for $characterId", e)
                null
            }
        }
    }
}
