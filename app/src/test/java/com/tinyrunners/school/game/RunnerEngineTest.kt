package com.tinyrunners.school.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunnerEngineTest {
    private val frame = 1f / 60f

    private fun RunnerEngine.run(seconds: Float, onFrame: (RunnerEngine) -> Unit = {}) {
        var t = 0f
        while (t < seconds) { onFrame(this); update(frame); t += frame }
    }

    /** Taps when the next obstacle's centre is about half a jump away, like a steady player would. */
    private fun RunnerEngine.autoJump() {
        val c = config
        val next = obstacles.firstOrNull { it.x + it.width > c.playerX + c.playerInset } ?: return
        val obstacleCentre = next.x + next.width / 2
        val playerCentre = c.playerX + c.playerSize / 2
        if (grounded && obstacleCentre - playerCentre <= speed * c.airTime / 2) tap()
    }

    @Test fun readyUntilFirstTap() {
        val e = RunnerEngine(seed = 3)
        e.run(2f)
        assertEquals(RunnerEngine.Phase.READY, e.phase)
        assertEquals(0f, e.distance, 0f)
        assertTrue(e.tap())
        assertEquals(RunnerEngine.Phase.RUNNING, e.phase)
    }

    @Test fun jumpReachesExpectedApexAndLands() {
        val e = RunnerEngine(seed = 3)
        e.tap()
        var apex = 0f
        e.run(e.config.airTime * 0.9f) { apex = maxOf(apex, it.playerY) }
        assertEquals(e.config.apexHeight, apex, 4f)
        assertFalse("still airborne just before landing", e.grounded)
        e.run(0.1f)
        assertTrue(e.grounded)
    }

    @Test fun cannotDoubleJump() {
        val e = RunnerEngine(seed = 3)
        e.tap()
        e.run(0.1f)
        assertFalse(e.tap())
    }

    @Test fun everyObstacleSizeIsClearableAtEverySpeed() {
        val c = RunnerConfig()
        val needed = c.obstacleMaxWidth + c.playerHitWidth
        for (speed in listOf(c.startSpeed, (c.startSpeed + c.maxSpeed) / 2, c.maxSpeed)) {
            val travelledAboveTallest = speed * c.timeAbove(c.obstacleMaxHeight)
            assertTrue("speed $speed covers $travelledAboveTallest < $needed", travelledAboveTallest >= needed)
        }
        assertTrue(c.apexHeight > c.obstacleMaxHeight)
    }

    @Test fun obstacleGapsLeaveRoomToLandAndJumpAgain() {
        val e = RunnerEngine(seed = 11)
        e.tap()
        var checked = 0
        e.run(150f) { engine ->
            engine.autoJump()
            val list = engine.obstacles
            for (i in 1 until list.size) {
                val gap = list[i].x - (list[i - 1].x + list[i - 1].width)
                val required = engine.speed * engine.config.airTime + engine.config.playerSize
                assertTrue("gap $gap < $required at speed ${engine.speed}", gap >= required)
                checked++
            }
        }
        assertTrue(checked > 0)
    }

    @Test fun steadyPlayerSurvivesLongRunsOnManySeeds() {
        for (seed in 1L..20L) {
            val e = RunnerEngine(seed)
            e.tap()
            e.run(180f) { it.autoJump() }
            assertEquals("seed $seed ended at ${e.meters}m", RunnerEngine.Phase.RUNNING, e.phase)
            assertEquals(e.config.maxSpeed, e.speed, 0.01f)
            assertTrue(e.cleared > 100)
        }
    }

    @Test fun standingStillCollidesAndEndsRun() {
        val e = RunnerEngine(seed = 5)
        e.tap()
        e.run(30f)
        assertEquals(RunnerEngine.Phase.OVER, e.phase)
        val metersAtCrash = e.meters
        e.run(2f)
        assertEquals("no movement after crash", metersAtCrash, e.meters)
    }

    @Test fun restartWaitsForDelayThenClearsState() {
        val e = RunnerEngine(seed = 5)
        e.tap()
        e.run(30f)
        assertEquals(RunnerEngine.Phase.OVER, e.phase)
        // A crash immediately followed by a tap must not restart.
        val quick = RunnerEngine(seed = 5)
        quick.tap()
        while (quick.phase != RunnerEngine.Phase.OVER) quick.update(frame)
        assertFalse(quick.tap())

        assertTrue(e.canRestart)
        assertTrue(e.tap())
        assertEquals(RunnerEngine.Phase.READY, e.phase)
        assertEquals(0f, e.distance, 0f)
        assertEquals(0, e.cleared)
        assertEquals(0, e.score)
        assertTrue(e.obstacles.isEmpty())
        assertEquals(e.config.startSpeed, e.speed, 0f)
        assertEquals(0f, e.playerY, 0f)
    }

    @Test fun pauseFreezesTheWorld() {
        val e = RunnerEngine(seed = 2)
        e.tap()
        e.run(1f)
        e.pause()
        val distance = e.distance
        val y = e.playerY
        e.run(5f)
        assertEquals(RunnerEngine.Phase.PAUSED, e.phase)
        assertEquals(distance, e.distance, 0f)
        assertEquals(y, e.playerY, 0f)
        e.tap()
        assertEquals(RunnerEngine.Phase.RUNNING, e.phase)
    }

    @Test fun longFrameStallIsClamped() {
        val e = RunnerEngine(seed = 2)
        e.tap()
        e.update(10f)
        assertTrue("moved ${e.distance}", e.distance <= e.config.maxSpeed * e.config.maxFrame + 1f)
    }

    @Test fun movementIsFrameRateIndependent() {
        fun distanceAfter(fps: Int): Float {
            val e = RunnerEngine(seed = 9)
            e.tap()
            repeat(fps * 2) { e.update(1f / fps) }
            return e.distance
        }
        assertEquals(distanceAfter(30), distanceAfter(120), 6f)
    }

    @Test fun sameSeedGivesSameCourse() {
        val a = RunnerEngine(seed = 42); val b = RunnerEngine(seed = 42)
        a.tap(); b.tap()
        a.run(20f) { it.autoJump() }
        b.run(20f) { it.autoJump() }
        assertEquals(a.obstacles.map { it.x to it.height }, b.obstacles.map { it.x to it.height })
    }
}
