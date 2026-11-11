package com.tinyrunners.school.result

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
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
import com.tinyrunners.school.ui.SceneryView
import kotlin.math.roundToInt

/**
 * Shows one finished run and saves it: the distance and score on the left, what to do next on the right, over the
 * usual hills. Saving uses the run's ID, so recreating this screen (rotation, returning from the background) never
 * counts the same run twice.
 */
class ResultActivity : Activity() {
    override fun onPause() {
        com.tinyrunners.school.audio.Music.pauseSoon()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        com.tinyrunners.school.audio.Music.playGame(this)
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

        // Left: what this run was. Only the distance and the score.
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        left.addView(label(getString(R.string.result_eyebrow), 15f, Page.LEAF, bold = true))
        left.addView(label(character.name, 30f, Page.INK, bold = true))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(22), dp(18), dp(22), dp(18))
            background = GradientDrawable().apply { setColor(Page.PAPER); cornerRadius = dp(22).toFloat(); setStroke(dp(1), Page.LINE) }
        }
        card.addView(stat(getString(R.string.result_distance_label), getString(R.string.result_meters, run.meters)),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(android.view.View(this).apply { setBackgroundColor(Page.LINE) },
            LinearLayout.LayoutParams(dp(1), LinearLayout.LayoutParams.MATCH_PARENT).apply { marginStart = dp(12); marginEnd = dp(12) })
        card.addView(stat(getString(R.string.result_score_label), run.score.toString()),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        left.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        // Only shown when there is news (a new best) or a problem (not saved).
        val status = label("", 15f, Page.LEAF, bold = true).apply { setPadding(dp(4), dp(10), 0, 0) }
        left.addView(status)

        // Right: the buttons, one under another.
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        fun add(text: String, primary: Boolean, click: () -> Unit): Button = button(text, primary, click).also {
            right.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { if (right.childCount > 0) topMargin = dp(8) })
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
            setPadding(dp(40), dp(60), dp(28), dp(16))
        }
        row.addView(left, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(right, LinearLayout.LayoutParams(dp(290), LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(36) })
        val frame = FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            addView(SceneryView(this@ResultActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
                        style(toStory, primary = true)
                        style(again, primary = false)
                    }
                }
            }
        }
        store.record(run) { result ->
            if (isDestroyed) return@record
            result.onSuccess { outcome -> if (outcome.newBest) status.setText(R.string.result_new_best) }
                .onFailure {
                    status.setTextColor(Page.ERROR)
                    status.text = getString(R.string.result_save_failed)
                }
        }
    }

    private fun stat(name: String, value: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(label(name, 15f, Page.MUTED))
        addView(label(value, 40f, Page.INK, bold = true))
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun button(text: String, primary: Boolean, click: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 16f; isAllCaps = false; stateListAnimator = null
        minHeight = 0; minimumHeight = 0; setPadding(dp(12), 0, dp(12), 0)
        style(this, primary)
        setOnClickListener { click() }
    }

    private fun style(button: Button, primary: Boolean) {
        button.setTextColor(if (primary) Color.WHITE else Page.LEAF)
        button.typeface = if (primary) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        button.background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
            setColor(if (primary) Page.LEAF else Page.PAPER); cornerRadius = dp(16).toFloat()
            setStroke(dp(1), if (primary) Page.LEAF else Page.LINE)
        }, null)
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

        fun intent(context: Context, run: RunResult) = Intent(context, ResultActivity::class.java)
            .putExtra(EXTRA_RUN_ID, run.runId)
            .putExtra(GameActivity.EXTRA_CHARACTER_ID, run.characterId)
            .putExtra(EXTRA_METERS, run.meters)
            .putExtra(EXTRA_SCORE, run.score)
            .putExtra(EXTRA_CLEARED, run.cleared)
    }
}
