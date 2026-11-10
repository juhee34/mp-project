package com.tinyrunners.school.game

import com.tinyrunners.school.characters.SpriteFrame

/** Selects artwork from simulation state without changing the simulation. */
internal object RunnerAnimation {
    private val runFrames = arrayOf(SpriteFrame.RUN_0, SpriteFrame.RUN_1, SpriteFrame.RUN_2, SpriteFrame.RUN_3)

    fun frame(engine: RunnerEngine, idleSeconds: Float): SpriteFrame {
        if (engine.phase == RunnerEngine.Phase.OVER) return SpriteFrame.FAIL_0
        if (engine.phase == RunnerEngine.Phase.READY) {
            return if ((idleSeconds / 0.5f).toInt() % 2 == 0) SpriteFrame.IDLE_0 else SpriteFrame.IDLE_1
        }
        if (!engine.grounded) {
            return if (engine.playerVy > 0f) SpriteFrame.JUMP_0 else SpriteFrame.JUMP_1
        }
        // At the starting speed of 260 world units/s, this gives ten frames/s.
        return runFrames[(engine.distance / 26f).toInt() % runFrames.size]
    }
}
