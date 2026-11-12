package com.tinyrunners.school.records

import android.app.Activity
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.CharacterProgress
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.story.SceneThumb
import com.tinyrunners.school.ui.CardShelf
import com.tinyrunners.school.ui.HomeButton
import com.tinyrunners.school.ui.Page
import com.tinyrunners.school.ui.PaperBackdropView
import kotlin.math.roundToInt

/**
 * Per-friend records for the current account, laid out like the shop: one small card per friend in a row that slides
 * sideways (CardShelf), with runs, total distance, best distance and best score. Friends never run with still
 * appear (their picture a little darker), without any penalty.
 */
class RecordsActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var shelf: CardShelf

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val characters = CharacterRepository(this).characters
        val store = ProgressStore.forCurrentUser(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(6), dp(16), dp(10)) }

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(HomeButton.create(this, getString(R.string.common_back_pill)) { finish() })
        top.addView(TextView(this).apply {
            setText(R.string.records_title); textSize = 22f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            setPadding(dp(12), 0, dp(12), 0)
        })
        status = TextView(this).apply { setText(R.string.records_loading); textSize = 14f; setTextColor(Page.LEAF); setTypeface(typeface, Typeface.BOLD) }
        top.addView(status, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(com.tinyrunners.school.ui.BagButton.create(this))
        content.addView(top)
        content.addView(TextView(this).apply {
            setText(if (store.location == ProgressStore.Location.CLOUD) R.string.records_where_cloud else R.string.records_where_device)
            textSize = 14f; setTextColor(Page.MUTED); setPadding(dp(10), dp(3), dp(10), dp(3))
            // A soft paper strip so the note stays readable over the tree in the picture.
            background = android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.argb(215, 255, 249, 235)); cornerRadius = dp(12).toFloat() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        shelf = CardShelf(this)
        content.addView(shelf, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            addView(PaperBackdropView(this@RecordsActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
        shelf.setCards(characters.map { card(it, null, loading = true) })

        store.loadAll { result ->
            if (isDestroyed) return@loadAll
            result.onSuccess { all ->
                status.text = getString(R.string.records_total, all.values.sumOf { it.runs }, all.values.sumOf { it.totalMeters })
                shelf.setCards(characters.map { card(it, all[it.id], loading = false) })
            }.onFailure {
                status.setTextColor(Page.ERROR)
                status.text = getString(R.string.records_load_failed)
            }
        }
    }

    private fun card(friend: CharacterDefinition, p: CharacterProgress?, loading: Boolean): View {
        val ran = p != null && p.runs > 0
        val card = CardShelf.card(this)
        card.addView(SceneThumb(this, friend.id, locked = !loading && !ran).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        val nameRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(CardShelf.title(this, friend.name), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (ran) nameRow.addView(CardShelf.chip(this, getString(R.string.records_chip_runs, p!!.runs)))
        card.addView(nameRow)
        when {
            loading -> card.addView(CardShelf.body(this, getString(R.string.records_loading), lines = 2))
            !ran -> card.addView(CardShelf.body(this, getString(R.string.records_none), lines = 3))
            else -> {
                card.addView(stat(R.string.records_stat_total, getString(R.string.records_meters, p!!.totalMeters)))
                card.addView(stat(R.string.records_stat_best, getString(R.string.records_meters, p.bestMeters)))
                card.addView(stat(R.string.records_stat_score, p.bestScore.toString()))
            }
        }
        card.contentDescription = if (ran) getString(R.string.records_card_description, friend.name, p!!.runs, p.totalMeters, p.bestMeters, p.bestScore)
            else friend.name + ". " + getString(R.string.records_none)
        return card
    }

    /** One "label ........ value" line of a record card. */
    private fun stat(label: Int, value: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; setPadding(dp(4), dp(3), dp(4), 0)
        addView(TextView(context).apply { setText(label); textSize = 13f; setTextColor(CardShelf.INK_SOFT) },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(TextView(context).apply { text = value; textSize = 15f; setTextColor(CardShelf.GREEN); setTypeface(typeface, Typeface.BOLD) })
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}
