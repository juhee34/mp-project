package com.tinyrunners.school.postcard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.CharacterProgress
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.shop.ShopActivity
import com.tinyrunners.school.shop.ShopBackground
import com.tinyrunners.school.story.StoryRepository
import com.tinyrunners.school.ui.HomeButton
import kotlin.math.roundToInt

/**
 * 엽서첩 (guide ⑤, opened from the bag): one column per friend with their postcards. A received card shows its
 * picture and opens to its back (the friend's words); a card not received yet shows how to get it and how far along
 * it is. Not having a card never costs anything.
 */
class PostcardActivity : Activity() {
    private val friends by lazy { CharacterRepository(this).characters }
    private val cards by lazy { PostcardRepository.all(this) }
    private val stories by lazy { StoryRepository.load(this).groupBy({ it.characterId }, { it.id }) }
    private var progress: Map<String, CharacterProgress>? = null
    private lateinit var grid: LinearLayout
    private lateinit var counter: TextView
    private lateinit var frame: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(26), dp(8), dp(26), dp(40)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(HomeButton.create(this, getString(R.string.common_back_pill)) { finish() })
        top.addView(View(this).apply { setBackgroundColor(ShopActivity.HONEY) },
            LinearLayout.LayoutParams(dp(2), dp(40)).apply { marginStart = dp(14); marginEnd = dp(14) })
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(TextView(this).apply {
            setText(R.string.postcard_title); textSize = 26f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD); includeFontPadding = false
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        })
        titles.addView(TextView(this).apply { setText(R.string.postcard_subtitle); textSize = 14f; setTextColor(SOFT) })
        top.addView(titles, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        counter = TextView(this).apply {
            textSize = 17f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD); setPadding(dp(18), dp(6), dp(18), dp(6))
            background = GradientDrawable().apply { setColor(ShopActivity.CARD); cornerRadius = dp(20).toFloat(); setStroke(dp(2), ShopActivity.HONEY) }
        }
        top.addView(counter)
        content.addView(top)
        grid = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        content.addView(grid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(10) })

        frame = FrameLayout(this).apply {
            setBackgroundColor(ShopActivity.FABRIC)
            addView(ShopBackground(this@PostcardActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
        }
        setContentView(frame)
        render()
    }

    override fun onResume() {
        super.onResume()
        ProgressStore.forCurrentUser(this).loadAll { result ->
            if (isDestroyed) return@loadAll
            progress = result.getOrNull().orEmpty()
            // Everything shown here counts as announced.
            PostcardSeen(this).ids = PostcardSeen(this).ids + PostcardRules.earnedIds(cards, progress.orEmpty(), stories)
            render()
        }
    }

    private fun render() {
        grid.removeAllViews()
        val p = progress
        val earned = if (p == null) emptySet() else PostcardRules.earnedIds(cards, p, stories)
        counter.text = if (p == null) getString(R.string.records_loading) else getString(R.string.postcard_count, earned.size, cards.size)
        friends.forEachIndexed { i, friend ->
            val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            column.addView(nameRow(friend))
            cards.filter { it.characterId == friend.id }.forEach { card ->
                val slot = if (card.id in earned) received(card, friend) else waiting(card, p?.get(friend.id))
                column.addView(slot, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) })
            }
            grid.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { if (i > 0) marginStart = dp(12) })
        }
    }

    private fun nameRow(friend: CharacterDefinition) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        CharacterSprites.load(resources, friend.id)?.let { s ->
            addView(ImageView(context).apply {
                setImageBitmap(s[SpriteFrame.IDLE_0]); scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                (drawable as? android.graphics.drawable.BitmapDrawable)?.paint?.isFilterBitmap = false
            }, LinearLayout.LayoutParams(dp(30), dp(30)))
        }
        addView(TextView(context).apply { text = friend.name; textSize = 16f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD); setPadding(dp(4), 0, 0, 0) })
    }

    private fun received(card: Postcard, friend: CharacterDefinition): View = PostcardArt(this, card, colorOf(friend)).apply {
        contentDescription = getString(R.string.postcard_received_description, friend.name, card.title)
        isClickable = true
        setOnClickListener { openBack(card, friend) }
    }

    /** A card not received yet: a dashed space with how to get it and how far along it is. */
    private fun waiting(card: Postcard, p: CharacterProgress?): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        setPadding(dp(8), dp(6), dp(8), dp(6))
        background = GradientDrawable().apply {
            setColor(Color.argb(150, 255, 250, 236)); cornerRadius = dp(12).toFloat()
            setStroke(dp(2), ShopActivity.HONEY, dp(6).toFloat(), dp(4).toFloat())
        }
        val (have, need) = PostcardRules.progress(card, p, stories[card.characterId].orEmpty())
        addView(TextView(context).apply {
            text = howTo(card); textSize = 12f; setTextColor(INK); gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD); maxLines = 3
            // Korean lines break between words.
            if (Build.VERSION.SDK_INT >= 33) lineBreakWordStyle = android.graphics.text.LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE
        })
        addView(TextView(context).apply {
            text = when (card.condition.type) {
                ConditionType.METERS -> getString(R.string.postcard_progress_meters, have.coerceAtMost(need), need)
                ConditionType.STORIES -> getString(R.string.postcard_progress_count, have.coerceAtMost(need), need)
                ConditionType.WEATHER -> getString(R.string.postcard_progress_times, have.coerceAtMost(need), need)
            }
            textSize = 12f; setTextColor(SOFT); gravity = Gravity.CENTER; setPadding(0, dp(2), 0, 0)
        })
    }

    private fun howTo(card: Postcard): String = when (card.condition.type) {
        ConditionType.METERS -> getString(R.string.postcard_how_meters, card.condition.value)
        ConditionType.STORIES -> getString(R.string.postcard_how_stories)
        ConditionType.WEATHER -> getString(R.string.postcard_how_weather)
    }

    /** The card turned over: its picture beside the friend's words, over a soft dim. Tap anywhere to close. */
    private fun openBack(card: Postcard, friend: CharacterDefinition) {
        val dim = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(150, 30, 36, 30)); isClickable = true
        }
        val pair = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        pair.addView(PostcardArt(this, card, colorOf(friend)), LinearLayout.LayoutParams(dp(330), dp(220)))
        val back = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(18), dp(22), dp(18))
            background = GradientDrawable().apply { setColor(Color.parseColor("#FFF8E7")); cornerRadius = dp(14).toFloat(); setStroke(dp(2), ShopActivity.HONEY) }
        }
        back.addView(TextView(this).apply { text = getString(R.string.postcard_from, friend.name); textSize = 14f; setTextColor(SOFT); setTypeface(typeface, Typeface.BOLD) })
        back.addView(TextView(this).apply { text = card.back; textSize = 18f; setTextColor(INK); setLineSpacing(0f, 1.25f); setPadding(0, dp(10), 0, dp(10)) })
        back.addView(TextView(this).apply { text = getString(R.string.postcard_got_by, howTo(card)); textSize = 12f; setTextColor(SOFT) })
        pair.addView(back, LinearLayout.LayoutParams(dp(330), dp(220)).apply { marginStart = dp(16) })
        dim.addView(pair, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        dim.setOnClickListener { frame.removeView(dim) }
        dim.contentDescription = getString(R.string.postcard_close)
        frame.addView(dim, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun colorOf(friend: CharacterDefinition) = runCatching { Color.parseColor(friend.color) }.getOrDefault(GREEN)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        private val GREEN = Color.parseColor("#5E4594")
        private val INK = Color.parseColor("#3D3550")
        private val SOFT = Color.parseColor("#7A6E5E")

        fun intent(context: Context) = Intent(context, PostcardActivity::class.java)
    }
}
