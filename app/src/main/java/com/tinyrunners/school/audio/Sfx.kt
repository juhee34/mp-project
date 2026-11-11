package com.tinyrunners.school.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.tinyrunners.school.R

/**
 * Short sound effects for the talk scenes (scripts/make-sfx.cjs makes the files): two notes when a choice or the
 * item panel appears and a chime when items are handed over. (Typing and next-line sounds were removed on request.)
 */
object Sfx {
    enum class Sound(val res: Int) { CHOICE(R.raw.sfx_choice), GIVE(R.raw.sfx_give) }

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

    fun play(context: Context, sound: Sound, rate: Float = 1f, volume: Float = 0.6f) {
        if (!SfxSettings(context).enabled) return
        init(context)
        val id = ids[sound] ?: return
        pool?.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
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
