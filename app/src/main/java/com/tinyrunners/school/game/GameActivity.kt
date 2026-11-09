package com.tinyrunners.school.game

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.widget.FrameLayout
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.RunResult
import com.tinyrunners.school.result.ResultActivity
import com.tinyrunners.school.weather.WeatherClient
import com.tinyrunners.school.weather.WeatherSettings
import java.util.UUID

/** Game entry point. Receives a validated character ID and runs the shared runner. */
class GameActivity : Activity() {
    private var runnerView: RunnerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val characterId = intent.getStringExtra(EXTRA_CHARACTER_ID)
        val character = CharacterRepository(this).find(characterId.orEmpty())
        if (character == null) { finish(); return }

        // Physics never depends on the character; only the drawing does.
        val engine = RunnerEngine(seed = System.nanoTime())
        val view = RunnerView(this, character, engine)
        runnerView = view
        // One ID per run; the result screen saves with it, so a recreated result screen cannot double count.
        val runId = UUID.randomUUID().toString()
        view.onRunOver = {
            view.postDelayed({
                if (!isFinishing) {
                    startActivity(ResultActivity.intent(this,
                        RunResult(runId, character.id, engine.meters, engine.score, engine.cleared)))
                    finish()
                }
            }, RESULT_DELAY_MS)
        }
        val weatherSettings = WeatherSettings(this)
        if (weatherSettings.enabled) {
            WeatherClient.fetch(weatherSettings.city) { result ->
                if (!isDestroyed) result.onSuccess { view.weather = it }
            }
        }
        setContentView(FrameLayout(this).apply {
            addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            setOnApplyWindowInsetsListener { v, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                insets
            }
        })
    }

    override fun onResume() {
        super.onResume()
        runnerView?.startLoop()
    }

    override fun onPause() {
        // Leaving the app or a dialog on top always pauses; the player resumes with a tap.
        runnerView?.engine?.pause()
        runnerView?.stopLoop()
        super.onPause()
    }

    companion object {
        const val EXTRA_CHARACTER_ID = "characterId"
        private const val RESULT_DELAY_MS = 900L
        fun intent(context: Context, characterId: String) =
            Intent(context, GameActivity::class.java).putExtra(EXTRA_CHARACTER_ID, characterId)
    }
}
