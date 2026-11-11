package com.tinyrunners.school.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import com.tinyrunners.school.R
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.opening.OpeningActivity
import com.tinyrunners.school.game.OnboardingStore
import com.tinyrunners.school.ui.Page
import com.tinyrunners.school.weather.City
import com.tinyrunners.school.weather.WeatherClient
import com.tinyrunners.school.weather.WeatherSettings

/** Account information, the weather city for the runner's sky, and sign-out. */
class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = WeatherSettings(this)
        val page = Page(this).show()
        page.backPill()
        page.eyebrow(getString(R.string.characters_brand))
        page.title(getString(R.string.settings_title))

        val account = page.card(16)
        page.line(account, getString(R.string.settings_account), 13f, Page.LEAF, bold = true, top = 0)
        if (AuthService.isGuest(this)) {
            page.line(account, getString(R.string.settings_guest), 16f, Page.INK)
        } else {
            page.line(account, AuthService.currentName(this) ?: getString(R.string.settings_no_name), 16f, Page.INK, bold = true)
            page.line(account, AuthService.currentEmail(this).orEmpty())
        }

        val weather = page.card(16)
        page.line(weather, getString(R.string.settings_weather), 13f, Page.LEAF, bold = true, top = 0)
        page.line(weather, getString(R.string.settings_weather_hint))
        val toggle = Switch(this).apply {
            text = getString(R.string.settings_weather_toggle)
            textSize = 16f
            setTextColor(Page.INK)
            isChecked = settings.enabled
            minHeight = page.dp(48)
        }
        weather.addView(toggle, page.params(8))
        val group = RadioGroup(this)
        City.entries.forEach { city ->
            group.addView(RadioButton(this).apply {
                id = city.ordinal + 1
                text = getString(city.label)
                textSize = 16f
                setTextColor(Page.INK)
                minHeight = page.dp(44)
                isChecked = city == settings.city
            })
        }
        group.isEnabled = settings.enabled
        weather.addView(group, page.params(4))
        val preview = page.line(weather, "", 14f)

        fun refreshPreview() {
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
        toggle.setOnCheckedChangeListener { _, on ->
            settings.enabled = on
            for (i in 0 until group.childCount) group.getChildAt(i).isEnabled = on
            refreshPreview()
        }
        for (i in 0 until group.childCount) group.getChildAt(i).isEnabled = settings.enabled
        group.setOnCheckedChangeListener { _, checkedId ->
            City.entries.getOrNull(checkedId - 1)?.let { settings.city = it; refreshPreview() }
        }
        page.text(getString(R.string.settings_weather_source), 12f)
        refreshPreview()

        // Background music on every screen.
        val music = page.card(16)
        page.line(music, getString(R.string.settings_music), 13f, Page.LEAF, bold = true, top = 0)
        val musicSettings = com.tinyrunners.school.audio.MusicSettings(this)
        music.addView(Switch(this).apply {
            text = getString(R.string.settings_music_toggle)
            textSize = 16f
            setTextColor(Page.INK)
            isChecked = musicSettings.enabled
            minHeight = page.dp(48)
            setOnCheckedChangeListener { _, on ->
                musicSettings.enabled = on
                if (on) com.tinyrunners.school.audio.Music.playGame(this@SettingsActivity) else com.tinyrunners.school.audio.Music.stop()
            }
        }, page.params(8))
        val sfx = com.tinyrunners.school.audio.SfxSettings(this)
        music.addView(Switch(this).apply {
            text = getString(R.string.settings_sfx_toggle)
            textSize = 16f
            setTextColor(Page.INK)
            isChecked = sfx.enabled
            minHeight = page.dp(48)
            setOnCheckedChangeListener { _, on ->
                sfx.enabled = on
                if (on) com.tinyrunners.school.audio.Sfx.play(this@SettingsActivity, com.tinyrunners.school.audio.Sfx.Sound.NEXT)
            }
        }, page.params(4))

        val guide = page.card(16)
        page.line(guide, getString(R.string.settings_guide), 13f, Page.LEAF, bold = true, top = 0)
        val guideStatus = page.line(guide, "")
        guide.addView(android.widget.Button(this).apply {
            text = getString(R.string.settings_guide_replay)
            isAllCaps = false
            setTextColor(Page.LEAF)
            minHeight = page.dp(48)
            background = page.shape(Page.MINT, 12)
            setOnClickListener {
                OnboardingStore(this@SettingsActivity).resetGameGuide()
                com.tinyrunners.school.talk.TalkProgress(this@SettingsActivity).resetAll()
                guideStatus.text = getString(R.string.settings_guide_reset)
            }
        }, page.params(8))

        page.button(getString(R.string.settings_sign_out)) {
            AuthService.signOut(this)
            startActivity(Intent(this, OpeningActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
        // The way out is the "◀ 처음으로" button at the top.
    }
}
