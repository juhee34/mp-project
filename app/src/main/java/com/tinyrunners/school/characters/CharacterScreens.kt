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
     * Friend selection as a swipeable stage: one friend at a time, large and gently bobbing on the left, with their
     * description on the right. Swipe sideways or use the arrows to meet the next friend; everything fits on one
     * landscape screen, so nothing needs scrolling.
     */
    fun showSelection() {
        if (repository.find(selectedId.orEmpty()) == null) selectedId = repository.characters.first().id
        val friends = repository.characters
        route = Route.SELECT
        host.removeAllViews()
        scroll = ScrollView(context).apply { isFillViewport = true }
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(12))
        }
        scroll.addView(body)
        host.addView(scroll, FrameLayout.LayoutParams(MATCH, MATCH))

        // Title row: back to the intro, title, and which friend of six this is.
        val top = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(smallButton(context.getString(R.string.characters_back_hub)) { (context as? android.app.Activity)?.finish() })
        top.addView(TextView(context).apply {
            text = context.getString(R.string.characters_select_title)
            textSize = 22f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD)
            if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        val dots = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(dots)
        body.addView(top, LinearLayout.LayoutParams(MATCH, WRAP))

        // Stage: ◀ [card] ▶
        val stage = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val prev = arrowButton("◀", R.string.characters_prev)
        val next = arrowButton("▶", R.string.characters_next)
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(12), dp(22), dp(12))
        }
        val spot = FrameLayout(context)
        val sprite = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        spot.addView(sprite, FrameLayout.LayoutParams(MATCH, MATCH))
        card.addView(spot, LinearLayout.LayoutParams(dp(150), dp(150)))
        val info = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), 0, 0, 0) }
        val name = TextView(context).apply { textSize = 26f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD) }
        val tag = TextView(context).apply {
            textSize = 14f; setTextColor(LEAF); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(10), dp(3), dp(10), dp(3))
        }
        val personality = TextView(context).apply { textSize = 15f; setTextColor(INK); setLineSpacing(dp(2).toFloat(), 1f) }
        val summary = TextView(context).apply {
            textSize = 13f; setTextColor(MUTED); setLineSpacing(dp(2).toFloat(), 1f)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        info.addView(name, LinearLayout.LayoutParams(WRAP, WRAP))
        info.addView(tag, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(4) })
        info.addView(personality, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })
        // The summary takes whatever height is left and shows only whole lines (ending in …), never a cut-off line.
        info.addView(summary, LinearLayout.LayoutParams(MATCH, 0, 1f).apply { topMargin = dp(6) })
        card.addView(info, LinearLayout.LayoutParams(0, MATCH, 1f))
        stage.addView(prev, LinearLayout.LayoutParams(dp(48), dp(48)))
        stage.addView(card, LinearLayout.LayoutParams(0, MATCH, 1f).apply { marginStart = dp(8); marginEnd = dp(8) })
        stage.addView(next, LinearLayout.LayoutParams(dp(48), dp(48)))
        body.addView(stage, LinearLayout.LayoutParams(MATCH, 0, 1f).apply { topMargin = dp(6) })

        // Actions in one row along the bottom.
        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        fun current() = friends.first { it.id == selectedId }
        actions.addView(rowButton(R.string.characters_run, primary = true) { onStartRun(current().id) },
            LinearLayout.LayoutParams(0, WRAP, 1.6f))
        actions.addView(rowButton(R.string.characters_profile_short) { showProfile(current().id) },
            LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginStart = dp(8) })
        actions.addView(rowButton(R.string.characters_story_book) {
            context.startActivity(StoryActivity.intent(context, current().id))
        }, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginStart = dp(8) })
        // The tutorial lives on the menu (hub), so the selection only has run / profile / story book.
        body.addView(actions, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        var frame = 0
        val sprites = mutableMapOf<String, CharacterSprites?>()
        fun spriteFrame(friend: CharacterDefinition): Drawable {
            val set = sprites.getOrPut(friend.id) { CharacterSprites.load(context.resources, friend.id) }
                ?: return CharacterPortrait(friend.color)
            val bitmap = set[if (frame % 2 == 0) SpriteFrame.IDLE_0 else SpriteFrame.IDLE_1]
            return BitmapDrawable(context.resources, bitmap).apply { isFilterBitmap = false; setAntiAlias(false) }
        }

        fun bind() {
            val index = friends.indexOfFirst { it.id == selectedId }
            val friend = friends[index]
            val tint = friendColor(friend)
            card.background = shape(PAPER, 24, tint)
            (card.background as GradientDrawable).setStroke(dp(3), tint)
            spot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(blend(tint, PAPER, 0.55f))
            }
            sprite.setImageDrawable(spriteFrame(friend))
            name.text = friend.name
            tag.text = context.getString(R.string.characters_species_home, friend.species, friend.home)
            tag.background = shape(blend(tint, PAPER, 0.7f), 12)
            personality.text = friend.personality
            summary.text = friend.summary
            summary.maxLines = Int.MAX_VALUE
            summary.post {
                val lines = summary.height / summary.lineHeight.coerceAtLeast(1)
                summary.visibility = if (lines < 1) View.INVISIBLE else View.VISIBLE
                summary.maxLines = lines.coerceAtLeast(1)
            }
            card.contentDescription = context.getString(R.string.characters_card_description,
                index + 1, friends.size, friend.name, friend.species, friend.home, friend.personality)
            dots.removeAllViews()
            friends.forEachIndexed { i, f ->
                dots.addView(View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (i == index) friendColor(f) else LINE)
                    }
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(if (i == index) 14 else 10), dp(if (i == index) 14 else 10)).apply { marginStart = dp(6) })
            }
        }

        fun go(step: Int) {
            val index = friends.indexOfFirst { it.id == selectedId }
            val target = (index + step + friends.size) % friends.size
            val out = if (step > 0) -dp(60).toFloat() else dp(60).toFloat()
            card.animate().translationX(out).alpha(0f).setDuration(120).withEndAction {
                selectedId = friends[target].id
                bind()
                card.translationX = -out
                card.animate().translationX(0f).alpha(1f).setDuration(160).start()
                card.announceForAccessibility(card.contentDescription)
            }.start()
        }
        prev.setOnClickListener { go(-1) }
        next.setOnClickListener { go(1) }

        // Sideways swipe on the stage moves between friends: compare where the finger went down and where it lifted.
        var downX = 0f
        var downY = 0f
        @Suppress("ClickableViewAccessibility")
        card.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; v.parent.requestDisallowInterceptTouchEvent(true) }
                android.view.MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    if (kotlin.math.abs(dx) > dp(40) && kotlin.math.abs(dx) > kotlin.math.abs(event.y - downY)) go(if (dx < 0) 1 else -1)
                }
            }
            true
        }

        // Gentle idle animation: the friend breathes between its two idle frames and bobs a little.
        val tick = object : Runnable {
            override fun run() {
                if (!sprite.isAttachedToWindow) return
                frame++
                sprite.setImageDrawable(spriteFrame(friends.first { it.id == selectedId }))
                sprite.translationY = if (frame % 2 == 0) 0f else -dp(4).toFloat()
                sprite.postDelayed(this, IDLE_MS)
            }
        }
        sprite.post { sprite.postDelayed(tick, IDLE_MS) }
        bind()
    }

    private fun friendColor(friend: CharacterDefinition): Int =
        try { Color.parseColor(friend.color) } catch (_: IllegalArgumentException) { LEAF }

    private fun blend(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) * (1 - t) + Color.red(b) * t).roundToInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).roundToInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).roundToInt())

    private fun arrowButton(symbol: String, description: Int) = Button(context).apply {
        text = symbol
        textSize = 18f
        setTextColor(LEAF)
        contentDescription = context.getString(description)
        setPadding(0, 0, 0, 0)
        background = RippleDrawable(ColorStateList.valueOf(0x33466F53), GradientDrawable().apply {
            shape = GradientDrawable.OVAL; setColor(PAPER); setStroke(dp(1), LINE)
        }, null)
        stateListAnimator = null
    }

    private fun smallButton(label: String, click: () -> Unit) = Button(context).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        setTextColor(LEAF)
        minHeight = dp(40); minimumHeight = dp(40)
        setPadding(dp(12), 0, dp(12), 0)
        background = ripple(PAPER, LINE)
        stateListAnimator = null
        setOnClickListener { click() }
    }

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
        body.action(R.string.characters_story_book) { context.startActivity(StoryActivity.intent(context, friend.id)) }
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
        const val IDLE_MS = 520L
    }
}
