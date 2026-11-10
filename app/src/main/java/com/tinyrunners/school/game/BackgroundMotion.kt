package com.tinyrunners.school.game

import kotlin.math.max

/**
 * Camera for the hometown panoramas. The picture scrolls at a quarter of the run speed and, when its right end
 * has passed, starts again from its first scene, so the background keeps changing for the whole run.
 */
internal object BackgroundMotion {
    fun scale(width: Float, groundY: Float, imageWidth: Int, floorY: Int): Float =
        max(width / imageWidth, groundY / floorY)

    /** How far the picture has scrolled left, always within one picture width: 0 ≤ offset < [imageWidth]. */
    fun offset(distance: Float, worldScale: Float, imageWidth: Float): Float {
        if (imageWidth <= 0f) return 0f
        val travelled = max(0f, distance * worldScale * PARALLAX)
        return travelled % imageWidth
    }

    /**
     * Left edges of the copies needed to cover a viewport of [viewportWidth]: the current picture and, once its
     * end comes into view, the next copy starting right after it.
     */
    fun copies(offset: Float, imageWidth: Float, viewportWidth: Float): List<Float> {
        val lefts = mutableListOf(-offset)
        var next = -offset + imageWidth
        while (imageWidth > 0f && next < viewportWidth) { lefts += next; next += imageWidth }
        return lefts
    }

    private const val PARALLAX = 0.25f
}
