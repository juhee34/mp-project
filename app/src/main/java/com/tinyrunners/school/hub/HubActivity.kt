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
import android.widget.TextView
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.ui.MockupFrame
import com.tinyrunners.school.talk.PrologueActivity
import kotlin.math.roundToInt

/**
 * First screen after login (the menu), in A's separated design pack: 라일's home under a hanging sign with the title,
 * the tagline, four menu buttons and the six friends in a row on the grass.
 * 스토리 진행하기 → friend selection (the story is told while running). 상점 → the shop.
 * 튜토리얼 → the game guide on a practice run. 설정 → settings.
 */
class HubActivity : Activity() {
    /** The login popup closes right where a button appears; a tap carried over from it must not open anything. */
    private val shownAt = SystemClock.uptimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The same scene as the opening (TitleScene): the logo, the tagline pill, the four menu buttons in the middle and
        // the six friends on the ground; the sunny meadow by day, the lavender village by night.
        fun add(label: Int, click: () -> Unit) = com.tinyrunners.school.ui.TitleScene.button(this, getString(label),
            primary = false, width = 0.8f, heightShare = 0.095f) { if (SystemClock.uptimeMillis() - shownAt >= TAP_GUARD_MS) click() }
        val buttons = listOf(
            // 스토리 진행하기 is the game now: choose a friend, then the story is told while running.
            add(R.string.hub_story) { startActivity(MainActivity.selectionIntent(this)) },
            add(R.string.hub_shop) { startActivity(com.tinyrunners.school.shop.ShopActivity.intent(this, null)) },
            // A practice run with 라일 that always starts with the game guide.
            add(R.string.hub_tutorial) { startActivity(GameActivity.intent(this, "mint").putExtra(GameActivity.EXTRA_SHOW_GUIDE, true)) },
            add(R.string.hub_settings) { startActivity(android.content.Intent(this, com.tinyrunners.school.settings.SettingsActivity::class.java)) },
        )
        val frame = com.tinyrunners.school.ui.TitleScene.screen(this, getString(R.string.hub_tagline_v2), buttons, gap = 0.014f, firstGap = 0.03f)
        setContentView(frame)
        hideSystemBars()
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
        private const val TAP_GUARD_MS = 600L

        /** Opens the hub as the only screen in the task (after login it replaces the opening and popups). */
        fun intent(context: Context) = Intent(context, HubActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
