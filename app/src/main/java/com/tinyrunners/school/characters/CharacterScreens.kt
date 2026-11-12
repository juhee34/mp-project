package com.tinyrunners.school.characters

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
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
import com.tinyrunners.school.stories.StoryActivity
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

    /** The friend currently shown on the selection stage, if any (the shop previews decorations on them). */
    val currentFriendId: String? get() = selectedId

    fun restore(state: Bundle?) {
        selectedId = state?.getString("selectedId")
        selectionScroll = state?.getInt("selectionScroll") ?: 0
        when (state?.getString("route")) {
            Route.SELECT.name -> showSelection()
            Route.PROFILE.name -> showProfile(selectedId.orEmpty(), state.getInt("profileScroll"))
            // The old intro page is gone: the hub after login does that job, so this screen opens on the selection.
            else -> showSelection()
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
            // Back from the selection leaves this screen (to the hub).
            Route.SELECT, Route.INTRO -> return false
        }
        return true
    }

    /**
     * Friend selection laid out like most games' character select (docs/research/CHARACTER_SELECT_RESEARCH.md): a big
     * stage on the left with the chosen friend standing at home, an info panel on the right (name, species and home,
     * tagline, short introduction, best record, "라일이랑 달리기", profile and story book), and a roster of six round
     * portraits along the bottom to switch friends. Swiping the stage or its ◀ ▶ also switches. The chosen friend is
     * the one the shop previews decorations on.
     */
    fun showSelection() {
        if (repository.find(selectedId.orEmpty()) == null) selectedId = repository.characters.first().id
        val friends = repository.characters
        route = Route.SELECT
        host.removeAllViews()
        // The selection does not scroll up and down; this keeps save() working.
        scroll = ScrollView(context)
        val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(2), dp(16), dp(8)) }
        host.addView(body, FrameLayout.LayoutParams(MATCH, MATCH))

        val top = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(com.tinyrunners.school.ui.HomeButton.create(context))
        top.addView(TextView(context).apply {
            text = context.getString(R.string.characters_select_title)
            textSize = 22f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD)
            if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        body.addView(top, LinearLayout.LayoutParams(MATCH, WRAP))

        // Stage (left) and info panel (right).
        val main = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val stageBox = FrameLayout(context)
        val stage = ShowcaseStage(context)
        stageBox.addView(stage, FrameLayout.LayoutParams(MATCH, MATCH))
        val prev = stageArrow("◀", R.string.characters_prev)
        val next = stageArrow("▶", R.string.characters_next)
        stageBox.addView(prev, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.START or Gravity.CENTER_VERTICAL).apply { marginStart = dp(8) })
        stageBox.addView(next, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = dp(8) })
        main.addView(stageBox, LinearLayout.LayoutParams(0, MATCH, 1.15f))

        val info = com.tinyrunners.school.ui.CardShelf.card(context).apply { setPadding(dp(14), dp(6), dp(14), dp(8)) }
        val name = TextView(context).apply { textSize = 22f; setTextColor(com.tinyrunners.school.ui.CardShelf.GREEN); setTypeface(typeface, Typeface.BOLD) }
        val tag = com.tinyrunners.school.ui.CardShelf.chip(context, "")
        val tagline = TextView(context).apply { textSize = 15f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD); maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }
        val intro = TextView(context).apply {
            textSize = 14f; setTextColor(com.tinyrunners.school.ui.CardShelf.INK_SOFT); setLineSpacing(0f, 1.15f)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val best = TextView(context).apply { textSize = 13f; setTextColor(LEAF); setTypeface(typeface, Typeface.BOLD) }
        val nameRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(name)
        nameRow.addView(tag, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = dp(10) })
        info.addView(nameRow)
        info.addView(tagline, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
        // The introduction takes the height that is left, showing only whole lines.
        info.addView(intro, LinearLayout.LayoutParams(MATCH, 0, 1f).apply { topMargin = dp(4) })
        info.addView(best, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(2) })
        val run = com.tinyrunners.school.ui.CardShelf.button(context, "", primary = true) { onStartRun(selectedId.orEmpty()) }
        info.addView(run, LinearLayout.LayoutParams(MATCH, dp(38)).apply { topMargin = dp(4) })
        val more = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        more.addView(com.tinyrunners.school.ui.CardShelf.button(context, context.getString(R.string.characters_profile_short), primary = false) {
            showProfile(selectedId.orEmpty())
        }, LinearLayout.LayoutParams(0, dp(32), 1f))
        more.addView(com.tinyrunners.school.ui.CardShelf.button(context, context.getString(R.string.characters_story_book), primary = false) {
            context.startActivity(StoryActivity.intent(context, selectedId.orEmpty()))
        }, LinearLayout.LayoutParams(0, dp(32), 1f).apply { marginStart = dp(8) })
        info.addView(more, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(5) })
        main.addView(info, LinearLayout.LayoutParams(0, MATCH, 1f).apply { marginStart = dp(12) })
        body.addView(main, LinearLayout.LayoutParams(MATCH, 0, 1f).apply { topMargin = dp(4) })

        // Roster: six round portraits along the bottom.
        val roster = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(dp(14), dp(3), dp(14), dp(2))
            // A cream strip so the names read well over the bench in the picture.
            background = shape(Color.argb(232, 255, 249, 235), 22)
        }
        body.addView(roster, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(5); gravity = Gravity.CENTER_HORIZONTAL })

        var bests: Map<String, Int> = emptyMap()

        fun bind() {
            val friend = friends.first { it.id == selectedId }
            val tint = friendColor(friend)
            stage.show(friend.id, tint)
            name.text = friend.name
            tag.text = context.getString(R.string.characters_species_home, friend.species, friend.home)
            tag.background = shape(blend(tint, PAPER, 0.62f), 10)
            tagline.text = friend.copy?.tagline ?: friend.personality
            intro.text = friend.copy?.selectionIntro ?: friend.summary
            intro.maxLines = Int.MAX_VALUE
            intro.post {
                val lines = intro.height / intro.lineHeight.coerceAtLeast(1)
                intro.visibility = if (lines < 1) View.INVISIBLE else View.VISIBLE
                intro.maxLines = lines.coerceAtLeast(1)
            }
            val b = bests[friend.id]
            best.text = if (b != null && b > 0) context.getString(R.string.characters_best, b) else context.getString(R.string.characters_best_none)
            run.text = runLabel(friend)
            info.contentDescription = context.getString(R.string.characters_card_description_short,
                friend.name, friend.species, friend.home, tagline.text.toString() + " " + greetingFor(friend))
            roster.removeAllViews()
            friends.forEach { f ->
                val on = f.id == selectedId
                val cell = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
                cell.addView(RosterPortrait(context, f.id, friendColor(f), on).apply {
                    contentDescription = f.name
                    setOnClickListener { if (selectedId != f.id) { selectedId = f.id; bind() } }
                }, LinearLayout.LayoutParams(dp(if (on) 50 else 42), dp(if (on) 50 else 42)))
                cell.addView(TextView(context).apply {
                    text = f.name; textSize = 11f; gravity = Gravity.CENTER; includeFontPadding = false
                    setTextColor(if (on) com.tinyrunners.school.ui.CardShelf.GREEN else MUTED)
                    if (on) setTypeface(typeface, Typeface.BOLD)
                })
                roster.addView(cell, LinearLayout.LayoutParams(dp(70), WRAP))
            }
        }

        fun go(step: Int) {
            val index = friends.indexOfFirst { it.id == selectedId }
            selectedId = friends[(index + step + friends.size) % friends.size].id
            bind()
            stage.announceForAccessibility(friends.first { it.id == selectedId }.name)
        }
        prev.setOnClickListener { go(-1) }
        next.setOnClickListener { go(1) }

        // Sideways swipe on the stage switches friends.
        var downX = 0f
        var downY = 0f
        @Suppress("ClickableViewAccessibility")
        stage.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; v.parent.requestDisallowInterceptTouchEvent(true) }
                android.view.MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    if (kotlin.math.abs(dx) > dp(40) && kotlin.math.abs(dx) > kotlin.math.abs(event.y - downY)) go(if (dx < 0) 1 else -1)
                }
            }
            true
        }
        bind()
        // Best distance per friend for the panel (cloud or this device).
        com.tinyrunners.school.progress.ProgressStore.forCurrentUser(context).loadAll { result ->
            if (route != Route.SELECT) return@loadAll
            bests = result.getOrNull().orEmpty().mapValues { it.value.bestMeters }
            bind()
        }
    }

    private fun stageArrow(symbol: String, description: Int) = Button(context).apply {
        text = symbol; textSize = 15f; setTextColor(com.tinyrunners.school.ui.CardShelf.GREEN)
        contentDescription = context.getString(description)
        setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        background = RippleDrawable(ColorStateList.valueOf(0x33466F53), GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(Color.argb(225, 255, 249, 235)); setStroke(dp(2), com.tinyrunners.school.ui.CardShelf.GREEN)
        }, null)
        stateListAnimator = null
    }

    private fun greetingFor(friend: CharacterDefinition): String {
        val greeting = when (friend.id) {
            "mint" -> R.string.characters_greeting_mint
            "cheese" -> R.string.characters_greeting_cheese
            "popo" -> R.string.characters_greeting_popo
            "lumi" -> R.string.characters_greeting_lumi
            "nunu" -> R.string.characters_greeting_nunu
            "piko" -> R.string.characters_greeting_piko
            else -> return friend.personality
        }
        return context.getString(greeting)
    }

    /** "라일이랑 달리기", "치즈랑 달리기": the friend's name with the right particle. */
    private fun runLabel(friend: CharacterDefinition): String =
        context.getString(R.string.characters_copy_run, com.tinyrunners.school.talk.Josa.with(friend.name, "이랑", "랑"))

    private fun friendColor(friend: CharacterDefinition): Int =
        try { Color.parseColor(friend.color) } catch (_: IllegalArgumentException) { LEAF }

    private fun blend(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) * (1 - t) + Color.red(b) * t).roundToInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).roundToInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).roundToInt())

    private fun rowButton(label: Int, primary: Boolean = false, click: () -> Unit) = Button(context).apply {
        setText(label)
        textSize = 15f
        isAllCaps = false
        setTextColor(if (primary) Color.WHITE else LEAF)
        if (primary) setTypeface(typeface, Typeface.BOLD)
        minHeight = dp(52)
        setPadding(dp(8), dp(8), dp(8), dp(8))
        background = ripple(if (primary) LEAF else PAPER, if (primary) LEAF else LINE)
        stateListAnimator = null
        setOnClickListener { click() }
    }

    private fun showProfile(id: String, scrollY: Int = 0) {
        val friend = repository.find(id) ?: run {
            selectedId = null
            showSelection()
            return
        }
        selectedId = friend.id
        val body = screen(Route.PROFILE)
        // Back to the selection at the top-left, not at the bottom of the scrolling page.
        body.addView(com.tinyrunners.school.ui.HomeButton.create(context, context.getString(R.string.characters_back_selection_pill)) { showSelection() },
            0, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) })
        val profile = panel()
        profile.background = shape(MINT, 22)
        profile.addView(portrait(friend), LinearLayout.LayoutParams(dp(96), dp(96)))
        profile.label(context.getString(R.string.characters_placeholder_portrait), 13f, MUTED, 8)
        profile.label(friend.name, 32f, top = 12, bold = true, heading = true)
        profile.label(friend.species, 16f, MUTED, 4)
        body.addView(profile, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })

        val copy = friend.copy
        if (copy == null) {
            val details = panel()
            details.background = shape(PAPER, 18, LINE)
            details.detail(R.string.characters_home, friend.home)
            details.detail(R.string.characters_personality, friend.personality, 16)
            details.detail(R.string.characters_story_outline, friend.summary, 16)
            body.addView(details, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(14) })
            body.action(R.string.characters_run, primary = true) { onStartRun(friend.id) }
            body.action(R.string.characters_story_book) { context.startActivity(StoryActivity.intent(context, friend.id)) }
            restoreScroll(scrollY)
            return
        }
        profile.label(copy.tagline, 18f, LEAF, 10, bold = true)

        // A's friendly profile, in small cards rather than one long block.
        fun section(title: Int, fill: Int = PAPER): LinearLayout = panel().apply {
            background = shape(fill, 18, LINE)
            label(context.getString(title), 15f, LEAF, bold = true, heading = true)
            body.addView(this, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })
        }
        section(R.string.characters_copy_home).label(friend.home, 17f, top = 5)
        section(R.string.characters_copy_personality).apply {
            label(copy.personality, 17f, top = 5)
            copy.traits.forEach { trait ->
                addView(TextView(context).apply {
                    text = trait; textSize = 14f; setTextColor(LEAF); setTypeface(typeface, Typeface.BOLD)
                    setPadding(dp(12), dp(5), dp(12), dp(5))
                    background = shape(MINT, 14)
                }, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(6) })
            }
        }
        section(R.string.characters_copy_habits).label(copy.habits.joinToString("\n") { "· " + it }, 16f, top = 5)
        section(R.string.characters_copy_cherished).label(copy.cherishedItem, 16f, top = 5)
        section(R.string.characters_copy_running).label(copy.runningReason, 16f, top = 5)
        section(R.string.characters_copy_story, MINT).apply {
            label(copy.storyTeaser, 16f, top = 5)
            action(R.string.characters_copy_story_button) { context.startActivity(StoryActivity.intent(context, friend.id)) }
        }
        body.label(context.getString(R.string.characters_copy_mechanics), 14f, MUTED, 14)
        val run = Button(context).apply {
            text = runLabel(friend); textSize = 16f; isAllCaps = false; setTextColor(Color.WHITE)
            minHeight = dp(52); setPadding(dp(14), dp(10), dp(14), dp(10))
            background = ripple(LEAF, LEAF); stateListAnimator = null
            setOnClickListener { onStartRun(friend.id) }
        }
        body.addView(run, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })
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

    private fun portraitArtwork(friend: CharacterDefinition): Drawable {
        val bitmap = CharacterSprites.load(context.resources, friend.id)?.get(SpriteFrame.IDLE_0)
            ?: return CharacterPortrait(friend.color)
        return BitmapDrawable(context.resources, bitmap).apply {
            isFilterBitmap = false
            setAntiAlias(false)
        }
    }

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
