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

    @Test fun cameraLoopsBackToTheFirstSceneAfterTheEnd() {
        // 0.25 × distance × scale: 6000 × 2 × 0.25 = 3000 = one picture width, so the first scene is back.
        assertEquals(0f, BackgroundMotion.offset(6000f, 2f, 3000f), 0.001f)
        assertEquals(500f, BackgroundMotion.offset(7000f, 2f, 3000f), 0.001f)
        // Far into a long run the offset still stays inside one picture.
        val far = BackgroundMotion.offset(1_000_000f, 2f, 3000f)
        assertTrue(far >= 0f && far < 3000f)
    }

    @Test fun nextCopyFillsTheViewportWhenThePictureEnds() {
        // Picture still covers the screen: one copy.
        assertEquals(listOf(-100f), BackgroundMotion.copies(100f, 3000f, 960f))
        // Its end is on screen at x = 500: the next copy starts exactly there.
        assertEquals(listOf(-2500f, 500f), BackgroundMotion.copies(2500f, 3000f, 960f))
    }

    @Test fun resetAndUnchangedDistanceGiveStableCamera() {
        assertEquals(0f, BackgroundMotion.offset(0f, 2f, 3000f), 0.001f)
        assertEquals(100f, BackgroundMotion.offset(200f, 2f, 3000f), 0.001f)
        assertEquals(100f, BackgroundMotion.offset(200f, 2f, 3000f), 0.001f)
    }
}
