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

    @Test fun oneAirJumpThenNoMoreUntilLanding() {
        val e = RunnerEngine(seed = 3)
        e.tap()
        e.run(0.1f)
        assertTrue("second tap in the air jumps", e.tap())
        assertEquals(e.config.airJumpVelocity, e.playerVy, 0.01f)
        e.run(0.05f)
        assertFalse("third tap does nothing", e.tap())
        e.run(2f)
        assertTrue(e.grounded)
        assertEquals(0, e.airJumpsUsed)
        assertTrue("jumps reset after landing", e.tap())
    }

    @Test fun doubleJumpGoesHigherThanSingle() {
        val single = RunnerEngine(seed = 3).apply { tap() }
        var singleApex = 0f
        single.run(1.5f) { singleApex = maxOf(singleApex, it.playerY) }
        val double = RunnerEngine(seed = 3).apply { tap() }
        var doubleApex = 0f
        var tapped = false
        double.run(1.5f) {
            if (!tapped && it.playerVy <= 0f && it.playerY > 0f) { it.tap(); tapped = true }
            doubleApex = maxOf(doubleApex, it.playerY)
        }
        val c = RunnerConfig()
        // The tap lands one 60 fps frame after the apex, so allow a few units below the ideal sum.
        assertEquals(c.apexHeight + c.airJumpVelocity * c.airJumpVelocity / (2 * c.gravity), doubleApex, 10f)
        assertTrue(doubleApex > singleApex)
    }

    /** Runs until an obstacle is on screen ahead of the player. */
    private fun RunnerEngine.runUntilObstacleAhead() {
        tap()
        var t = 0f
        while (obstacles.none { it.x > config.playerX + config.playerSize && it.x < config.playerX + config.skillRange } && t < 20f) {
            update(frame); t += frame
        }
        assertTrue("an obstacle appeared", t < 20f)
    }

    @Test fun skillRemovesNearestObstacleAheadAndCountsIt() {
        val e = RunnerEngine(seed = 7)
        e.runUntilObstacleAhead()
        val target = e.obstacles.filter { it.x + it.width > e.config.playerX }.minBy { it.x }
        val clearedBefore = e.cleared
        assertTrue(e.useSkill())
        assertFalse(target in e.obstacles)
        assertEquals(clearedBefore + 1, e.cleared)
        assertEquals(target, e.lastSkillTarget)
        assertEquals(e.config.skillCooldown, e.skillCooldownLeft, 0f)
    }

    @Test fun skillRestsTenSecondsAndPauseDoesNotCount() {
        val e = RunnerEngine(seed = 7)
        e.runUntilObstacleAhead()
        assertTrue(e.useSkill())
        assertFalse("cooling down", e.useSkill())
        e.pause(); e.run(30f); e.resume()
        assertEquals("paused time does not count", e.config.skillCooldown, e.skillCooldownLeft, 0.01f)
        e.run(e.config.skillCooldown - 0.5f) { it.autoJump() }
        assertFalse(e.skillReady)
        e.run(0.6f) { it.autoJump() }
        assertTrue(e.skillReady)
    }

    @Test fun skillWithNothingAheadKeepsCooldown() {
        val e = RunnerEngine(seed = 7)
        e.tap()
        assertTrue(e.obstacles.isEmpty())
        assertFalse(e.useSkill())
        assertEquals(0f, e.skillCooldownLeft, 0f)
        assertEquals(0, e.skillUses)
    }

    @Test fun skillOnlyWhileRunningAndResetsOnRestart() {
        val e = RunnerEngine(seed = 7)
        assertFalse("not before start", e.useSkill())
        e.runUntilObstacleAhead()
        assertTrue(e.useSkill())
        while (e.phase != RunnerEngine.Phase.OVER) e.update(frame)
        e.run(1f)
        e.tap()
        assertEquals(0f, e.skillCooldownLeft, 0f)
        assertEquals(0, e.skillUses)
        assertEquals(null, e.lastSkillTarget)
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
