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
    /** The screen's root: story talks and the ending picture are laid over the run here. */
    private var stage: FrameLayout? = null
    /** The rest card while it is open (back closes it and the run goes on). */
    private var pausePanel: PausePanel? = null
    private var goToResult: () -> Unit = {}
    private var weatherPending = false

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
        goToResult = {
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
                    // Straight on to the result screen (the story is told during the run).
                    com.tinyrunners.school.audio.Music.setRoute(null)
                    startActivity(result)
                    finish()
                }
            }, RESULT_DELAY_MS)
        }
        view.onRunOver = { goToResult() }
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
            if (!weatherSettings.fullyChosen) {
                weatherPending = true
                WeatherClient.fetch(this, city) { result ->
                    weatherPending = false
                    if (!isDestroyed) result.onSuccess { show(com.tinyrunners.school.weather.WeatherChoice.apply(it, city, sky, day)) }
                }
            }
        }
        if (!intent.getBooleanExtra(EXTRA_SHOW_GUIDE, false)) {
            // The run waits for the opening talk (it loads the progress and the weather first), at most a few seconds.
            view.holdStart = true
            view.postDelayed({ view.holdStart = false }, HOLD_START_MAX_MS)
            attachStory(character, engine, view)
        }
        // 정지 opens the rest card: 계속 달리기, 친구 바꾸기, 집으로 돌아가기.
        view.onStopTapped = {
            stage?.let { root ->
                view.restCard = true
                val card = PausePanel(this, character.id,
                    onResume = { view.restCard = false; pausePanel = null; engine.resume() },
                    onChangeFriend = {
                        com.tinyrunners.school.audio.Music.setRoute(null)
                        startActivity(com.tinyrunners.school.MainActivity.selectionIntent(this)); finish()
                    },
                    onHome = {
                        com.tinyrunners.school.audio.Music.setRoute(null)
                        com.tinyrunners.school.ui.HomeButton.goToMenu(this); finish()
                    })
                pausePanel = card
                root.addView(card, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            }
        }
        val onboarding = OnboardingStore(this)
        setContentView(FrameLayout(this).also { stage = it }.apply {
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
     * The story of this run in four acts (RunStory): 발단 — an RPG talk before the start; 전개·위기 — bubbles and the
     * crossroads while running; 절정 — the run pauses for a talk that follows the route; 결말 — a last, slightly faster
     * stretch to a goal, then the ending picture with its talk, after which the story counts as seen (the same read
     * record as before, so the story book and postcards follow) and the result screen opens. A crash before the end
     * leaves the story unseen, to be run again. The friend's next unread village story is told, or the one this run
     * was started for; the weather of the day joins the opening and the run.
     */
    private fun attachStory(character: com.tinyrunners.school.data.CharacterDefinition, engine: RunnerEngine, view: RunnerView) {
        val stories = com.tinyrunners.school.story.StoryRepository.load(this)
        val routes = RunRoutes(this)
        val color = runCatching { android.graphics.Color.parseColor(character.color) }.getOrDefault(android.graphics.Color.rgb(30, 90, 42))
        com.tinyrunners.school.progress.ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            val read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            val story = intent.getStringExtra(EXTRA_STORY_ID)?.let { id -> stories.firstOrNull { it.id == id } }
                ?: com.tinyrunners.school.story.Story.nextUnread(stories, character.id, read)
            // Wait (a few seconds at most) for the real weather, so the story can speak of it.
            var waited = 0
            fun begin() {
                if (isDestroyed) return
                if (weatherPending && waited < 30) { waited++; view.postDelayed({ begin() }, 100); return }
                val weatherKey = StoryWeather.key(view.weather)
                val script = RunScriptParser.load(this, story?.id, character.name, story?.title ?: character.home, character.id, weatherKey)
                    ?: run { view.holdStart = false; return }
                val own = stories.filter { it.characterId == character.id }
                fun allRight(up: Boolean): Boolean {
                    val earlierRight = own.filter { it.id != story?.id }.all { s ->
                        val chosen = routes.get(s.id)
                        val right = RunScriptParser.load(this, s.id, character.name, s.title)?.fork?.correct
                        chosen != null && (right == null || chosen == right)
                    }
                    return earlierRight && script.fork?.correct.let { it == null || it == up }
                }
                view.story = RunStoryPlayer(script, missedLine = getString(com.tinyrunners.school.R.string.run_missed_clue), tellEndingInRun = false)
                script.fork?.let { engine.scheduleFork(it.atMeters) }

                fun talk(lines: List<RunLine>, dim: Boolean, done: () -> Unit) {
                    val root = stage
                    if (lines.isEmpty() || root == null) { done(); return }
                    view.cinematic = true
                    // The run never goes on under a talk: it stops here and goes on when the talk is over.
                    val stopped = engine.phase == RunnerEngine.Phase.RUNNING
                    engine.pause()
                    // Under the game guide (if it is open), above the run.
                    root.addView(StoryDialogView(this, character.name, color, lines, dim, friendId = character.id) {
                        view.cinematic = false
                        if (stopped) engine.resume()
                        done()
                    },
                        minOf(1, root.childCount), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                }

                // 1. 발단: before the start.
                talk(script.opening, dim = true) { view.holdStart = false }

                var choiceMeters = -1
                var choice: Boolean? = null
                var climaxDone = false
                var endingShown = false
                view.onForkChosen = { up ->
                    choice = up; choiceMeters = engine.meters
                    story?.let { routes.set(it.id, up) }
                    com.tinyrunners.school.audio.Music.setRoute(up)
                }
                var asked = false
                view.onTick = tick@{
                    // 2. 위기: when the crossroads comes on screen, stop and ask with two buttons.
                    val ahead = engine.forkDistance
                    if (!asked && ahead != null && ahead <= FORK_ASK_AHEAD) {
                        val fork = script.fork
                        val root = stage
                        if (fork != null && root != null) {
                            asked = true
                            engine.pause()
                            view.cinematic = true
                            root.addView(ForkChoiceView(this, fork) { up ->
                                view.cinematic = false
                                engine.chooseFork(up)
                                engine.resume()
                            }, minOf(1, root.childCount), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                        }
                    }
                    val chosen = choice ?: return@tick
                    // 3. 절정: a few metres after the crossroads, once the bubbles have finished.
                    // (Never later than CLIMAX_LATEST_METERS: any bubble still waiting is dropped then.)
                    val bubblesDone = view.story?.current == null || engine.meters >= choiceMeters + CLIMAX_LATEST_METERS
                    if (!climaxDone && engine.meters >= choiceMeters + CLIMAX_AFTER_METERS && bubblesDone) {
                        climaxDone = true
                        view.story = null
                        engine.pause()
                        val key = if (script.finale) (if (allRight(chosen)) "right" else "wrong") else (if (chosen) "up" else "down")
                        talk(script.climax[key].orEmpty(), dim = true) {
                            // 4. 결말: the last stretch, a little faster, to the goal.
                            view.goal = engine.meters to engine.meters + script.goalMeters
                            engine.stretch = FINAL_STRETCH
                            engine.resume()
                        }
                    }
                    val goal = view.goal
                    if (climaxDone && !endingShown && goal != null && engine.meters >= goal.second) {
                        endingShown = true
                        engine.pause()
                        view.goal = null
                        val ending = if (script.finale) EndingRule.pick(allRight(chosen), night = view.weather?.isDay == false) else (if (chosen) "up" else "down")
                        // A wrong route in an ordinary story ends its talk with a hint (never told at the choice itself).
                        val missed = !script.finale && script.fork?.correct.let { it != null && it != chosen }
                        val lines = if (script.finale) script.endings[ending].orEmpty()
                            else script.closing[ending].orEmpty() + if (missed) listOf(RunLine(0, true, getString(com.tinyrunners.school.R.string.run_missed_clue))) else emptyList()
                        val root = stage ?: return@tick
                        val picture = EndingPicture(this, character.id, ending, script.title)
                        root.addView(picture, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                        view.cinematic = true
                        root.addView(StoryDialogView(this, character.name, color, lines, dim = false) {
                            if (story != null) {
                                routes.setEnding(story.id, ending)
                                com.tinyrunners.school.progress.ProgressStore.forCurrentUser(this).markStoryRead(character.id, story.id) { }
                                android.widget.Toast.makeText(this, getString(com.tinyrunners.school.R.string.run_story_done, script.title), android.widget.Toast.LENGTH_SHORT).show()
                            }
                            goToResult()
                        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    }
                }
            }
            begin()
        }
    }

    override fun onResume() {
        super.onResume()
        runnerView?.startLoop()
    }

    @Deprecated("Back on the rest card closes it and runs on.")
    override fun onBackPressed() {
        val card = pausePanel
        if (card != null) card.close() else @Suppress("DEPRECATION") super.onBackPressed()
    }

    override fun onPause() {
        // Leaving the app or a dialog on top always pauses; the player resumes with a tap.
        runnerView?.engine?.pause()
        runnerView?.stopLoop()
        super.onPause()
    }

    companion object {
        /** The longest the run waits for its opening talk before a tap may start it anyway. */
        private const val HOLD_START_MAX_MS = 8000L
        const val EXTRA_CHARACTER_ID = "characterId"
        /** Set when a story started this run; the result screen then offers to continue the story. */
        const val EXTRA_STORY_ID = "storyId"
        /** Set by the "튜토리얼하기" button: show the game guide even if this account has already seen it. */
        const val EXTRA_SHOW_GUIDE = "showGuide"
        private const val RESULT_DELAY_MS = 900L
        /** Metres between the crossroads and the climax talk. */
        private const val CLIMAX_AFTER_METERS = 8
        /** The crossroads question comes up when the raised path is this close (world units, about two seconds). */
        private const val FORK_ASK_AHEAD = 420f
        private const val CLIMAX_LATEST_METERS = 25
        /** The last stretch runs this much faster than the road would. */
        private const val FINAL_STRETCH = 1.06f
        fun intent(context: Context, characterId: String) =
            Intent(context, GameActivity::class.java).putExtra(EXTRA_CHARACTER_ID, characterId)
    }
}
