package com.tinyrunners.school.talk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import kotlin.math.roundToInt

/**
 * The world's story right after login: white text on black, one short page per tap, then the friend selection.
 * "건너뛰기" jumps straight to the selection.
 */
class PrologueActivity : Activity() {
    private lateinit var pages: List<String>
    private lateinit var text: TextView
    private lateinit var hint: TextView
    private var page = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pages = TalkRepository.load(this)?.prologue.orEmpty()
        if (pages.isEmpty()) { openSelection(); return }
        page = savedInstanceState?.getInt(KEY_PAGE)?.coerceIn(0, pages.lastIndex) ?: 0

        text = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.45f)
            val pad = dp(48)
            setPadding(pad, pad, pad, pad)
        }
        hint = TextView(this).apply {
            setText(R.string.talk_tap_hint)
            setTextColor(Color.rgb(150, 150, 150))
            textSize = 14f
        }
        val skip = TextView(this).apply {
            setText(R.string.talk_skip)
            setTextColor(Color.rgb(200, 200, 200))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            minHeight = dp(48)
            gravity = Gravity.CENTER
            setPadding(dp(20), 0, dp(20), 0)
            setOnClickListener { openSelection() }
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(text, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(skip, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END).apply { setMargins(0, dp(12), dp(12), 0) })
            addView(hint, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(24) })
            setOnClickListener { next() }
        })
        showPage(animate = true)
    }

    private fun next() {
        if (page >= pages.lastIndex) { openSelection(); return }
        page++
        showPage(animate = true)
    }

    private fun showPage(animate: Boolean) {
        text.text = pages[page]
        text.contentDescription = pages[page]
        text.announceForAccessibility(pages[page])
        // The last page asks "who will you go with?", so the hint changes to say the selection comes next.
        hint.setText(if (page == pages.lastIndex) R.string.talk_prologue_choose else R.string.talk_tap_hint)
        if (animate) {
            text.alpha = 0f
            text.animate().alpha(1f).setDuration(FADE_MS).start()
        }
    }

    private fun openSelection() {
        TalkProgress(this).markPrologueSeen()
        startActivity(selectionIntent(this))
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_PAGE, page)
        super.onSaveInstanceState(outState)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val KEY_PAGE = "page"
        private const val FADE_MS = 450L

        /** After login: the prologue for a player who has not seen it yet, otherwise straight to the selection. */
        fun afterLogin(context: Context): Intent =
            if (TalkProgress(context).shouldShowPrologue) Intent(context, PrologueActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            else selectionIntent(context)

        private fun selectionIntent(context: Context) = MainActivity.selectionIntent(context)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}

/** Reads talks.json once per call; returns null if the file is missing or broken so screens can fall back. */
object TalkRepository {
    fun load(context: Context): TalkScript? = try {
        context.assets.open("content/talks.json").bufferedReader().use { TalkScript.parse(it.readText()) }
    } catch (e: Exception) {
        android.util.Log.w("TalkRepository", "talks.json could not be read", e)
        null
    }
}

/** Density-independent size in pixels for custom-drawn talk views. */
internal fun View.dpF(v: Float) = v * resources.displayMetrics.density
