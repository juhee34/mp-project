package com.tinyrunners.school.hub

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.opening.OpeningBackground
import com.tinyrunners.school.talk.PrologueActivity
import kotlin.math.roundToInt

/**
 * First screen after login. Same look as the opening (title, sky, hills, clouds and the six friends on the ground)
 * with five buttons stacked in the middle: white with green text. The user calls this screen the menu.
 * 스토리 진행하기 → world story (black screen) → talks with the village residents.
 * 게임하기 → friend selection. 꾸미기 상점 → the shop (trails and music). 튜토리얼 → the game guide on a practice run.
 * 설정 → settings.
 */
class HubActivity : Activity() {
    /** The login popup closes right where a button appears; a tap carried over from it must not open anything. */
    private val shownAt = SystemClock.uptimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            // Narrow enough to fit in the gap between 포포 and 루미 on the ground.
            translationY = dp(10).toFloat()
        }
        buttons.addView(button(R.string.hub_story) { startActivity(PrologueActivity.forStory(this)) }, slot(first = true))
        buttons.addView(button(R.string.hub_game) { startActivity(MainActivity.selectionIntent(this)) }, slot())
        buttons.addView(button(R.string.hub_shop) { startActivity(com.tinyrunners.school.shop.ShopActivity.intent(this, null)) }, slot())
        buttons.addView(button(R.string.hub_tutorial) {
            // A practice run with 라일 that always starts with the game guide.
            startActivity(GameActivity.intent(this, "mint").putExtra(GameActivity.EXTRA_SHOW_GUIDE, true))
        }, slot())
        buttons.addView(button(R.string.hub_settings) {
            startActivity(android.content.Intent(this, com.tinyrunners.school.settings.SettingsActivity::class.java))
        }, slot())

        setContentView(FrameLayout(this).apply {
            addView(OpeningBackground(this@HubActivity, titleY = 0.13f), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(buttons, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        })
        hideSystemBars()
    }

    /** White pill with a green border and bold green text (all four buttons the same). */
    private fun button(label: Int, click: () -> Unit) = Button(this).apply {
        setText(label)
        textSize = 17f
        isAllCaps = false
        setTextColor(LEAF)
        typeface = Typeface.DEFAULT_BOLD
        minHeight = dp(40); minimumHeight = dp(40)
        setPadding(dp(16), 0, dp(16), 0)
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(24).toFloat()
            setStroke(dp(3), LEAF)
        }
        stateListAnimator = null
        setOnClickListener { if (SystemClock.uptimeMillis() - shownAt >= TAP_GUARD_MS) click() }
    }

    private fun slot(first: Boolean = false) = LinearLayout.LayoutParams(dp(200), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
        if (!first) topMargin = dp(7)
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        private val LEAF = Color.rgb(48, 83, 65)
        private const val TAP_GUARD_MS = 600L

        /** Opens the hub as the only screen in the task (after login it replaces the opening and popups). */
        fun intent(context: Context) = Intent(context, HubActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
