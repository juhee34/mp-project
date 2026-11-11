package com.tinyrunners.school.hub

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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.talk.PrologueActivity
import com.tinyrunners.school.ui.Page
import com.tinyrunners.school.ui.SceneryView
import kotlin.math.roundToInt

/**
 * First screen after login: three big choices side by side on the opening's sky and hills.
 * 스토리 진행하기 → world story (black screen) → talks with the village residents.
 * 게임하기 → friend selection. 튜토리얼 하기 → the game guide on a practice run.
 */
class HubActivity : Activity() {
    /** The login popup closes right where a card appears; a tap carried over from it must not open anything. */
    private val shownAt = android.os.SystemClock.uptimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(16), dp(24), dp(24))
        }
        content.addView(TextView(this).apply {
            setText(R.string.hub_title)
            textSize = 34f; setTextColor(Page.INK); setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        })
        content.addView(TextView(this).apply {
            setText(R.string.hub_subtitle)
            textSize = 16f; setTextColor(Page.MUTED); gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        row.addView(choice(R.string.hub_story, R.string.hub_story_body, STORY, primary = false) {
            startActivity(PrologueActivity.forStory(this))
        }, weighted(first = true))
        row.addView(choice(R.string.hub_game, R.string.hub_game_body, Page.LEAF, primary = true) {
            startActivity(MainActivity.selectionIntent(this))
        }, weighted())
        row.addView(choice(R.string.hub_tutorial, R.string.hub_tutorial_body, TUTORIAL, primary = false) {
            // A practice run with 라일 that always starts with the game guide.
            startActivity(GameActivity.intent(this, "mint").putExtra(GameActivity.EXTRA_SHOW_GUIDE, true))
        }, weighted())
        content.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(18) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Page.BACKGROUND)
            addView(SceneryView(this@HubActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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

    /** A big rounded card button: a title and one line saying where it goes. */
    private fun choice(title: Int, body: Int, accent: Int, primary: Boolean, click: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(18), dp(16), dp(18))
        isClickable = true
        isFocusable = true
        contentDescription = getString(title) + ". " + getString(body)
        background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
            setColor(if (primary) accent else Page.PAPER)
            cornerRadius = dp(28).toFloat()
            setStroke(dp(3), accent)
        }, null)
        setOnClickListener { if (android.os.SystemClock.uptimeMillis() - shownAt >= TAP_GUARD_MS) click() }
        addView(TextView(context).apply {
            setText(title)
            textSize = 24f; setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER
            setTextColor(if (primary) Color.WHITE else accent)
        })
        addView(TextView(context).apply {
            setText(body)
            textSize = 15f; gravity = Gravity.CENTER
            setTextColor(if (primary) Color.argb(230, 255, 255, 255) else Page.MUTED)
            setPadding(0, dp(8), 0, 0)
        })
    }

    private fun weighted(first: Boolean = false) =
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
            if (!first) marginStart = dp(16)
            topMargin = dp(8); bottomMargin = dp(8)
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        private val STORY = Color.rgb(140, 96, 150)
        private val TUTORIAL = Color.rgb(176, 112, 64)
        private const val TAP_GUARD_MS = 600L

        /** Opens the hub as the only screen in the task (after login it replaces the opening and popups). */
        fun intent(context: Context) = Intent(context, HubActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
