package com.tinyrunners.school.quest

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
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.shop.ShopStore
import com.tinyrunners.school.story.Story
import com.tinyrunners.school.story.StoryListActivity
import com.tinyrunners.school.story.StoryRepository
import com.tinyrunners.school.talk.ItemIconView
import com.tinyrunners.school.talk.TalkActivity
import com.tinyrunners.school.ui.Page
import com.tinyrunners.school.ui.SceneryView
import kotlin.math.roundToInt

/**
 * 가방: what the account has gathered. Coins (from distance run, spent in the shop) and each friend's items picked
 * up on the road. A friend whose next village story can take the items gets a "전하러 가기" button.
 */
class BagActivity : Activity() {
    private val friends by lazy { CharacterRepository(this).characters }
    private lateinit var coins: TextView
    private lateinit var grid: LinearLayout
    private lateinit var message: TextView
    private var bag: ItemBag? = null
    private var read: Set<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(12)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(com.tinyrunners.school.ui.HomeButton.create(this))
        top.addView(TextView(this).apply {
            setText(R.string.bag_title); textSize = 24f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            setPadding(dp(12), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        coins = TextView(this).apply {
            setText(R.string.shop_loading)
            textSize = 18f; setTextColor(COIN_INK); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(14), dp(8), dp(16), dp(8))
            background = GradientDrawable().apply { setColor(COIN_BG); cornerRadius = dp(20).toFloat(); setStroke(dp(2), COIN) }
            compoundDrawablePadding = dp(8)
            setCompoundDrawablesRelative(GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(COIN); setStroke(dp(2), COIN_INK); setBounds(0, 0, dp(18), dp(18))
            }, null, null, null)
            // Coins are spent in the shop.
            setOnClickListener { startActivity(com.tinyrunners.school.shop.ShopActivity.intent(this@BagActivity, null)) }
        }
        top.addView(coins)
        content.addView(top)
        message = TextView(this).apply { setText(R.string.bag_body); textSize = 15f; setTextColor(Page.MUTED); setPadding(0, dp(6), 0, 0) }
        content.addView(message)
        grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            addView(SceneryView(this@BagActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(content, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            setOnApplyWindowInsetsListener { v, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                insets
            }
        })
    }

    override fun onResume() {
        super.onResume()
        com.tinyrunners.school.audio.Music.playGame(this)
        render()
        // Everything is re-read when the screen comes back: runs, purchases and hand-overs change it.
        ShopStore.forCurrentUser(this).load { result ->
            if (isDestroyed) return@load
            result.onSuccess { coins.text = getString(R.string.shop_coins, it.coins) }.onFailure { coins.text = "—" }
        }
        ItemBagStore.forCurrentUser(this).load { result ->
            if (isDestroyed) return@load
            result.onSuccess { bag = it; render() }.onFailure { message.setTextColor(Page.ERROR); message.setText(R.string.quest_error) }
        }
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            render()
        }
    }

    override fun onPause() {
        com.tinyrunners.school.audio.Music.pauseSoon()
        super.onPause()
    }

    /** Six item cards, three to a row. */
    private fun render() {
        grid.removeAllViews()
        val stories = StoryRepository.load(this)
        QuestItems.all.chunked(3).forEachIndexed { r, rowItems ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowItems.forEachIndexed { i, item ->
                row.addView(card(item, stories), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (i > 0) marginStart = dp(10)
                })
            }
            grid.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { if (r > 0) topMargin = dp(10) })
        }
    }

    private fun card(item: QuestItem, stories: List<Story>): View {
        val friend = friends.firstOrNull { it.id == item.friendId }
        val have = bag?.count(item.friendId)
        val next = read?.let { Story.nextUnread(stories, item.friendId, it) }
        val ready = have != null && have >= QuestRules.NEEDED && next != null
        val tint = try { Color.parseColor(friend?.color) } catch (_: Exception) { Page.LEAF }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply { setColor(Page.PAPER); cornerRadius = dp(18).toFloat(); setStroke(dp(if (ready) 3 else 1), if (ready) tint else Page.LINE) }
        }
        card.addView(ItemIconView(this, item), LinearLayout.LayoutParams(dp(52), dp(52)))
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        column.addView(TextView(this).apply {
            text = getString(R.string.bag_item_title, getString(item.name), have?.toString() ?: "…")
            textSize = 17f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD); maxLines = 1
        })
        column.addView(TextView(this).apply {
            text = getString(R.string.bag_item_owner, friend?.name.orEmpty())
            textSize = 13f; setTextColor(Page.MUTED); maxLines = 1
        })
        column.addView(TextView(this).apply {
            text = when {
                have == null || read == null -> ""
                next == null -> getString(R.string.bag_all_told)
                ready -> getString(R.string.bag_ready, next.title)
                else -> getString(R.string.bag_need, QuestRules.NEEDED - have)
            }
            textSize = 13f; setTextColor(if (ready) Page.LEAF else Page.MUTED); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            if (ready) setTypeface(typeface, Typeface.BOLD)
        })
        if (ready && next != null) column.addView(Button(this).apply {
            setText(R.string.bag_give)
            textSize = 14f; isAllCaps = false; stateListAnimator = null
            minHeight = dp(36); minimumHeight = dp(36); setPadding(dp(12), 0, dp(12), 0)
            setTextColor(Color.WHITE)
            background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply { setColor(Page.LEAF); cornerRadius = dp(14).toFloat() }, null)
            setOnClickListener {
                // The village list (on that friend) stays underneath, so the story ends there.
                startActivities(arrayOf(StoryListActivity.intent(this@BagActivity, item.friendId),
                    TalkActivity.village(this@BagActivity, item.friendId, next.id, askItems = true)))
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36)).apply { topMargin = dp(4) })
        card.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return card
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        private val COIN = Color.rgb(246, 200, 80)
        private val COIN_BG = Color.rgb(255, 247, 220)
        private val COIN_INK = Color.rgb(140, 96, 20)
        fun intent(context: Context) = Intent(context, BagActivity::class.java)
    }
}
