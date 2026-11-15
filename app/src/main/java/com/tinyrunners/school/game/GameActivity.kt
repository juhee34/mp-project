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
        view.trail = com.tinyrunners.school.shop.ShopStore.equippedStyle(this)
        // One ID per run; the result screen saves with it, so a recreated result screen cannot double count.
        val runId = UUID.randomUUID().toString()
        // The home-screen widget shows the friend of the latest run.
        com.tinyrunners.school.widget.TodayFriend.remember(this, character.id)
        com.tinyrunners.school.widget.FriendWidget.refreshAll(this)
        view.onRunOver = {
            // Items picked up on the road go into the bag right away, so leaving the talk early keeps them.
            val picked = engine.collected
            if (picked > 0) com.tinyrunners.school.quest.ItemBagStore.forCurrentUser(this).add(character.id, picked) { }
            val bonus = engine.coins
            if (bonus > 0) com.tinyrunners.school.shop.ShopStore.forCurrentUser(this).addFound(bonus) { }
            view.postDelayed({
                if (!isFinishing) {
                    val result = ResultActivity.intent(this,
                        RunResult(runId, character.id, engine.meters, engine.score, engine.cleared))
                        .putExtra(EXTRA_STORY_ID, intent.getStringExtra(EXTRA_STORY_ID))
                        .putExtra(ResultActivity.EXTRA_ITEMS, picked)
                        .putExtra(ResultActivity.EXTRA_COINS, bonus)
                        .putExtra(ResultActivity.EXTRA_DISTANCE, engine.distance)
                    // The weather the run had, for the share card.
                    view.weather?.let { ResultActivity.putWeather(result, it) }
                    // Straight on (the story is told during the run now, not in a talk window): when the run earned coins
                    // an optional ad that doubles them, then the result screen.
                    val runCoins = com.tinyrunners.school.shop.ShopRules.coinsForRun(engine.meters) + bonus
                    val next = if (runCoins > 0) com.tinyrunners.school.result.AdOfferActivity.intent(this, result, runCoins) else result
                    com.tinyrunners.school.audio.Music.setRoute(null)
                    startActivity(next)
                    finish()
                }
            }, RESULT_DELAY_MS)
        }
        val weatherSettings = WeatherSettings(this)
        if (weatherSettings.enabled) {
            val city = weatherSettings.city
            val sky = weatherSettings.chosenSky
            val day = weatherSettings.chosenDay
            // A sky and time picked in settings show at once; the real weather fills in whatever was left to it.
            fun show(report: com.tinyrunners.school.weather.WeatherReport?) {
                view.weather = report
                // The weather also sets how hard the road is (a clear day easiest, a stormy night hardest).
                engine.difficulty = Difficulty.of(report)
            }
            show(com.tinyrunners.school.weather.WeatherChoice.apply(null, city, sky, day))
            if (!weatherSettings.fullyChosen) WeatherClient.fetch(city) { result ->
                if (!isDestroyed) result.onSuccess { show(com.tinyrunners.school.weather.WeatherChoice.apply(it, city, sky, day)) }
            }
        }
        if (!intent.getBooleanExtra(EXTRA_SHOW_GUIDE, false)) attachStory(character, engine, view)
        val onboarding = OnboardingStore(this)
        setContentView(FrameLayout(this).apply {
            addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            if (onboarding.shouldShowGameGuide || intent.getBooleanExtra(EXTRA_SHOW_GUIDE, false)) {
                // The guide sits on top and absorbs taps; the run only starts with a tap after it closes.
                lateinit var guide: TutorialOverlay
                guide = TutorialOverlay(this@GameActivity) {
                    onboarding.markGameGuideDone()
                    removeView(guide)
                }
                addView(guide, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            }
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

    /**
     * The story told during this run: the friend's next unread village story (or the one this run was started for),
     * told from run_stories.json — that story's own script, or the shared one with its title. Its crossroads is
     * scheduled in the engine; the choice colours the music and picks the ending; when the ending has been told the
     * story counts as seen (the same read record as before, so the story book and postcards follow).
     */
    private fun attachStory(character: com.tinyrunners.school.data.CharacterDefinition, engine: RunnerEngine, view: RunnerView) {
        val stories = com.tinyrunners.school.story.StoryRepository.load(this)
        val routes = RunRoutes(this)
        com.tinyrunners.school.progress.ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            val read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            val story = intent.getStringExtra(EXTRA_STORY_ID)?.let { id -> stories.firstOrNull { it.id == id } }
                ?: com.tinyrunners.school.story.Story.nextUnread(stories, character.id, read)
            val script = RunScriptParser.load(this, story?.id, character.name, story?.title ?: character.home) ?: return@loadAll
            val own = stories.filter { it.characterId == character.id }
            // The finale: every earlier story's route right (and this one) → happy by day, hidden at night; else bad.
            val endingFor: (Boolean) -> String = if (!script.finale) ({ up -> if (up) "up" else "down" }) else ({ up ->
                val earlierRight = own.filter { it.id != story?.id }.all { s ->
                    val chosen = routes.get(s.id)
                    val right = RunScriptParser.load(this, s.id, character.name, s.title)?.fork?.correct
                    chosen != null && (right == null || chosen == right)
                }
                val finalRight = script.fork?.correct.let { it == null || it == up }
                EndingRule.pick(earlierRight && finalRight, night = view.weather?.isDay == false)
            })
            view.story = RunStoryPlayer(script, endingFor, getString(com.tinyrunners.school.R.string.run_missed_clue))
            script.fork?.let { engine.scheduleFork(it.atMeters) }
            view.onForkChosen = { up ->
                story?.let { routes.set(it.id, up) }
                com.tinyrunners.school.audio.Music.setRoute(up)
            }
            view.onStoryFinished = {
                val s = view.story
                if (story != null) {
                    s?.endingKey?.let { routes.setEnding(story.id, it) }
                    com.tinyrunners.school.progress.ProgressStore.forCurrentUser(this).markStoryRead(character.id, story.id) { }
                    android.widget.Toast.makeText(this, getString(com.tinyrunners.school.R.string.run_story_done, script.title), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
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
        /** Set when a story started this run; the result screen then offers to continue the story. */
        const val EXTRA_STORY_ID = "storyId"
        /** Set by the "튜토리얼하기" button: show the game guide even if this account has already seen it. */
        const val EXTRA_SHOW_GUIDE = "showGuide"
        private const val RESULT_DELAY_MS = 900L
        fun intent(context: Context, characterId: String) =
            Intent(context, GameActivity::class.java).putExtra(EXTRA_CHARACTER_ID, characterId)
    }
}
