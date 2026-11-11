package com.tinyrunners.school.audio

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tinyrunners.school.R
import com.tinyrunners.school.shop.ShopCatalog
import com.tinyrunners.school.shop.ShopStore

/** A screen that wants a particular song; every other screen plays the song chosen in the shop. */
interface MusicScreen {
    /** Raw resource to play while this screen is showing, or null for the shop-chosen song. */
    fun musicTrack(): Int?
}

/**
 * Background music on every screen. One looping player is shared and follows the screen on top: each screen that
 * opens (see [MusicLifecycle]) asks for its song, the same song simply keeps going, and a different one fades in
 * while the old one fades out. Leaving the app pauses it after a short delay.
 */
object Music {
    private var player: MediaPlayer? = null
    private var playing = 0
    private val handler = Handler(Looper.getMainLooper())
    private val pauseTask = Runnable { player?.takeIf { it.isPlaying }?.pause() }
    /** Current volume of each player, so a new fade starts where the last one stopped. */
    private val volumes = java.util.WeakHashMap<MediaPlayer, Float>()

    /** Story screens without a friend of their own: the main theme. */
    fun playStory(context: Context) = play(context, R.raw.main_theme)

    /** The music bought and chosen in the shop, or the main theme. */
    fun playGame(context: Context) = play(context, gameTrack(context))

    /** Plays [resId]; [force] plays even with music switched off in settings (the shop's "들어보기"). */
    fun play(context: Context, resId: Int, force: Boolean = false) {
        handler.removeCallbacks(pauseTask)
        if (!force && !MusicSettings(context).enabled) { stop(); return }
        val current = player
        if (playing == resId && current != null) {
            if (!current.isPlaying) current.start()
            return
        }
        val next = try {
            MediaPlayer.create(context.applicationContext, resId)?.apply { isLooping = true }
        } catch (e: Exception) {
            Log.w("Music", "cannot play $resId", e); null
        } ?: return
        player = next
        playing = resId
        // Cross-fade: the new song comes up while the old one goes down, so changes never cut abruptly.
        fade(next, VOLUME, start = true)
        current?.let { old -> fade(old, 0f) { old.release() } }
    }

    /** Waits a moment so the next screen can keep the song playing without a gap. */
    fun pauseSoon() { handler.postDelayed(pauseTask, PAUSE_DELAY_MS) }

    fun stop() {
        handler.removeCallbacks(pauseTask)
        player?.let { handler.removeCallbacksAndMessages(it) }
        player?.release()
        player = null
        playing = 0
    }

    /** Raw resource of the worn shop music if it exists in the app, otherwise the main theme. */
    fun gameTrack(context: Context): Int {
        val item = ShopCatalog.find(ShopStore.equippedMusic(context)) ?: return R.raw.main_theme
        return raw(context, item.track) ?: R.raw.main_theme
    }

    /** A friend's own song (res/raw/music_<id>), or the main theme when there is none. */
    fun friendTrack(context: Context, characterId: String): Int = raw(context, "music_$characterId") ?: R.raw.main_theme

    @SuppressLint("DiscouragedApi")
    private fun raw(context: Context, name: String?): Int? =
        name?.let { context.resources.getIdentifier(it, "raw", context.packageName) }?.takeIf { it != 0 }

    /**
     * Moves [p]'s volume to [to] in small steps. Any fade still running on the same player is cancelled first, so
     * two fades never fight over one player (that made the music wobble when songs changed quickly).
     */
    private fun fade(p: MediaPlayer, to: Float, start: Boolean = false, done: () -> Unit = {}) {
        handler.removeCallbacksAndMessages(p)
        val from = volumes[p] ?: 0f
        try { p.setVolume(from, from); if (start) p.start() } catch (_: IllegalStateException) { return }
        val begin = android.os.SystemClock.uptimeMillis()
        for (i in 1..FADE_STEPS) handler.postAtTime({
            val v = from + (to - from) * i / FADE_STEPS
            try { p.setVolume(v, v); volumes[p] = v } catch (_: IllegalStateException) { }
            if (i == FADE_STEPS) done()
        }, p, begin + FADE_MS * i / FADE_STEPS)
    }

    private const val VOLUME = 0.7f
    private const val PAUSE_DELAY_MS = 600L
    private const val FADE_MS = 600L
    private const val FADE_STEPS = 12
}

/**
 * Keeps the music following the screen on top: registered once by the app, so every screen (menu, selection,
 * shop, settings, records…) has music without each one asking for it.
 */
class MusicLifecycle : Application.ActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) {
        val track = (activity as? MusicScreen)?.musicTrack()
        if (track != null) Music.play(activity, track) else Music.playGame(activity)
    }

    override fun onActivityPaused(activity: Activity) = Music.pauseSoon()
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}

/** Settings switch for background music (on by default). */
class MusicSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("music", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean(KEY, true)
        set(value) { prefs.edit().putBoolean(KEY, value).apply() }

    private companion object { const val KEY = "enabled" }
}
