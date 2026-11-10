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

    /**
     * Plays like a steady player: one jump centred on a box, a double jump (second tap at the peak) centred on a
     * tower, and no jump at all under a floating bar.
     */
    private fun RunnerEngine.autoJump() {
        val c = config
        val next = obstacles.firstOrNull { it.x + it.width > c.playerX + c.playerInset } ?: return
        val obstacleCentre = next.x + next.width / 2
        val playerCentre = c.playerX + c.playerSize / 2
        val ahead = obstacleCentre - playerCentre
        when (next.kind) {
            ObstacleKind.BOX -> if (grounded && ahead <= speed * c.airTime / 2) tap()
            ObstacleKind.TOWER -> {
                val (enter, leave) = c.doubleJumpWindow(next.top)!!
                if (grounded && ahead <= speed * (enter + leave) / 2) tap()
                else if (!grounded && airJumpsUsed == 0 && playerVy <= 0f) tap()
            }
            ObstacleKind.FLOAT -> Unit
        }
    }

    /** Clear gap needed between two neighbours, matching the engine's rule. */
    private fun RunnerEngine.requiredGap(a: Obstacle, b: Obstacle): Float {
        val nearTower = a.kind == ObstacleKind.TOWER || b.kind == ObstacleKind.TOWER
        return speed * (if (nearTower) config.doubleAirTime else config.airTime) + config.playerSize
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

    @Test fun everyObstacleSizeIsClearableAtEverySpeed() {
        val c = RunnerConfig()
        val needed = c.obstacleMaxWidth + c.playerHitWidth
        for (speed in listOf(c.startSpeed, (c.startSpeed + c.maxSpeed) / 2, c.maxSpeed)) {
            val travelledAboveTallest = speed * c.timeAbove(c.obstacleMaxHeight)
            assertTrue("speed $speed covers $travelledAboveTallest < $needed", travelledAboveTallest >= needed)
        }
        assertTrue(c.apexHeight > c.obstacleMaxHeight)
    }

    @Test fun towersNeedTheDoubleJumpAndItIsEnoughAtEverySpeed() {
        val c = RunnerConfig()
        assertTrue("a single jump must not clear a tower", c.apexHeight < c.towerMinHeight)
        val (enter, leave) = c.doubleJumpWindow(c.towerMaxHeight)!!
        val needed = c.towerMaxWidth + c.playerHitWidth
        for (speed in listOf(c.startSpeed, c.maxSpeed)) {
            val travelled = speed * (leave - enter)
            assertTrue("speed $speed covers $travelled < $needed over a tower", travelled >= needed)
        }
    }

    @Test fun boxesKeepPropFriendlyProportions() {
        val e = RunnerEngine(seed = 12)
        e.tap()
        val c = e.config
        var boxes = 0
        e.run(90f) { engine ->
            engine.autoJump()
            for (o in engine.obstacles.filter { it.kind == ObstacleKind.BOX }) {
                boxes++
                assertTrue("width ${o.width}", o.width in c.obstacleMinWidth..c.obstacleMaxWidth)
                assertTrue("height ${o.height}", o.height in c.obstacleMinHeight..c.obstacleMaxHeight)
                // Sprites are 0.5-1.23 tall per unit width; boxes stay close so the art is barely stretched.
                assertTrue("aspect ${o.height / o.width}", o.height / o.width in 0.55f..1.05f)
            }
        }
        assertTrue(boxes > 0)
    }

    @Test fun floatingBarsLetTheRunnerPassUnderneath() {
        val c = RunnerConfig()
        assertTrue(c.playerHitHeight + 2 * c.obstacleInset < c.floatMinBottom)
    }

    @Test fun everyObstacleTypeAppearsAfterTheCalmStart() {
        val e = RunnerEngine(seed = 4)
        e.tap()
        val seen = mutableSetOf<ObstacleKind>()
        var firstNonBoxAt = Float.MAX_VALUE
        e.run(120f) { engine ->
            engine.autoJump()
            for (o in engine.obstacles) {
                seen += o.kind
                if (o.kind != ObstacleKind.BOX) firstNonBoxAt = minOf(firstNonBoxAt, engine.elapsed)
            }
        }
        assertEquals(ObstacleKind.entries.toSet(), seen)
        assertTrue("towers and bars only after the calm start", firstNonBoxAt >= e.config.mixedAfterSeconds)
    }

    @Test fun jumpingIntoAFloatingBarEndsTheRun() {
        val c = RunnerConfig()
        val e = RunnerEngine(seed = 4)
        e.tap()
        // Play normally until a bar is just ahead, then jump straight into it.
        var t = 0f
        while (t < 120f && e.phase == RunnerEngine.Phase.RUNNING) {
            val bar = e.obstacles.firstOrNull { it.kind == ObstacleKind.FLOAT && it.x > c.playerX + c.playerSize }
            if (bar != null && bar.x - (c.playerX + c.playerSize) < 30f && e.grounded) {
                e.tap()
                e.run(0.6f)
                break
            }
            e.autoJump(); e.update(frame); t += frame
        }
        assertEquals(RunnerEngine.Phase.OVER, e.phase)
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
                val required = engine.requiredGap(list[i - 1], list[i])
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
            // About one obstacle every two seconds once towers widen some gaps (seed 1: 94 in 3 minutes).
            assertTrue("seed $seed cleared only ${e.cleared}", e.cleared > 60)
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
