package com.tinyrunners.school.game

import kotlin.math.abs
import kotlin.math.ln

/** Which road prop draws an obstacle box. Pure (no Android), so it is unit tested. */
object PropPick {
    /** How much further from the box's shape than the closest prop a prop may be and still be chosen (log ratio). */
    const val TOLERANCE = 0.45f

    /**
     * Index into [aspects] (height / width of each prop) for a box of shape [target]: one of the props whose shape is
     * close to the closest one's, chosen by [seed] (the same seed always gives the same prop).
     */
    fun choose(aspects: List<Float>, target: Float, seed: Int): Int {
        require(aspects.isNotEmpty())
        val distance = aspects.map { abs(ln(it / target)) }
        val best = distance.min()
        val near = distance.indices.filter { distance[it] <= best + TOLERANCE }
        return near[(seed and Int.MAX_VALUE) % near.size]
    }
}
