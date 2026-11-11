package com.tinyrunners.school.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tinyrunners.school.R
import com.tinyrunners.school.shop.ShopCatalog
import com.tinyrunners.school.shop.ShopStore

/**
 * Background music for runs and story screens. One looping player is shared, so moving between music screens
 * (talk → run → crash talk → result) keeps the song going; leaving them pauses it after a short delay.
 * Screens call [play] in onResume and [pauseSoon] in onPause.
 */
object Music {
    private var player: MediaPlayer? = null
    private var playing = 0
    private val handler = Handler(Looper.getMainLooper())
    private val pauseTask = Runnable { player?.takeIf { it.isPlaying }?.pause() }

    /** Story screens: always the main theme. */
    fun playStory(context: Context) = play(context, R.raw.main_theme)

    /** Runs and their talks/result: the music bought and chosen in the shop, or the main theme. */
    fun playGame(context: Context) = play(context, gameTrack(context))

    fun play(context: Context, resId: Int) {
        handler.removeCallbacks(pauseTask)
        if (!MusicSettings(context).enabled) { stop(); return }
        if (playing != resId) {
            stop()
            player = try {
                MediaPlayer.create(context.applicationContext, resId)?.apply { isLooping = true; setVolume(VOLUME, VOLUME) }
            } catch (e: Exception) {
                Log.w("Music", "cannot play $resId", e); null
            }
            playing = if (player != null) resId else 0
        }
        player?.takeIf { !it.isPlaying }?.start()
    }

    /** Waits a moment so the next music screen can keep the song playing without a gap. */
    fun pauseSoon() { handler.postDelayed(pauseTask, PAUSE_DELAY_MS) }

    fun stop() {
        handler.removeCallbacks(pauseTask)
        player?.release()
        player = null
        playing = 0
    }

    /** Raw resource of the worn shop music if it exists in the app, otherwise the main theme. */
    @SuppressLint("DiscouragedApi")
    fun gameTrack(context: Context): Int {
        val item = ShopCatalog.find(ShopStore.equippedMusic(context)) ?: return R.raw.main_theme
        val name = item.track ?: return R.raw.main_theme
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (id != 0) id else R.raw.main_theme
    }

    private const val VOLUME = 0.7f
    private const val PAUSE_DELAY_MS = 600L
}

/** Settings switch for background music (on by default). */
class MusicSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("music", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean(KEY, true)
        set(value) { prefs.edit().putBoolean(KEY, value).apply() }

    private companion object { const val KEY = "enabled" }
}
