package com.tinyrunners.school.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioMixTest {
    @Test fun overallTimesPart() {
        assertEquals(0.7f, AudioMix.gain(100, 70), 1e-6f)
        assertEquals(0.35f, AudioMix.gain(50, 70), 1e-6f)
        assertEquals(0f, AudioMix.gain(0, 100), 1e-6f)
    }

    @Test fun outOfRangeValuesAreClamped() {
        assertEquals(1f, AudioMix.gain(150, 120), 1e-6f)
        assertEquals(0f, AudioMix.gain(-5, 80), 1e-6f)
    }

    @Test fun talkSpeedsGoFromSlowToFast() {
        assertTrue(TalkSpeed.SLOW.charMs > TalkSpeed.NORMAL.charMs && TalkSpeed.NORMAL.charMs > TalkSpeed.FAST.charMs)
        // The normal speed is the one the talks always used.
        assertEquals(33f, TalkSpeed.NORMAL.charMs, 0f)
    }
}
