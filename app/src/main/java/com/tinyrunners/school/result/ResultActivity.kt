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
        intent.getIntExtra(EXTRA_AD_BONUS, 0).takeIf { it > 0 }?.let { reward(CoinIcon(this), getString(R.string.result_chip_ad, it)) }
        reward(null, getString(R.string.result_chip_cleared, run.cleared))
        left.addView(android.widget.HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })

        // Right: the buttons, one under another.
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        fun add(text: String, primary: Boolean, click: () -> Unit): Button = ScoreBoard.button(this, text, primary, click).also {
            right.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { if (right.childCount > 0) topMargin = dp(8) })
        }
        if (storyId != null) {
            // Any result continues the story; there is no "try again to unlock".
            StoryRunSignal.finishedFor = storyId
            add(getString(R.string.result_continue_story), primary = true) { finish() }
        }
        val again = add(getString(R.string.result_again), primary = storyId == null) {
            // A short "again!" line from the friend, then the run.
            startActivity(com.tinyrunners.school.talk.TalkActivity.retry(this, character.id,
                GameActivity.intent(this, character.id).putExtra(GameActivity.EXTRA_STORY_ID, storyId)))
            finish()
        }
        // Back to the village: straight to the friend's next story when enough items are in the bag.
        val toStory = add(getString(R.string.result_to_village), primary = false) { openVillage(character.id, null) }
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
            addView(ResultScenery(this@ResultActivity, character.id, intent.getFloatExtra(EXTRA_DISTANCE, 0f)),
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(android.widget.ScrollView(this@ResultActivity).apply { isFillViewport = true; addView(row) },
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            setOnApplyWindowInsetsListener { v, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    getChildAt(1).setPadding(bars.left, bars.top, bars.right, bars.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    getChildAt(1).setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                insets
            }
        }
        setContentView(frame)
        BagButton.floatIn(frame)

        val store = ProgressStore.forCurrentUser(this)
        val item = com.tinyrunners.school.quest.QuestItems.of(character.id)
        if (item != null && storyId == null) {
            com.tinyrunners.school.quest.ItemBagStore.forCurrentUser(this).load { bag ->
                if (isDestroyed) return@load
                val have = bag.getOrNull()?.count(character.id) ?: return@load
                store.loadAll { all ->
                    if (isDestroyed) return@loadAll
                    val read = all.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
                    val next = com.tinyrunners.school.story.Story.nextUnread(
                        com.tinyrunners.school.story.StoryRepository.load(this), character.id, read) ?: return@loadAll
                    if (have >= com.tinyrunners.school.quest.QuestRules.NEEDED) {
                        toStory.text = getString(R.string.result_give_items, character.name, getString(item.name))
                        toStory.setOnClickListener { openVillage(character.id, next.id) }
                        ScoreBoard.restyle(toStory, primary = true)
                        ScoreBoard.restyle(again, primary = false)
                    }
                }
            }
        }
        store.record(run) { result ->
            if (isDestroyed) return@record
            result.onSuccess { outcome ->
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

    /**
     * Opens the village list on this friend's page and, with [storyId], that story's talk on top of it, so the
     * list is what is left when the story ends.
     */
    private fun openVillage(characterId: String, storyId: String?) {
        val list = com.tinyrunners.school.story.StoryListActivity.intent(this, characterId)
        if (storyId == null) startActivity(list)
        else startActivities(arrayOf(list, com.tinyrunners.school.talk.TalkActivity.village(this, characterId, storyId, askItems = true)))
        finish()
    }

    companion object {
        /** Items of the friend picked up on this run. */
        const val EXTRA_ITEMS = "items"
        /** Bonus coins picked up on the road in this run. */
        const val EXTRA_COINS = "bonusCoins"
        const val EXTRA_RUN_ID = "runId"
        const val EXTRA_METERS = "meters"
        const val EXTRA_SCORE = "score"
        const val EXTRA_CLEARED = "cleared"
        /** Where the run ended (world units), so the background shows that spot of the friend's road. */
        const val EXTRA_DISTANCE = "distance"
        /** Coins given for watching the optional ad after the crash. */
        const val EXTRA_AD_BONUS = "adBonus"

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
