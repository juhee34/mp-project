package com.tinyrunners.school.game

import org.junit.Assert.*
import org.junit.Test

class RunningBackgroundMotionTest {
    @Test fun homeAppearsOnlyBeforeTheFirstDeparture() {
        for (distance in listOf(3000f, 3001f, 6000f, 1_000_000f)) {
            assertTrue(RunningBackgroundMotion.parts(distance, 3000f, 1200f, 960f).all { it.isLoop })
        }
        assertFalse(RunningBackgroundMotion.parts(0f, 3000f, 1200f, 960f).first().isLoop)
    }

    @Test fun introductionJoinsTheFirstLoopWithoutAGap() {
        val parts = RunningBackgroundMotion.parts(2500f, 3000f, 1200f, 960f)
        assertEquals(listOf(RunningBackgroundMotion.Part(false, -2500f), RunningBackgroundMotion.Part(true, 500f)), parts)
    }

    @Test fun everyViewportIsCoveredAcrossManyCycles() {
        for (viewport in listOf(640f, 960f, 2400f)) for (distance in listOf(0f, 2999.5f, 3000f, 4199.5f, 4200f, 100_000f)) {
            val parts = RunningBackgroundMotion.parts(distance, 3000f, 1200f, viewport)
            assertTrue(parts.first().left <= 0f)
            parts.zipWithNext().forEach { (a, b) ->
                assertEquals(a.left + if (a.isLoop) 1200f else 3000f, b.left, 0.01f)
            }
            val last = parts.last()
            assertTrue(last.left + (if (last.isLoop) 1200f else 3000f) >= viewport)
        }
    }

    @Test fun pauseIsStableAndANewRunReturnsHome() {
        assertEquals(RunningBackgroundMotion.parts(9000f, 3000f, 1200f, 960f), RunningBackgroundMotion.parts(9000f, 3000f, 1200f, 960f))
        assertEquals(RunningBackgroundMotion.Part(false, 0f), RunningBackgroundMotion.parts(0f, 3000f, 1200f, 960f).first())
    }
}
