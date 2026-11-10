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

/** Keeps the approved pixels intact; decodes only once per game view. */
internal class StoryBackground private constructor(
    private val bitmap: Bitmap,
    private val region: Rect,
    private val floorY: Int,
) {
    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val source = Rect()
    private val destination = RectF()

    fun draw(canvas: Canvas, width: Float, height: Float, groundY: Float, distance: Float, worldScale: Float) {
        val scale = BackgroundMotion.scale(width, groundY, region.width(), floorY)
        val artWidth = region.width() * scale
        val offset = BackgroundMotion.offset(distance, worldScale, artWidth, width)
        source.set(region.left, region.top, region.right, region.top + floorY)
        destination.set(-offset, groundY - floorY * scale, artWidth - offset, groundY)
        canvas.drawBitmap(bitmap, source, destination, paint)
        // Only the underground band fills the engine's fixed ground thickness.
        // Scenery keeps its aspect ratio; the physics floor never moves.
        source.set(region.left, region.top + floorY, region.right, region.bottom)
        destination.set(-offset, groundY, artWidth - offset, height)
        canvas.drawBitmap(bitmap, source, destination, paint)
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
                StoryBackground(bitmap, region, floor)
            } catch (e: Exception) {
                Log.w("StoryBackground", "Background unavailable for $characterId", e)
                null
            }
        }
    }
}
