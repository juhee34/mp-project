package com.tinyrunners.school.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.tinyrunners.school.R

/**
 * Short sound effects for the talk scenes (scripts/make-sfx.cjs makes the files): a soft 도로록 blip while letters
 * type (each speaker at their own pitch; can be switched off on its own in settings), two notes when a choice or the
 * item panel appears and a chime when items are handed over.
 */
object Sfx {
    enum class Sound(val res: Int) { CHOICE(R.raw.sfx_choice), GIVE(R.raw.sfx_give), BLIP(R.raw.sfx_blip) }

    private var pool: SoundPool? = null
    private val ids = mutableMapOf<Sound, Int>()

    /** Loads the sounds; call early (loading takes a moment and a sound played before it is ready is skipped). */
    fun init(context: Context) {
        if (pool != null) return
        val p = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()
        Sound.entries.forEach { ids[it] = p.load(context.applicationContext, it.res, 1) }
        pool = p
    }

    private var lastBlip = 0L

    /** A typing blip for [speaker], at most every [BLIP_GAP_MS] so fast text does not buzz. */
    fun blip(context: Context, speaker: String) {
        if (!AudioSettings(context).typingSound) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastBlip < BLIP_GAP_MS) return
        lastBlip = now
        val (rate, volume) = VOICES[speaker] ?: (1f to 0.4f)
        play(context, Sound.BLIP, rate, volume)
    }

    /** Pitch and loudness of each voice: narration low and quiet, each friend at their own pitch. */
    private val VOICES = mapOf(
        "narrator" to (0.7f to 0.2f), "player" to (1f to 0.35f),
        "mint" to (1.0f to 0.4f), "cheese" to (1.15f to 0.4f), "popo" to (0.88f to 0.4f),
        "lumi" to (1.3f to 0.4f), "nunu" to (0.8f to 0.36f), "piko" to (1.22f to 0.4f),
    )
    private const val BLIP_GAP_MS = 70L

    fun play(context: Context, sound: Sound, rate: Float = 1f, volume: Float = 0.6f) {
        if (!SfxSettings(context).enabled) return
        init(context)
        val id = ids[sound] ?: return
        // Scaled by the overall and effects volumes from settings (80% effects ≈ the earlier fixed level).
        val v = (volume * AudioSettings(context).effectsGain / 0.8f).coerceIn(0f, 1f)
        pool?.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }
}

/** Settings switch for the talk sound effects (on by default). */
class SfxSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("music", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean(KEY, true)
        set(value) { prefs.edit().putBoolean(KEY, value).apply() }

    private companion object { const val KEY = "sfx" }
}
