package com.tinyrunners.school.settings

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.audio.AudioSettings
import com.tinyrunners.school.audio.Music
import com.tinyrunners.school.audio.MusicSettings
import com.tinyrunners.school.audio.Sfx
import com.tinyrunners.school.audio.SfxSettings
import com.tinyrunners.school.audio.TalkSpeed
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.game.OnboardingStore
import com.tinyrunners.school.opening.OpeningActivity
import com.tinyrunners.school.shop.ShopActivity
import com.tinyrunners.school.shop.ShopBackground
import com.tinyrunners.school.weather.City
import com.tinyrunners.school.weather.WeatherClient
import com.tinyrunners.school.weather.WeatherSettings
import kotlin.math.roundToInt

/**
 * Settings in the shop's cosy style (cream fabric, honey frame, green controls). Laid out like most game options
 * screens (docs/research/SETTINGS_UI_RESEARCH.md): categories down the left, the chosen category's options on the
 * right, each a labelled row with a slider or switch and its value shown. Categories: 소리 (overall / music /
 * effects volumes, vibration), 대화·안내 (dialogue speed with a preview, replay guides), 날씨, 계정 (account,
 * app info, sign-out). Every change applies at once; there is no "save" button.
 */
class SettingsActivity : Activity() {
    private enum class Tab(val label: Int) { SOUND(R.string.settings_tab_sound), TALK(R.string.settings_tab_talk), WEATHER(R.string.settings_tab_weather), ACCOUNT(R.string.settings_tab_account) }

