package com.tinyrunners.school.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundMotionTest {
    @Test fun sceneCoversWideAndTallViewportsAboveFixedFloor() {
        for ((width, floor) in listOf(960f to 468f, 2400f to 900f, 1080f to 2200f)) {
            val scale = BackgroundMotion.scale(width, floor, 2161, 329)
            assertTrue(2161 * scale >= width)
            assertTrue(329 * scale >= floor)
        }
    }

    @Test fun cameraStopsAtEndWithoutWrappingOrBlankSpace() {
        assertEquals(2040f, BackgroundMotion.offset(100000f, 2f, 3000f, 960f), 0.001f)
        assertEquals(0f, BackgroundMotion.offset(100f, 2f, 960f, 960f), 0.001f)
    }

    @Test fun resetAndUnchangedDistanceGiveStableCamera() {
        assertEquals(0f, BackgroundMotion.offset(0f, 2f, 3000f, 960f), 0.001f)
        assertEquals(100f, BackgroundMotion.offset(200f, 2f, 3000f, 960f), 0.001f)
        assertEquals(100f, BackgroundMotion.offset(200f, 2f, 3000f, 960f), 0.001f)
    }
}
