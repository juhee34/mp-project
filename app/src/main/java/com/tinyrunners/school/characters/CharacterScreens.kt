package com.tinyrunners.school.characters

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.data.CharacterRepository
import kotlin.math.roundToInt

internal class CharacterScreens(
    private val host: FrameLayout,
    private val repository: CharacterRepository,
    private val onStartRun: (characterId: String) -> Unit
) {
    private val context = host.context
    private lateinit var scroll: ScrollView
    private var route = Route.INTRO
    private var selectedId: String? = null
    private var selectionScroll = 0

    fun restore(state: Bundle?) {
        selectedId = state?.getString("selectedId")
        selectionScroll = state?.getInt("selectionScroll") ?: 0
        when (state?.getString("route")) {
            Route.SELECT.name -> showSelection()
            Route.PROFILE.name -> showProfile(selectedId.orEmpty(), state.getInt("profileScroll"))
            else -> showIntro()
        }
    }

    fun save(state: Bundle) {
        state.putString("route", route.name)
        state.putString("selectedId", selectedId)
        state.putInt("selectionScroll", if (route == Route.SELECT) scroll.scrollY else selectionScroll)
        if (route == Route.PROFILE) state.putInt("profileScroll", scroll.scrollY)
    }

    fun goBack(): Boolean {
        when (route) {
            Route.PROFILE -> showSelection()
            Route.SELECT -> showIntro()
            Route.INTRO -> return false
        }
        return true
    }

    private fun showIntro() {
        val body = screen(Route.INTRO)
        val cover = panel()
        cover.background = shape(LEAF, 22)
        cover.label(context.getString(R.string.characters_intro_eyebrow), 13f, MINT, bold = true)
        cover.label(context.getString(R.string.characters_intro_title), 36f, Color.WHITE, 10, bold = true, heading = true)
        cover.label(context.getString(R.string.characters_intro_body), 16f, Color.WHITE, 12)
        body.addView(cover, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })

        body.label(context.getString(R.string.characters_intro_friends, repository.characters.size), 20f, top = 24, bold = true, heading = true)
        val portraitsPerRow = (repository.characters.size + 1) / 2
        repository.characters.chunked(portraitsPerRow).forEach { friends ->
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            friends.forEach { friend ->
                row.addView(portrait(friend), LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginEnd = dp(6)
                })
            }
            body.addView(row, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(6) })
        }
        body.label(context.getString(R.string.characters_placeholder_notice), 14f, MUTED, 8)
        body.action(R.string.characters_choose, primary = true) { showSelection() }
    }

    fun showSelection() {
        if (repository.find(selectedId.orEmpty()) == null) selectedId = repository.characters.first().id
        val body = screen(Route.SELECT)
        body.label(context.getString(R.string.characters_select_title), 28f, top = 8, bold = true, heading = true)
        body.label(context.getString(R.string.characters_select_hint, repository.characters.size), 16f, MUTED, 8)
        body.label(context.getString(R.string.characters_placeholder_notice), 14f, MUTED, 4)

        repository.characters.forEach { friend ->
            val selected = friend.id == selectedId
            val name = context.getString(R.string.characters_name_and_species, friend.name, friend.species)
            val title = if (selected) context.getString(R.string.characters_selected_name, name) else name
            val card = Button(context).apply {
                text = SpannableStringBuilder(title).apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    append('\n').append(friend.personality.substringBefore('.'))
                    append('\n').append(friend.home)
                }
                textSize = 16f
                isAllCaps = false
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                isSelected = selected
                setSingleLine(false)
                setTextColor(INK)
                setPaddingRelative(dp(12), dp(12), dp(12), dp(12))
                minHeight = dp(76)
                compoundDrawablePadding = dp(12)
                val thumbnail = portraitArtwork(friend).apply { setBounds(0, 0, dp(48), dp(48)) }
                setCompoundDrawablesRelative(thumbnail, null, null, null)
                background = ripple(if (selected) MINT else PAPER, if (selected) LEAF else LINE)
                stateListAnimator = null
                setOnClickListener {
                    selectionScroll = scroll.scrollY
                    selectedId = friend.id
                    showSelection()
                }
            }
            body.addView(card, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })
        }
        repository.find(selectedId.orEmpty())?.let { friend ->
            body.label(context.getString(R.string.characters_selected_name, friend.name), 18f, top = 16, bold = true)
            body.action(R.string.characters_run, primary = true) { onStartRun(friend.id) }
            body.action(R.string.characters_profile) { selectionScroll = scroll.scrollY; showProfile(friend.id) }
        }
        body.action(R.string.characters_back_intro) { showIntro() }
        restoreScroll(selectionScroll)
    }

    private fun showProfile(id: String, scrollY: Int = 0) {
        val friend = repository.find(id) ?: run {
            selectedId = null
            showSelection()
            return
        }
        selectedId = friend.id
        val body = screen(Route.PROFILE)
        val profile = panel()
        profile.background = shape(MINT, 22)
        profile.addView(portrait(friend), LinearLayout.LayoutParams(dp(96), dp(96)))
        profile.label(context.getString(R.string.characters_placeholder_portrait), 13f, MUTED, 8)
        profile.label(friend.name, 32f, top = 12, bold = true, heading = true)
        profile.label(friend.species, 16f, MUTED, 4)
        body.addView(profile, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })

        val details = panel()
        details.background = shape(PAPER, 18, LINE)
        details.detail(R.string.characters_home, friend.home)
        details.detail(R.string.characters_personality, friend.personality, 16)
        details.detail(R.string.characters_story_outline, friend.summary, 16)
        body.addView(details, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(14) })

        body.action(R.string.characters_run, primary = true) { onStartRun(friend.id) }
        body.action(R.string.characters_back_selection) { showSelection() }
        restoreScroll(scrollY)
    }

    private fun screen(next: Route): LinearLayout {
        route = next
        host.removeAllViews()
        scroll = ScrollView(context).apply { isFillViewport = true }
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        scroll.addView(body)
        host.addView(scroll, FrameLayout.LayoutParams(MATCH, MATCH))
        body.label(context.getString(R.string.characters_brand), 13f, LEAF, bold = true)
        return body
    }

    private fun restoreScroll(y: Int) {
        val page = scroll
        if (y > 0) page.post { page.scrollTo(0, y) }
    }

    private fun panel() = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
    }

    private fun portrait(friend: CharacterDefinition) = ImageView(context).apply {
        setImageDrawable(portraitArtwork(friend))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun portraitArtwork(friend: CharacterDefinition): Drawable = CharacterPortrait(friend.color)

    private fun LinearLayout.detail(title: Int, value: String, top: Int = 0) {
        label(context.getString(title), 15f, LEAF, top, bold = true, heading = true)
        label(value, 17f, top = 5)
    }

    private fun LinearLayout.label(
        value: CharSequence,
        size: Float,
        color: Int = INK,
        top: Int = 0,
        bold: Boolean = false,
        heading: Boolean = false
    ) {
        val label = TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            setLineSpacing(dp(2).toFloat(), 1f)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            if (heading && android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        }
        addView(label, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(top) })
    }

    private fun LinearLayout.action(label: Int, primary: Boolean = false, click: () -> Unit) {
        val button = Button(context).apply {
            setText(label)
            textSize = 16f
            isAllCaps = false
            setTextColor(if (primary) Color.WHITE else LEAF)
            minHeight = dp(52)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = ripple(if (primary) LEAF else PAPER, if (primary) LEAF else LINE)
            stateListAnimator = null
            setOnClickListener { click() }
        }
        addView(button, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })
    }

    private fun ripple(fill: Int, border: Int) =
        RippleDrawable(ColorStateList.valueOf(0x33466F53), shape(fill, 16, border), null)

    private fun shape(fill: Int, radius: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (border != null) setStroke(dp(1), border)
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()

    private enum class Route { INTRO, SELECT, PROFILE }

    private companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
        val LEAF = Color.rgb(48, 83, 65)
        val MINT = Color.rgb(225, 239, 218)
        val PAPER = Color.rgb(255, 253, 246)
        val LINE = Color.rgb(221, 224, 209)
        val INK = Color.rgb(38, 52, 43)
        val MUTED = Color.rgb(79, 96, 84)
    }
}
