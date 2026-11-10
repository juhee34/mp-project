package com.tinyrunners.school.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialFlowTest {
    @Test fun walksThroughFourPagesInOrderThenFinishes() {
        val flow = TutorialFlow()
        assertEquals(4, flow.count)
        val seen = mutableListOf<TutorialStep>()
        while (!flow.finished) { seen += flow.current; flow.next() }
        assertEquals(listOf(TutorialStep.JUMP, TutorialStep.DOUBLE_JUMP, TutorialStep.FLOATING, TutorialStep.PAUSE), seen)
    }

    @Test fun onlyTheLastPageIsLast() {
        val flow = TutorialFlow()
        repeat(3) { assertFalse(flow.isLast); flow.next() }
        assertTrue(flow.isLast)
        assertFalse(flow.finished)
    }

    @Test fun skipFinishesFromAnyPage() {
        val flow = TutorialFlow()
        flow.next()
        flow.skip()
        assertTrue(flow.finished)
    }

    @Test fun guideCoversEveryObstacleKind() {
        // If a new obstacle type is added, the guide should get a page for it too.
        assertEquals(3, ObstacleKind.entries.size)
        assertTrue(TutorialStep.JUMP in TutorialStep.entries)
        assertTrue(TutorialStep.DOUBLE_JUMP in TutorialStep.entries)
        assertTrue(TutorialStep.FLOATING in TutorialStep.entries)
    }
}
