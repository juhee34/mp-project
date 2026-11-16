package com.tinyrunners.school.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PropPickTest {
    @Test fun aBoxGetsAPropOfASimilarShape() {
        // A tall pot (2.0), a square crate (1.0) and a flat log (0.5).
        val aspects = listOf(2.0f, 1.0f, 0.5f)
        for (seed in 0 until 20) assertEquals(1, PropPick.choose(aspects, 1.0f, seed))
        for (seed in 0 until 20) assertEquals(2, PropPick.choose(aspects, 0.45f, seed))
    }

    @Test fun similarPropsTakeTurnsAndOneObstacleKeepsItsProp() {
        // Two square props (a crate and a bought basket): different obstacles get either, each always the same one.
        val aspects = listOf(1.0f, 1.1f, 0.5f)
        val picks = (0 until 40).map { PropPick.choose(aspects, 1.0f, it) }.toSet()
        assertEquals(setOf(0, 1), picks)
        assertEquals(PropPick.choose(aspects, 1.0f, 12345), PropPick.choose(aspects, 1.0f, 12345))
        assertTrue(PropPick.choose(aspects, 1.0f, Int.MIN_VALUE) in 0..1)   // negative seeds too
    }
}
