package com.tinyrunners.school.game

import kotlin.math.max

/** One-pass camera for approved panoramas whose endpoints are not seamless. */
internal object BackgroundMotion {
    fun scale(width: Float, groundY: Float, imageWidth: Int, floorY: Int): Float =
        max(width / imageWidth, groundY / floorY)

    fun offset(distance: Float, worldScale: Float, imageWidth: Float, viewportWidth: Float): Float =
        (distance * worldScale * 0.25f).coerceIn(0f, max(0f, imageWidth - viewportWidth))
}