    private var tab = Tab.SOUND
    private lateinit var tabs: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var scroll: ScrollView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tab = savedInstanceState?.getString("tab")?.let { name -> Tab.entries.firstOrNull { it.name == name } } ?: Tab.SOUND

        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(26), dp(10), dp(26), dp(18)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(com.tinyrunners.school.ui.HomeButton.create(this).apply {
            textSize = 14f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply {
                setColor(ShopActivity.CARD); cornerRadius = dp(18).toFloat(); setStroke(dp(2), ShopActivity.HONEY)
            }, null)
            minHeight = dp(38); minimumHeight = dp(38); setPadding(dp(14), 0, dp(16), 0)
        })
        top.addView(View(this).apply { setBackgroundColor(ShopActivity.HONEY) }, LinearLayout.LayoutParams(dp(2), dp(28)).apply { marginStart = dp(14); marginEnd = dp(14) })
        top.addView(TextView(this).apply {
            setText(R.string.settings_title); textSize = 26f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        })
        top.addView(View(this).apply { setBackgroundColor(ShopActivity.HONEY) }, LinearLayout.LayoutParams(dp(2), dp(28)).apply { marginStart = dp(14); marginEnd = dp(14) })
        top.addView(TextView(this).apply { setText(R.string.settings_intro); textSize = 15f; setTextColor(INK_SOFT); maxLines = 1 },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(top)

        // Categories on the left, the chosen category on the right.
        val split = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        split.addView(tabs, LinearLayout.LayoutParams(dp(150), LinearLayout.LayoutParams.MATCH_PARENT))
        scroll = ScrollView(this).apply {
            isFillViewport = true; isVerticalScrollBarEnabled = true
            background = GradientDrawable().apply { setColor(ShopActivity.CARD); cornerRadius = dp(18).toFloat(); setStroke(dp(2), ShopActivity.HONEY_LIGHT) }
        }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(14), dp(22), dp(18)) }
        scroll.addView(body)
        split.addView(scroll, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = dp(16) })
        content.addView(split, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(12) })

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(ShopActivity.FABRIC)
            addView(ShopBackground(this@SettingsActivity), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
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
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("tab", tab.name)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun render() {
        tabs.removeAllViews()
        Tab.entries.forEachIndexed { i, t ->
            tabs.addView(Button(this).apply {
                val on = t == tab
                setText(t.label); textSize = 16f; isAllCaps = false; stateListAnimator = null; setTypeface(typeface, Typeface.BOLD)
                setTextColor(if (on) Color.WHITE else GREEN)
                gravity = Gravity.CENTER_VERTICAL or Gravity.START; setPadding(dp(18), 0, dp(12), 0)
                background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply {
                    setColor(if (on) GREEN else ShopActivity.CARD); cornerRadius = dp(20).toFloat(); setStroke(dp(2), if (on) GREEN else ShopActivity.HONEY_LIGHT)
                }, null)
                isSelected = on
                setOnClickListener { if (tab != t) { tab = t; handler.removeCallbacksAndMessages(null); render(); scroll.post { scroll.scrollTo(0, 0) } } }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { if (i > 0) topMargin = dp(10) })
        }
        body.removeAllViews()
        when (tab) {
            Tab.SOUND -> sound()
            Tab.TALK -> talk()
            Tab.WEATHER -> weather()
            Tab.ACCOUNT -> account()
        }
    }

    // ---- 소리 ---------------------------------------------------------------------------------------------------

    private fun sound() {
        val audio = AudioSettings(this)
        val music = MusicSettings(this)
        val sfx = SfxSettings(this)
        heading(R.string.settings_tab_sound, R.string.settings_sound_hint)

        slider(R.string.settings_master, audio.master) { v, done ->
            audio.master = v; Music.applyVolume(this)
            if (done) Sfx.play(this, Sfx.Sound.CHOICE)
        }
        rule()
        switch(R.string.settings_music_toggle, music.enabled) { on ->
            music.enabled = on
            if (on) Music.playGame(this) else Music.stop()
        }
        slider(R.string.settings_music_volume, audio.music) { v, _ -> audio.music = v; Music.applyVolume(this) }
        rule()
        switch(R.string.settings_sfx_toggle, sfx.enabled) { on -> sfx.enabled = on; if (on) Sfx.play(this, Sfx.Sound.CHOICE) }
        slider(R.string.settings_sfx_volume, audio.effects) { v, done -> audio.effects = v; if (done) Sfx.play(this, Sfx.Sound.CHOICE) }
        switch(R.string.settings_typing_sound, audio.typingSound) { on ->
            audio.typingSound = on
            if (on) typingDemo()
        }
        body.addView(smallButton(getString(R.string.settings_sfx_test)) { Sfx.play(this, Sfx.Sound.GIVE) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(36)).apply { topMargin = dp(4) })
        rule()
        switch(R.string.settings_vibration, audio.vibration) { on ->
            audio.vibration = on
            if (on) body.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        }
        note(R.string.settings_vibration_hint)
    }

    /** A short run of typing blips so the switch can be heard. */
    private fun typingDemo() {
        for (i in 0 until 8) handler.postDelayed({ Sfx.blip(this, "mint") }, i * 80L)
    }

    // ---- 대화·안내 -----------------------------------------------------------------------------------------------

    private fun talk() {
        val audio = AudioSettings(this)
        heading(R.string.settings_tab_talk, R.string.settings_talk_hint)
        label(R.string.settings_talk_speed)
        val preview = TextView(this).apply {
            textSize = 16f; setTextColor(INK); minLines = 2
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply { setColor(Color.rgb(250, 244, 230)); cornerRadius = dp(14).toFloat(); setStroke(dp(2), ShopActivity.HONEY_LIGHT) }
        }
        val choices = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun typePreview() {
            handler.removeCallbacksAndMessages(null)
            val line = getString(R.string.settings_talk_preview)
            var shown = 0
            val step = object : Runnable {
                override fun run() {
                    shown++
                    preview.text = line.take(shown)
                    if (shown < line.length) handler.postDelayed(this, audio.talkSpeed.charMs.toLong())
                }
            }
            handler.post(step)
        }
        fun drawChoices() {
            choices.removeAllViews()
            listOf(TalkSpeed.SLOW to R.string.settings_talk_slow, TalkSpeed.NORMAL to R.string.settings_talk_normal, TalkSpeed.FAST to R.string.settings_talk_fast)
                .forEachIndexed { i, (speed, label) ->
                    choices.addView(pill(getString(label), audio.talkSpeed == speed) {
                        audio.talkSpeed = speed; drawChoices(); typePreview()
                    }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { if (i > 0) marginStart = dp(10) })
                }
        }
        drawChoices()
        body.addView(choices, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        body.addView(preview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        typePreview()
        rule()
        label(R.string.settings_guide)
        val status = note(0)
        body.addView(smallButton(getString(R.string.settings_guide_replay)) {
            OnboardingStore(this).resetGameGuide()
            com.tinyrunners.school.talk.TalkProgress(this).resetAll()
            status.text = getString(R.string.settings_guide_reset)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)).apply { topMargin = dp(6) })
    }

    // ---- 날씨 ---------------------------------------------------------------------------------------------------

    private fun weather() {
        val settings = WeatherSettings(this)
        heading(R.string.settings_weather, R.string.settings_weather_hint)
        val preview = TextView(this).apply { textSize = 15f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD) }
        val cities = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun refresh() {
            if (!settings.enabled) { preview.text = getString(R.string.settings_weather_off); return }
            val city = settings.city
            preview.text = getString(R.string.settings_weather_loading)
            WeatherClient.fetch(city) { result ->
                if (isDestroyed || settings.city != city) return@fetch
                preview.text = result.fold(
                    { getString(R.string.weather_hud, getString(it.city.label), getString(it.sky.label), it.temperatureC) },
                    { getString(R.string.settings_weather_failed) })
            }
        }
        fun drawCities() {
            cities.removeAllViews()
            City.entries.chunked(4).forEach { rowCities ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                rowCities.forEachIndexed { i, city ->
                    row.addView(pill(getString(city.label), settings.city == city, enabled = settings.enabled) {
                        settings.city = city; drawCities(); refresh()
                    }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { if (i > 0) marginStart = dp(8) })
                }
                repeat(4 - rowCities.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(8) }) }
                cities.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
            }
        }
        switch(R.string.settings_weather_toggle, settings.enabled) { on -> settings.enabled = on; drawCities(); refresh() }
        label(R.string.settings_weather_city)
        body.addView(cities)
        drawCities()
        body.addView(preview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        refresh()
        note(R.string.settings_weather_source)
    }

    // ---- 계정 ---------------------------------------------------------------------------------------------------

    private fun account() {
        heading(R.string.settings_tab_account, 0)
        label(R.string.settings_account)
        if (AuthService.isGuest(this)) {
            value(getString(R.string.settings_guest))
        } else {
            value(AuthService.currentName(this) ?: getString(R.string.settings_no_name), bold = true)
            value(AuthService.currentEmail(this).orEmpty())
        }
        rule()
        label(R.string.settings_about)
        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (_: Exception) { null } ?: "-"
        value(getString(R.string.settings_version, version))
        note(R.string.settings_about_body)
        rule()
        body.addView(smallButton(getString(R.string.settings_sign_out), danger = true) {
            AuthService.signOut(this)
            startActivity(Intent(this, OpeningActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)).apply { topMargin = dp(6) })
    }

    // ---- Parts --------------------------------------------------------------------------------------------------

    private fun heading(title: Int, hint: Int) {
        body.addView(TextView(this).apply {
            setText(title); textSize = 20f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
            if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        })
        if (hint != 0) body.addView(TextView(this).apply { setText(hint); textSize = 13f; setTextColor(INK_SOFT); setPadding(0, dp(2), 0, dp(4)) })
    }

    private fun label(text: Int) = body.addView(TextView(this).apply {
        setText(text); textSize = 15f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(8), 0, 0)
    })

    private fun value(text: String, bold: Boolean = false) = body.addView(TextView(this).apply {
        this.text = text; textSize = 16f; setTextColor(INK); if (bold) setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(4), 0, 0)
    })

    private fun note(text: Int): TextView = TextView(this).apply {
        if (text != 0) setText(text); textSize = 13f; setTextColor(INK_SOFT); setPadding(0, dp(4), 0, 0)
    }.also { body.addView(it) }

    private fun rule() = body.addView(View(this).apply { setBackgroundColor(ShopActivity.HONEY_LIGHT) },
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(12); bottomMargin = dp(4) })

    /** A labelled 0–100 slider with its value; [onChange] gets (value, finished dragging). */
    private fun slider(label: Int, start: Int, onChange: (Int, Boolean) -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply { setText(label); textSize = 15f; setTextColor(INK) }, LinearLayout.LayoutParams(dp(120), LinearLayout.LayoutParams.WRAP_CONTENT))
        val shown = TextView(this).apply { textSize = 15f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD); gravity = Gravity.END; text = getString(R.string.settings_percent, start) }
        row.addView(SeekBar(this).apply {
            max = 100; progress = start
            progressTintList = ColorStateList.valueOf(GREEN)
            progressBackgroundTintList = ColorStateList.valueOf(ShopActivity.HONEY_LIGHT)
            thumbTintList = ColorStateList.valueOf(GREEN)
            contentDescription = getString(label)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                    shown.text = getString(R.string.settings_percent, value)
                    if (fromUser) onChange(value, false)
                }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = onChange(bar.progress, true)
            })
        }, LinearLayout.LayoutParams(0, dp(44), 1f))
        row.addView(shown, LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT))
        body.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
    }

    private fun switch(label: Int, on: Boolean, onChange: (Boolean) -> Unit) = body.addView(Switch(this).apply {
        setText(label); textSize = 15f; setTextColor(INK); isChecked = on; minHeight = dp(46)
        thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(GREEN, Color.rgb(176, 160, 132)))
        trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(ShopActivity.OWNED, Color.rgb(196, 178, 150)))
        setOnCheckedChangeListener { _, checked -> onChange(checked) }
    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) })

    private fun pill(text: String, on: Boolean, enabled: Boolean = true, click: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 15f; isAllCaps = false; stateListAnimator = null; setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(6), 0, dp(6), 0); minWidth = 0; minimumWidth = 0
        setTextColor(if (on) Color.WHITE else GREEN)
        background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply {
            setColor(if (on) GREEN else ShopActivity.CARD); cornerRadius = dp(20).toFloat(); setStroke(dp(2), if (on) GREEN else ShopActivity.HONEY_LIGHT)
        }, null)
        isEnabled = enabled; alpha = if (enabled) 1f else 0.45f
        setOnClickListener { click() }
    }

    private fun smallButton(text: String, danger: Boolean = false, click: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 14f; isAllCaps = false; stateListAnimator = null; setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(16), 0, dp(16), 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        val ink = if (danger) Color.rgb(176, 74, 58) else GREEN
        setTextColor(ink)
        background = RippleDrawable(ColorStateList.valueOf(ShopActivity.OWNED), GradientDrawable().apply {
            setColor(ShopActivity.CARD); cornerRadius = dp(16).toFloat(); setStroke(dp(2), ink)
        }, null)
        setOnClickListener { click() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private companion object {
        val GREEN = ShopActivity.GREEN
        val INK = Color.parseColor("#3E3A2E")
        val INK_SOFT = Color.parseColor("#5E5A4C")
    }
}
