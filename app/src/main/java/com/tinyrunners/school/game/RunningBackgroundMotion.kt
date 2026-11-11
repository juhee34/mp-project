package com.tinyrunners.school.game

/** The home panorama is visited once. Only the separate running landscape repeats. */
internal object RunningBackgroundMotion {
    data class Part(val isLoop: Boolean, val left: Float)

    fun parts(travel: Float, introWidth: Float, loopWidth: Float, viewportWidth: Float): List<Part> {
        require(introWidth > 0f && loopWidth > 0f && viewportWidth > 0f)
        val position = travel.coerceAtLeast(0f)
        val result = mutableListOf<Part>()
        if (position < introWidth) result += Part(false, if (position == 0f) 0f else -position)
        var left = if (position < introWidth) introWidth - position
            else -((position - introWidth) % loopWidth)
        while (left < viewportWidth) {
            result += Part(true, left)
            left += loopWidth
        }
        return result
    }
}
