package com.tinyrunners.school.audio

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Sound and feel settings beyond the on/off switches (MusicSettings, SfxSettings): separate volumes for everything,
 * music and effects (0–100), vibration, and how fast dialogue is typed. Stored on the device.
 */
class AudioSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("music", Context.MODE_PRIVATE)

    var master: Int
        get() = prefs.getInt(KEY_MASTER, 100)
        set(value) { prefs.edit().putInt(KEY_MASTER, value.coerceIn(0, 100)).apply() }
    var music: Int
        get() = prefs.getInt(KEY_MUSIC, 70)
        set(value) { prefs.edit().putInt(KEY_MUSIC, value.coerceIn(0, 100)).apply() }
    var effects: Int
        get() = prefs.getInt(KEY_SFX, 80)
        set(value) { prefs.edit().putInt(KEY_SFX, value.coerceIn(0, 100)).apply() }
    var vibration: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) { prefs.edit().putBoolean(KEY_VIBRATION, value).apply() }
    var talkSpeed: TalkSpeed
        get() = TalkSpeed.entries.firstOrNull { it.name == prefs.getString(KEY_TALK_SPEED, null) } ?: TalkSpeed.NORMAL
        set(value) { prefs.edit().putString(KEY_TALK_SPEED, value.name).apply() }

    /** Player volume for background music (0..1). */
    val musicGain: Float get() = AudioMix.gain(master, music)
    /** Multiplier for sound effects (0..1). */
    val effectsGain: Float get() = AudioMix.gain(master, effects)

    private companion object {
        const val KEY_MASTER = "master_volume"
        const val KEY_MUSIC = "music_volume"
        const val KEY_SFX = "sfx_volume"
        const val KEY_VIBRATION = "vibration"
        const val KEY_TALK_SPEED = "talk_speed"
    }
}

/** How quickly dialogue appears: milliseconds per character. */
enum class TalkSpeed(val charMs: Float) { SLOW(55f), NORMAL(33f), FAST(16f) }

/** Pure volume arithmetic, so it is unit tested. */
object AudioMix {
    /** Overall × part, both 0–100 → 0..1. */
    fun gain(master: Int, part: Int): Float = (master.coerceIn(0, 100) / 100f) * (part.coerceIn(0, 100) / 100f)
}

/** Short vibrations during a run (when switched on in settings): a bump on a crash, a tick when an item is picked up. */
object Haptics {
    fun crash(view: View) = buzz(view, HapticFeedbackConstants.LONG_PRESS)
    fun pickup(view: View) = buzz(view, HapticFeedbackConstants.KEYBOARD_TAP)

    private fun buzz(view: View, kind: Int) {
        if (AudioSettings(view.context).vibration) view.performHapticFeedback(kind)
    }
}
