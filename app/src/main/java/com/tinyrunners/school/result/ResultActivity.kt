package com.tinyrunners.school.result

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.progress.RunResult
import com.tinyrunners.school.records.RecordsActivity
import com.tinyrunners.school.story.StoryRunSignal
import com.tinyrunners.school.ui.BagButton
import com.tinyrunners.school.ui.Page
import com.tinyrunners.school.ui.ScoreBoard
import kotlin.math.roundToInt

/**
 * Shows one finished run and saves it: the score board on the left (a storybook page with a ribbon, the friend and
 * their word, the distance and score counting up, a 새 기록 stamp, how close to the best, what the run brought home),
 * what to do next on the right in the story-collection buttons, over the friend's road where the run ended. Saving uses the run's ID, so recreating this screen (rotation, returning from the background) never
 * counts the same run twice.
 */
class ResultActivity : Activity() {
    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val run = RunResult(
            runId = intent.getStringExtra(EXTRA_RUN_ID).orEmpty(),
            characterId = intent.getStringExtra(GameActivity.EXTRA_CHARACTER_ID).orEmpty(),
            meters = intent.getIntExtra(EXTRA_METERS, 0),
            score = intent.getIntExtra(EXTRA_SCORE, 0),
            cleared = intent.getIntExtra(EXTRA_CLEARED, 0),
            // Real special weather only (a sky picked by hand in settings does not count) — for the postcards.
            specialWeather = weatherOf(intent)?.let { !it.chosen && com.tinyrunners.school.weather.SpecialDay.key(it) != null } == true,
        )
        val character = CharacterRepository(this).find(run.characterId)
        if (character == null || run.runId.isEmpty()) { finish(); return }
        val storyId = intent.getStringExtra(GameActivity.EXTRA_STORY_ID)

