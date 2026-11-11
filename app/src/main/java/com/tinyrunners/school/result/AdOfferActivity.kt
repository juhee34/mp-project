package com.tinyrunners.school.result

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.ui.Page
import com.tinyrunners.school.ui.SceneryView
import kotlin.math.roundToInt

/**
 * After a crash, before the result: an optional rewarded ad. "광고 보기" plays a short ad and then gives this run's
 * coins once more; "괜찮아요" goes straight to the result. The ad here is a demonstration screen (this school build
 * has no ad network account); [AdPlayer] is where a real rewarded ad (for example AdMob) would be shown instead.
 */
class AdOfferActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var done = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOffer()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @Deprecated("Back during the offer or the ad means no ad: on to the result.")
    override fun onBackPressed() = toResult(0)

    private val coins get() = intent.getIntExtra(EXTRA_COINS, 0)

    private fun showOffer() {
        val card = card()
        card.addView(label(getString(R.string.ad_offer_title), 22f, Page.INK, bold = true))
        card.addView(label(getString(R.string.ad_offer_body, coins), 16f, Page.MUTED).apply { setPadding(0, dp(8), 0, 0) })
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(16), 0, 0) }
        buttons.addView(button(getString(R.string.ad_offer_decline), primary = false) { toResult(0) },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(10) })
        buttons.addView(button(getString(R.string.ad_offer_watch), primary = true) { playAd() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        card.addView(buttons)
        show(card, scenery = true)
    }

    /** The ad: a few seconds that cannot be skipped, then the reward. */
    private fun playAd() {
        AdPlayer.show(this, onFinished = { rewarded -> if (rewarded) reward() else toResult(0) }) { ad -> show(ad, scenery = false) }
    }

    private fun reward() {
        // The coins are saved with the account, then the result screen opens.
        com.tinyrunners.school.shop.ShopStore.forCurrentUser(this).addFound(coins) { }
        toResult(coins)
    }

    private fun toResult(bonus: Int) {
        if (done) return
        done = true
        @Suppress("DEPRECATION")
        val result = intent.getParcelableExtra<Intent>(EXTRA_RESULT) ?: run { finish(); return }
        startActivity(result.putExtra(ResultActivity.EXTRA_AD_BONUS, bonus))
        finish()
    }

    private fun show(content: android.view.View, scenery: Boolean) {
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(if (scenery) Page.BACKGROUND else Color.rgb(20, 18, 30))
            if (scenery) {
                addView(SceneryView(this@AdOfferActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                addView(android.view.View(this@AdOfferActivity).apply { setBackgroundColor(Color.argb(110, 20, 30, 24)) },
                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            }
            addView(content, FrameLayout.LayoutParams(dp(460), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        })
    }

    internal fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(26), dp(22), dp(26), dp(20))
        background = GradientDrawable().apply { setColor(Page.PAPER); cornerRadius = dp(22).toFloat() }
    }

    internal fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    internal fun button(text: String, primary: Boolean, click: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 16f; isAllCaps = false; stateListAnimator = null
        minHeight = 0; minimumHeight = 0; setPadding(dp(8), 0, dp(8), 0)
        setTextColor(if (primary) Color.WHITE else Page.LEAF)
        if (primary) typeface = Typeface.DEFAULT_BOLD
        background = RippleDrawable(ColorStateList.valueOf(Page.MINT), GradientDrawable().apply {
            setColor(if (primary) Page.LEAF else Page.PAPER); cornerRadius = dp(16).toFloat(); setStroke(dp(1), if (primary) Page.LEAF else Page.LINE)
        }, null)
        setOnClickListener { click() }
    }

    internal fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
    internal fun later(ms: Long, block: () -> Unit) = handler.postDelayed(block, ms)

    companion object {
        private const val EXTRA_RESULT = "adResult"
        private const val EXTRA_COINS = "adCoins"

        /** Offer an ad that doubles [coins] (this run's coins), then open [result]. */
        fun intent(context: Context, result: Intent, coins: Int) = Intent(context, AdOfferActivity::class.java)
            .putExtra(EXTRA_RESULT, result).putExtra(EXTRA_COINS, coins)
    }
}

/**
 * Shows a rewarded ad. This build plays a demonstration ad (a short countdown, clearly labelled); swapping in a real
 * ad network only needs this object to change: call [onFinished] with true when the ad grants its reward.
 */
internal object AdPlayer {
    const val SECONDS = 5

    fun show(host: AdOfferActivity, onFinished: (Boolean) -> Unit, display: (android.view.View) -> Unit) {
        val ad = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(host.dp(26), host.dp(22), host.dp(26), host.dp(20))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(98, 72, 160), Color.rgb(214, 92, 120))).apply {
                cornerRadius = host.dp(22).toFloat()
            }
        }
        ad.addView(host.label(host.getString(R.string.ad_label), 13f, Color.argb(220, 255, 255, 255), bold = true))
        ad.addView(host.label(host.getString(R.string.ad_demo_title), 24f, Color.WHITE, bold = true).apply { gravity = Gravity.CENTER; setPadding(0, host.dp(10), 0, 0) })
        ad.addView(host.label(host.getString(R.string.ad_demo_body), 15f, Color.argb(235, 255, 255, 255)).apply { gravity = Gravity.CENTER; setPadding(0, host.dp(6), 0, 0) })
        val count = host.label("", 15f, Color.WHITE, bold = true).apply { setPadding(0, host.dp(16), 0, 0) }
        ad.addView(count)
        val claim = host.button(host.getString(R.string.ad_claim), primary = true) { onFinished(true) }.apply { visibility = android.view.View.GONE }
        ad.addView(claim, LinearLayout.LayoutParams(host.dp(220), host.dp(48)).apply { topMargin = host.dp(10) })
        display(ad)
        for (left in SECONDS downTo 0) host.later(((SECONDS - left) * 1000).toLong()) {
            if (left > 0) count.text = host.getString(R.string.ad_countdown, left)
            else { count.setText(R.string.ad_finished); claim.visibility = android.view.View.VISIBLE }
        }
    }
}
