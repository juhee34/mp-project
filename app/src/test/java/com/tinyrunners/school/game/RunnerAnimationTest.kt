package com.tinyrunners.school.game

import com.tinyrunners.school.characters.SpriteFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunnerAnimationTest {
    private val step = 1f / 120f

    @Test fun idleAlternatesEveryHalfSecond() {
        val engine = RunnerEngine()
        assertEquals(SpriteFrame.IDLE_0, RunnerAnimation.frame(engine, 0f))
        assertEquals(SpriteFrame.IDLE_0, RunnerAnimation.frame(engine, 0.49f))
        assertEquals(SpriteFrame.IDLE_1, RunnerAnimation.frame(engine, 0.5f))
        assertEquals(SpriteFrame.IDLE_0, RunnerAnimation.frame(engine, 1f))
    }

    @Test fun jumpUsesVelocityAndReturnsToRunningAfterLanding() {
        val engine = RunnerEngine()
        engine.tap()
        assertEquals(SpriteFrame.JUMP_0, RunnerAnimation.frame(engine, 0f))
        while (engine.playerVy > 0f) engine.update(step)
        assertEquals(SpriteFrame.JUMP_1, RunnerAnimation.frame(engine, 0f))
        while (!engine.grounded) engine.update(step)
        assertTrue(RunnerAnimation.frame(engine, 0f).name.startsWith("RUN_"))
    }

    @Test fun runVisitsAllFourFramesByDistanceAtBothSpeeds() {
        for (speed in listOf(260f, 520f)) {
            val engine = RunnerEngine(config = RunnerConfig(startSpeed = speed, maxSpeed = speed))
            engine.tap()
            while (!engine.grounded) engine.update(step)
            val seen = mutableSetOf<SpriteFrame>()
            repeat(60) {
                seen += RunnerAnimation.frame(engine, 0f)
                engine.update(step)
            }
            assertEquals(setOf(SpriteFrame.RUN_0, SpriteFrame.RUN_1, SpriteFrame.RUN_2, SpriteFrame.RUN_3), seen)
        }
    }

    @Test fun pausedFrameStaysFrozenOnGroundAndInAir() {
        for (airborne in listOf(true, false)) {
            val engine = RunnerEngine()
            engine.tap()
            if (!airborne) while (!engine.grounded) engine.update(step)
            val before = RunnerAnimation.frame(engine, 1f)
            engine.pause()
            repeat(120) { engine.update(step) }
            assertEquals(before, RunnerAnimation.frame(engine, 9f))
        }
    }

    @Test fun failureOverridesJumpAndRestartRestoresIdle() {
        val engine = RunnerEngine()
        engine.tap()
        repeat(2400) { engine.update(step) }
        assertEquals(RunnerEngine.Phase.OVER, engine.phase)
        assertEquals(SpriteFrame.FAIL_0, RunnerAnimation.frame(engine, 100f))
        engine.restart()
        assertEquals(SpriteFrame.IDLE_0, RunnerAnimation.frame(engine, 0f))
        engine.tap()
        assertEquals(SpriteFrame.JUMP_0, RunnerAnimation.frame(engine, 0f))
    }

    @Test fun selectingFramesHasNoEffectOnPhysicsOrScore() {
        val baseline = RunnerEngine(seed = 8)
        val animated = RunnerEngine(seed = 8)
        baseline.tap()
        animated.tap()
        repeat(1200) {
            baseline.update(step)
            animated.update(step)
            RunnerAnimation.frame(animated, it * step)
            assertEquals(baseline.playerY, animated.playerY, 0f)
            assertEquals(baseline.distance, animated.distance, 0f)
            assertEquals(baseline.phase, animated.phase)
            assertEquals(baseline.score, animated.score)
        }
    }
}