        // Left: the score board (docs/research/SCORE_SCREEN_RESEARCH.md) on a storybook page over the friend's road.
        val tint = try { Color.parseColor(character.color) } catch (_: IllegalArgumentException) { ScoreBoard.GREEN }
        val (pageWrap, left) = ScoreBoard.page(this, getString(R.string.result_eyebrow))
        // The friend and a word from them about the run.
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(ScoreBoard.portrait(this, character.id, tint), LinearLayout.LayoutParams(dp(58), dp(58)))
        val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        names.addView(label(getString(R.string.result_with_friend), 12f, ScoreBoard.SOFT, bold = true))
        names.addView(label(character.name, 22f, ScoreBoard.INK, bold = true))
        head.addView(names)
        val said = com.tinyrunners.school.talk.ShortLineRepository.pick(this, character.id, "stumble")
        if (said != null) head.addView(ScoreBoard.bubble(this, "“$said”"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) })
        left.addView(head)

        // Distance and score, large, counting up; a 새 기록 stamp on the distance when it is a new best.
        val tiles = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false; isBaselineAligned = false }
        val (distanceTile, distanceValue) = ScoreBoard.tile(this, getString(R.string.result_distance_label), getString(R.string.result_meters, 0))
        val (scoreTile, scoreValue) = ScoreBoard.tile(this, getString(R.string.result_score_label), "0")
        val distanceBox = FrameLayout(this).apply { clipChildren = false; addView(distanceTile, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)) }
        val stamp = ScoreBoard.stamp(this, getString(R.string.result_stamp_new)).apply { visibility = android.view.View.GONE }
        distanceBox.addView(stamp, FrameLayout.LayoutParams(dp(62), dp(62), Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = dp(8) })
        tiles.addView(distanceBox, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        tiles.addView(scoreTile, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
        left.addView(tiles, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        ScoreBoard.countUp(distanceValue, run.meters, { getString(R.string.result_meters, it) }, 150)
        ScoreBoard.countUp(scoreValue, run.score, { it.toString() }, 300)

        // How close to this friend's best (filled once the run is saved).
        val status = label(getString(R.string.result_saving), 13f, ScoreBoard.SOFT, bold = true)
        val bar = com.tinyrunners.school.ui.BestBar(this)
        left.addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        left.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(12)).apply { topMargin = dp(4) })

        // What this run brought home: coins, the friend's items and the obstacles cleared.
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        chips.addView(label(getString(R.string.result_rewards), 13f, ScoreBoard.SOFT, bold = true).apply { setPadding(0, 0, dp(8), 0) })
        val coins = com.tinyrunners.school.shop.ShopRules.coinsForRun(run.meters) + intent.getIntExtra(EXTRA_COINS, 0)
        fun reward(icon: android.view.View?, text: String) = chips.addView(ScoreBoard.pill(this, icon, text),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(6) })
        reward(CoinIcon(this), getString(R.string.result_chip_coins, coins))
        com.tinyrunners.school.quest.QuestItems.of(character.id)?.let { item ->
            reward(com.tinyrunners.school.talk.ItemIconView(this, item), getString(R.string.result_chip_items, getString(item.name), intent.getIntExtra(EXTRA_ITEMS, 0)))
        }
        reward(null, getString(R.string.result_chip_cleared, run.cleared))
        left.addView(android.widget.HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })

        // Right: the buttons, one under another.
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        fun add(text: String, primary: Boolean, click: () -> Unit): Button = ScoreBoard.button(this, text, primary, click).also {
            // Six or seven buttons fit on one landscape screen without scrolling.
            right.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(41)).apply { if (right.childCount > 0) topMargin = dp(6) })
        }
        if (storyId != null) {
            // Any result continues the story; there is no "try again to unlock".
            StoryRunSignal.finishedFor = storyId
            add(getString(R.string.result_continue_story), primary = true) { finish() }
        }
        add(getString(R.string.result_again), primary = storyId == null) {
            // Straight back on the road: the story goes on during the run (no talk window in between).
            startActivity(GameActivity.intent(this, character.id).putExtra(GameActivity.EXTRA_STORY_ID, storyId))
            finish()
        }
        // A square picture card of this run (friend, distance, score, the day's weather, their word) for other apps.
        add(getString(R.string.share_button), primary = false) {
            try {
                val card = com.tinyrunners.school.share.ShareCard.runCard(this, character,
                    com.tinyrunners.school.share.ShareCard.Run(run.meters, run.score, said, weatherOf(intent)))
                com.tinyrunners.school.share.ShareCard.share(this, card, "tiny_runners_run.png",
                    getString(R.string.share_run_text, com.tinyrunners.school.talk.Josa.with(character.name, "과", "와"), run.meters))
            } catch (e: Exception) {
                android.util.Log.w("ResultActivity", "Share card failed", e)
                android.widget.Toast.makeText(this, R.string.share_failed, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        add(getString(R.string.result_shop), primary = false) {
            startActivity(com.tinyrunners.school.shop.ShopActivity.intent(this, character.id))
        }
        add(getString(R.string.result_records), primary = false) { startActivity(Intent(this, RecordsActivity::class.java)) }
        // In a story run, leaving goes back to the story instead (the "continue story" button above).
        if (storyId == null) add(getString(R.string.result_choose_friend), primary = false) {
            startActivity(MainActivity.selectionIntent(this))
            finish()
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // Room at the top for the bag button in the corner.
            setPadding(dp(28), dp(52), dp(28), dp(16))
        }
        row.addView(pageWrap, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(right, LinearLayout.LayoutParams(dp(260), LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(24) })
        val frame = FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            // A's separated pack (backgrounds/result): a snowy lake under a violet evening sky, with the day's line on top.
            addView(com.tinyrunners.school.ui.CoverPicture(this@ResultActivity, R.drawable.bg_v2_result_wide, sideInset = 0.1f),
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(label(getString(R.string.result_headline_v2), 20f, Color.rgb(255, 246, 222), bold = true).apply {
                setShadowLayer(8f, 0f, 2f, Color.argb(140, 40, 30, 60)); gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(14) })
            addView(android.widget.ScrollView(this@ResultActivity).apply { isFillViewport = true; addView(row) },
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            setOnApplyWindowInsetsListener { v, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    getChildAt(2).setPadding(bars.left, bars.top, bars.right, bars.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    getChildAt(2).setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                insets
            }
        }
        setContentView(frame)
        BagButton.floatIn(frame)
        frame.addView(com.tinyrunners.school.ui.HomeButton.create(this, getString(R.string.result_home_pill)) {
            com.tinyrunners.school.ui.HomeButton.goToMenu(this); finish()
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START).apply { leftMargin = dp(16); topMargin = dp(12) })

        val store = ProgressStore.forCurrentUser(this)
        // (The "스토리 진행으로 / 아이템 전하기" button left with the talk windows: the story is told during the run.)
        store.record(run) { result ->
            if (isDestroyed) return@record
            result.onSuccess { outcome ->
                // A postcard this run earned shows up at the top.
                com.tinyrunners.school.postcard.PostcardNotice.checkNew(this)
                val p = outcome.progress
                if (outcome.newBest) {
                    status.setText(R.string.result_new_best); status.setTextColor(ScoreBoard.GREEN)
                    stamp.visibility = android.view.View.VISIBLE
                    stamp.scaleX = 1.8f; stamp.scaleY = 1.8f; stamp.alpha = 0f
                    stamp.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(500).setDuration(260).start()
                    bar.set(1f)
                } else {
                    status.text = getString(R.string.result_best_so_far, p.bestMeters, p.bestScore)
                    bar.set(if (p.bestMeters > 0) run.meters.toFloat() / p.bestMeters else 0f)
                }
            }
                .onFailure {
                    status.setTextColor(Page.ERROR)
                    status.text = getString(R.string.result_save_failed)
                }
        }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        /** Items of the friend picked up on this run. */
        const val EXTRA_ITEMS = "items"
        /** Bonus coins picked up on the road in this run. */
        const val EXTRA_COINS = "bonusCoins"
        const val EXTRA_RUN_ID = "runId"
        private const val EXTRA_WEATHER_CITY = "weatherCity"
        private const val EXTRA_WEATHER_SKY = "weatherSky"
        private const val EXTRA_WEATHER_TEMP = "weatherTemp"
        private const val EXTRA_WEATHER_DAY = "weatherDay"
        private const val EXTRA_WEATHER_CHOSEN = "weatherChosen"

        /** Passes the run's weather on (for the share card). */
        fun putWeather(intent: Intent, w: com.tinyrunners.school.weather.WeatherReport): Intent = intent
            .putExtra(EXTRA_WEATHER_CITY, w.city.name).putExtra(EXTRA_WEATHER_SKY, w.sky.name)
            .putExtra(EXTRA_WEATHER_TEMP, w.temperatureC).putExtra(EXTRA_WEATHER_DAY, w.isDay)
            .putExtra(EXTRA_WEATHER_CHOSEN, w.chosen)

        /** The run's weather, or null when the run had none (weather off, or nothing known yet). */
        fun weatherOf(intent: Intent): com.tinyrunners.school.weather.WeatherReport? {
            val city = intent.getStringExtra(EXTRA_WEATHER_CITY)?.let { n -> com.tinyrunners.school.weather.City.entries.firstOrNull { it.name == n } } ?: return null
            val sky = intent.getStringExtra(EXTRA_WEATHER_SKY)?.let { n -> com.tinyrunners.school.weather.Sky.entries.firstOrNull { it.name == n } } ?: return null
            return com.tinyrunners.school.weather.WeatherReport(city, sky, intent.getDoubleExtra(EXTRA_WEATHER_TEMP, Double.NaN),
                intent.getBooleanExtra(EXTRA_WEATHER_DAY, true), intent.getBooleanExtra(EXTRA_WEATHER_CHOSEN, false))
        }
        const val EXTRA_METERS = "meters"
        const val EXTRA_SCORE = "score"
        const val EXTRA_CLEARED = "cleared"
        /** Where the run ended (world units), so the background shows that spot of the friend's road. */
        const val EXTRA_DISTANCE = "distance"

        fun intent(context: Context, run: RunResult) = Intent(context, ResultActivity::class.java)
            .putExtra(EXTRA_RUN_ID, run.runId)
            .putExtra(GameActivity.EXTRA_CHARACTER_ID, run.characterId)
            .putExtra(EXTRA_METERS, run.meters)
            .putExtra(EXTRA_SCORE, run.score)
            .putExtra(EXTRA_CLEARED, run.cleared)
    }
}

/**
 * The friend's own running scenery at the spot where the run ended (same framing as the run and the talk scenes),
 * or the usual hills when the friend has no background yet.
 */
@android.annotation.SuppressLint("ViewConstructor")
internal class ResultScenery(context: Context, characterId: String, private val distance: Float) : android.view.View(context) {
    private val scenery = com.tinyrunners.school.game.StoryBackground.load(context, characterId)
    private val fallback = com.tinyrunners.school.ui.ScenePainter()

    override fun onDraw(canvas: android.graphics.Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val scale = minOf(h / 360f, w / 640f)
        val groundY = h - 48f * scale
        if (scenery != null) scenery.draw(canvas, w, h, groundY, distance, scale)
        else fallback.draw(canvas, width, height, groundY, h * 0.17f)
    }
}

/** A still gold coin. */
private class CoinIcon(context: Context) : android.view.View(context) {
    override fun onDraw(canvas: android.graphics.Canvas) {
        val r = minOf(width, height) / 2f
        com.tinyrunners.school.game.CoinArt.draw(canvas, width / 2f, height / 2f, r * 0.9f, spin = 0f)
    }
}
