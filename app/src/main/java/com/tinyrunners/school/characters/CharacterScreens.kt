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
import com.tinyrunners.school.ui.MockupFrame
import kotlin.math.roundToInt

internal class CharacterScreens(
    private val host: FrameLayout,
    private val repository: CharacterRepository,
    private val onStartRun: (characterId: String, storyId: String?) -> Unit
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
     * Friend selection in A's separated design pack (reference/screen-5): the chosen friend's home fills the screen
     * (bg_v2_friends_<id>), the friend stands on the grass at the left with a greeting in a speech bubble, and a cream
     * panel at the right holds 오늘의 길동무, the name and species, home, a short introduction, the best record and the
     * roster of six to switch friends. Below the panel: "라일이랑 달리기", 자세히 보기 and 이야기 도감. ◀ ▶ at the screen
     * edges and a sideways swipe also switch. The chosen friend is the one the shop previews decorations on.
     */
    fun showSelection() {
        if (repository.find(selectedId.orEmpty()) == null) selectedId = repository.characters.first().id
        val friends = repository.characters
        route = Route.SELECT
        host.removeAllViews()
        // The selection does not scroll up and down; this keeps save() working.
        scroll = ScrollView(context)
        val frame = MockupFrame(context, 0)
        frame.anchor = host.rootView
        frame.stretchX = true
        host.addView(frame, FrameLayout.LayoutParams(MATCH, MATCH))
        fun text(px: Int, color: Int, bold: Boolean = false) = TextView(context).apply {
            setTextColor(color); includeFontPadding = false
            if (bold) typeface = Typeface.DEFAULT_BOLD
            frame.onScaleText(this, MockupFrame.v2(px).toFloat())
        }

        // Header: back and the question.
        frame.placeV2(text(26, INK, bold = true).apply {
            text = context.getString(R.string.characters_select_title); gravity = Gravity.CENTER
            if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            // A cream pill: the picture behind is the leafy top of the home now (filled edge to edge).
            background = GradientDrawable().apply { setColor(Color.argb(235, 255, 248, 232)); cornerRadius = 999f }
        }, 24, 138, 368, 184)
        val counter = text(21, PANEL_SOFT).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL; letterSpacing = 0.2f }

        // The friend big in the middle of the open space, on a soft pool of light so they stand out, with a bubble above.
        frame.placeV2(SpotLight(context), 185, 230, 615, 640)
        val stand = FrameLayout(context)
        frame.placeV2(stand, 293, 318, 507, 532)
        val bubble = text(26, PANEL_INK).apply {
            gravity = Gravity.CENTER_VERTICAL; setLineSpacing(0f, 1.2f)
            val pad = MockupFrame.v2(18)
            setPadding(pad, 0, pad, MockupFrame.v2(12))
            background = speech
            maxLines = 3; ellipsize = android.text.TextUtils.TruncateAt.END
        }
        frame.placeV2(bubble, 165, 250, 635, 392)

        // ◀ ▶ at the screen edges.
        val prev = stageArrow("◀", R.string.characters_prev)
        val next = stageArrow("▶", R.string.characters_next)
        frame.placeV2(prev, 18, 317, 94, 393)
        frame.placeV2(next, 1186, 317, 1262, 393)

        // The panel at the right.
        frame.placeV2(ImageView(context).apply {
            setImageResource(R.drawable.ui_v2_friend_panel); scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, 724, 139, 1184, 476)
        frame.placeV2(text(21, PANEL_SOFT).apply { text = context.getString(R.string.characters_today_mate); letterSpacing = 0.1f }, 766, 166, 1000, 196)
        // 01 / 06 at the top right inside the panel.
        frame.placeV2(counter, 1000, 166, 1142, 196)
        val name = text(40, PANEL_INK, bold = true).apply { gravity = Gravity.BOTTOM; maxLines = 1 }
        frame.placeV2(name, 766, 198, 1142, 252)
        frame.placeV2(View(context).apply { setBackgroundColor(Color.argb(70, 70, 90, 60)) }, 766, 262, 1142, 263)
        val home = text(23, PANEL_SOFT).apply { maxLines = 1 }
        frame.placeV2(home, 766, 266, 1142, 300)
        val intro = text(23, PANEL_INK).apply {
            setLineSpacing(0f, 1.2f); ellipsize = android.text.TextUtils.TruncateAt.END; maxLines = 3
        }
        frame.placeV2(intro, 766, 302, 1142, 418)
        val best = text(21, LEAF, bold = true).apply { maxLines = 1 }
        frame.placeV2(best, 766, 424, 1142, 452)
        // The six friends to choose from, on a wide cream strip along the bottom of the screen: a round portrait and a
        // name each; the whole cell is the button.
        frame.placeV2(View(context).apply {
            background = shape(Color.argb(232, 255, 249, 235), 26)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, 200, 568, 1080, 676)
        val portraits = friends.map { FrameLayout(context) }
        val names = friends.map { f -> text(22, PANEL_INK).apply { text = f.name; gravity = Gravity.CENTER } }
        val cells = friends.mapIndexed { i, f ->
            val x = 228 + i * 140
            frame.placeV2(portraits[i], x + 32, 572, x + 92, 632)
            frame.placeV2(names[i], x, 634, x + 124, 668)
            View(context).apply {
                contentDescription = f.name; isClickable = true
                background = RippleDrawable(ColorStateList.valueOf(Color.argb(40, 0, 0, 0)), null, android.graphics.drawable.ColorDrawable(Color.WHITE))
                setOnClickListener { if (selectedId != f.id) { selectedId = f.id; bindSelection() } }
            }.also { frame.placeV2(it, x, 568, x + 124, 676) }
        }

        // Buttons under the panel.
        val run = MockupFrame.skinButton(context, frame, R.drawable.ui_v2_btn_primary, R.drawable.ui_v2_btn_primary_pressed,
            "", CREAM, MockupFrame.v2(27).toFloat()) { chooseStory(friends.first { it.id == selectedId }) }
        // The buttons wear the chosen friend's own colour (set in bindSelection), so they sit on the exact button rects.
        frame.placeV2(run, 724, 482, 940, 558)
        val more = MockupFrame.skinButton(context, frame, R.drawable.ui_v2_btn_secondary, R.drawable.ui_v2_btn_secondary_pressed,
            context.getString(R.string.characters_profile_short), PANEL_INK, MockupFrame.v2(24).toFloat()) {
            showProfile(selectedId.orEmpty())
        }
        frame.placeV2(more, 948, 482, 1066, 558)
        val book = MockupFrame.skinButton(context, frame, R.drawable.ui_v2_btn_secondary, R.drawable.ui_v2_btn_secondary_pressed,
            context.getString(R.string.characters_story_book), PANEL_INK, MockupFrame.v2(24).toFloat()) {
            context.startActivity(StoryActivity.intent(context, selectedId.orEmpty()))
        }
        frame.placeV2(book, 1074, 482, 1192, 558)

        var bests: Map<String, Int> = emptyMap()

        bindSelection = {
            val index = friends.indexOfFirst { it.id == selectedId }
            val friend = friends[index]
            backdrop(SELECT_BACKGROUNDS[friend.id] ?: R.drawable.bg_v2_friends_mint_wide)
            stand.removeAllViews()
            stand.addView(com.tinyrunners.school.opening.OpeningFriend(context, friend.id, 0), FrameLayout.LayoutParams(MATCH, MATCH))
            // The friend's greeting first, then another of their lines every few seconds (A's short lines).
            bubble.removeCallbacks(chatter)
            bubble.alpha = 1f
            bubble.text = greetingFor(friend)
            chatter = object : Runnable {
                var turn = 0
                override fun run() {
                    val event = if (turn++ % 2 == 0) "select" else "home"
                    val line = com.tinyrunners.school.talk.ShortLineRepository.pick(context, friend.id, event)
                    if (line != null) bubble.animate().alpha(0f).setDuration(FADE_MS).withEndAction {
                        bubble.text = line
                        bubble.animate().alpha(1f).setDuration(FADE_MS).start()
                    }.start()
                    bubble.postDelayed(this, LINE_MS)
                }
            }
            bubble.postDelayed(chatter, LINE_MS)
            counter.text = context.getString(R.string.characters_counter, index + 1, friends.size)
            val sb = SpannableStringBuilder(friend.name)
            val start = sb.length
            sb.append("  ").append(friend.species)
            sb.setSpan(android.text.style.RelativeSizeSpan(0.38f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(android.text.style.ForegroundColorSpan(PANEL_SOFT), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(StyleSpan(Typeface.NORMAL), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            name.text = sb
            home.text = friend.home
            intro.text = friend.copy?.selectionIntro ?: friend.summary
            val b = bests[friend.id]
            best.text = if (b != null && b > 0) context.getString(R.string.characters_best, b) else context.getString(R.string.characters_best_none)
            run.text = runLabel(friend)
            // Buttons in the friend's colour: filled (deepened so white words read) for the run, outlined for the rest.
            val tint = friendColor(friend)
            speech.setColors(blend(tint, Color.WHITE, 0.82f), blend(tint, Color.rgb(30, 26, 34), 0.3f))
            val deep = blend(tint, Color.rgb(30, 26, 34), 0.38f)
            val deeper = blend(tint, Color.rgb(30, 26, 34), 0.55f)
            run.background = pressable(pill(deep, deep), pill(deeper, deeper))
            run.setTextColor(Color.WHITE)
            for (b in listOf(more, book)) {
                b.background = pressable(pill(Color.rgb(255, 251, 242), deep), pill(blend(tint, Color.WHITE, 0.7f), deep))
                b.setTextColor(deeper)
            }
            for (a in listOf(prev, next)) {
                a.setTextColor(deep)
                a.background = RippleDrawable(ColorStateList.valueOf(Color.argb(60, Color.red(tint), Color.green(tint), Color.blue(tint))),
                    GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(235, 255, 251, 242)); setStroke(dp(2), deep) }, null)
            }
            name.contentDescription = context.getString(R.string.characters_card_description_short,
                friend.name, friend.species, friend.home, greetingFor(friend))
            cells.forEachIndexed { i, cell ->
                val on = friends[i].id == selectedId
                cell.isSelected = on
                // The chosen friend's portrait is ringed and a little bigger; its name bold in the friend's colour.
                portraits[i].removeAllViews()
                portraits[i].addView(RosterPortrait(context, friends[i].id, friendColor(friends[i]), on).apply {
                    isClickable = false; isFocusable = false
                    val inset = if (on) 0 else MockupFrame.v2(5)
                    layoutParams = FrameLayout.LayoutParams(MATCH, MATCH).apply { setMargins(inset, inset, inset, inset) }
                })
                names[i].setTextColor(if (on) deeper else MUTED)
                names[i].typeface = if (on) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }

        fun go(step: Int) {
            val index = friends.indexOfFirst { it.id == selectedId }
            selectedId = friends[(index + step + friends.size) % friends.size].id
            bindSelection()
            frame.announceForAccessibility(friends.first { it.id == selectedId }.name)
        }
        prev.setOnClickListener { go(-1) }
        next.setOnClickListener { go(1) }

        // A sideways swipe anywhere on the open picture switches friends.
        var downX = 0f
        var downY = 0f
        @Suppress("ClickableViewAccessibility")
        frame.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y }
                android.view.MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    if (kotlin.math.abs(dx) > dp(40) && kotlin.math.abs(dx) > kotlin.math.abs(event.y - downY)) go(if (dx < 0) 1 else -1)
                }
            }
            true
        }
        bindSelection()
        // Best distance per friend for the panel (cloud or this device).
        com.tinyrunners.school.progress.ProgressStore.forCurrentUser(context).loadAll { result ->
            if (route != Route.SELECT) return@loadAll
            bests = result.getOrNull().orEmpty().mapValues { it.value.bestMeters }
            bindSelection()
        }
    }

    private var bindSelection: () -> Unit = {}

    /** The rounded speech bubble above the friend (its colours follow the friend). */
    private val speech = SpeechBubble()

    /**
     * Before a run: the friend's stories as a little book, one page per episode (StoryPicker, an AlertDialog with a
     * custom view). Read ones are ticked, the next one is marked, later ones stay locked until the ones before are
     * read. Picking one starts the run with that story.
     */
    private fun chooseStory(friend: CharacterDefinition) {
        val stories = com.tinyrunners.school.story.StoryRepository.load(context).filter { it.characterId == friend.id }
        if (stories.isEmpty()) { onStartRun(friend.id, null); return }
        com.tinyrunners.school.progress.ProgressStore.forCurrentUser(context).loadAll { result ->
            val read = result.getOrNull().orEmpty().values.flatMap { it.readStoryIds }.toSet()
            StoryPicker(context, friend.name, stories, read, friendColor(friend)) { story -> onStartRun(friend.id, story.id) }.show()
        }
    }

    /** A round-ended button face: [fill] with a [edge] line. */
    private fun pill(fill: Int, edge: Int) = GradientDrawable().apply { setColor(fill); cornerRadius = 999f; setStroke(dp(2), edge) }

    private fun pressable(normal: Drawable, pressed: Drawable) = android.graphics.drawable.StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), pressed)
        addState(intArrayOf(), normal)
    }

    /** Changes the friend's line in the selection bubble now and then; replaced when another friend is chosen. */
    private var chatter: Runnable = Runnable { }

    /** The full-screen picture behind the character screens: a friend's home on the selection, the shared paper otherwise. */
    private fun backdrop(res: Int?) {
        val view = host.rootView.findViewById<FrameLayout>(R.id.screen_backdrop) ?: return
        view.removeAllViews()
        if (res == null) { view.visibility = View.GONE; return }
        // Only the original picture (the widened file's middle 80 %), filling the screen: no blurred side bands. A light
        // dim keeps it behind the friend and the panel.
        view.addView(com.tinyrunners.school.ui.CoverPicture(context, res, sideInset = 0.1f), FrameLayout.LayoutParams(MATCH, MATCH))
        view.addView(View(context).apply { setBackgroundColor(Color.argb(70, 28, 22, 34)) }, FrameLayout.LayoutParams(MATCH, MATCH))
        view.visibility = View.VISIBLE
    }

    /** A soft, round pool of warm light (strongest at the bottom middle, where the friend stands), fading to nothing. */
    private class SpotLight(context: android.content.Context) : View(context) {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            // An oval that fades out inside the view on every side (no straight edge where the view ends).
            val cx = w / 2f; val cy = h * 0.6f
            val rx = w / 2f; val ry = minOf(cy, h - cy)
            paint.shader = android.graphics.RadialGradient(cx, cy, rx,
                intArrayOf(Color.argb(200, 255, 246, 222), Color.argb(110, 255, 240, 210), Color.argb(0, 255, 240, 210)),
                floatArrayOf(0f, 0.55f, 1f), android.graphics.Shader.TileMode.CLAMP).apply {
                setLocalMatrix(android.graphics.Matrix().apply { setScale(1f, ry / rx, cx, cy) })
            }
        }
        override fun onDraw(canvas: android.graphics.Canvas) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    /** A rounded speech bubble with a small tail at the bottom, toward the friend below; [setColors] tints it. */
    private class SpeechBubble : Drawable() {
        private val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 250, 234) }
        private val edge = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(176, 158, 118); style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f
        }
        private val shape = android.graphics.Path()
        private val box = android.graphics.RectF()
        private val tail = android.graphics.Path()

        fun setColors(fillColor: Int, edgeColor: Int) { fill.color = fillColor; edge.color = edgeColor; invalidateSelf() }

        override fun draw(canvas: android.graphics.Canvas) {
            val r = bounds
            val t = r.height() * 0.14f
            val bottom = r.bottom - t - 3f
            val cx = r.left + r.width() * 0.5f
            box.set(r.left + 3f, r.top + 3f, r.right - 3f, bottom)
            val radius = minOf(box.height() * 0.42f, box.width() * 0.2f)
            // One outline: the rounded box joined with the tail, filled then stroked.
            shape.reset()
            shape.addRoundRect(box, radius, radius, android.graphics.Path.Direction.CW)
            tail.reset()
            tail.moveTo(cx - t, bottom - 2f); tail.lineTo(cx, r.bottom - 3f); tail.lineTo(cx + t, bottom - 2f); tail.close()
            shape.op(tail, android.graphics.Path.Op.UNION)
            canvas.drawPath(shape, fill)
            canvas.drawPath(shape, edge)
        }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
    }

    private fun stageArrow(symbol: String, description: Int) = Button(context).apply {
        text = symbol; textSize = 18f; setTextColor(com.tinyrunners.school.ui.CardShelf.GREEN)
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

    /**
     * The friend's profile (자세히 보기), built like the run result's score board so the two read alike: over the
     * friend's own road, a storybook page with a 친구 소개 ribbon, the friend's portrait, name, species · home and a
     * word in a bubble, number tiles for this friend (best distance, distance together, runs) that count up once
     * loaded, their traits as small pills, and A's profile copy in one stitched box. On the right, the story-collection
     * buttons: run with this friend, the story book, back to the selection.
     */
    private fun showProfile(id: String, scrollY: Int = 0) {
        val friend = repository.find(id) ?: run {
            selectedId = null
            showSelection()
            return
        }
        selectedId = friend.id
        route = Route.PROFILE
        host.removeAllViews()
        // The friend's home from the selection stays behind the profile (A's separated pack).
        backdrop(SELECT_BACKGROUNDS[friend.id] ?: R.drawable.bg_v2_friends_mint_wide)
        val board = com.tinyrunners.school.ui.ScoreBoard
        val copy = friend.copy

        val (pageWrap, page) = board.page(context, context.getString(R.string.characters_profile_ribbon))
        val head = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(board.portrait(context, friend.id, friendColor(friend)), LinearLayout.LayoutParams(dp(62), dp(62)))
        val names = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        names.addView(TextView(context).apply {
            text = context.getString(R.string.characters_species_home, friend.species, friend.home)
            textSize = 12f; setTextColor(board.SOFT); setTypeface(typeface, Typeface.BOLD); maxLines = 1
        })
        names.addView(TextView(context).apply {
            text = friend.name; textSize = 24f; setTextColor(board.INK); setTypeface(typeface, Typeface.BOLD)
            if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        })
        head.addView(names)
        head.addView(board.bubble(context, "“" + greetingFor(friend) + "”"),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) })
        page.addView(head)

        // This friend's numbers, counting up once the records are read.
        val tiles = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; isBaselineAligned = false }
        val (bestTile, bestValue) = board.tile(context, context.getString(R.string.characters_stat_best), "–")
        val (totalTile, totalValue) = board.tile(context, context.getString(R.string.characters_stat_total), "–")
        val (runsTile, runsValue) = board.tile(context, context.getString(R.string.characters_stat_runs), "–")
        listOf(bestTile, totalTile, runsTile).forEachIndexed { i, t ->
            tiles.addView(t, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { if (i > 0) marginStart = dp(10) })
        }
        page.addView(tiles, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })
        com.tinyrunners.school.progress.ProgressStore.forCurrentUser(context).loadAll { result ->
            if (route != Route.PROFILE || selectedId != friend.id) return@loadAll
            val p = result.getOrNull()?.get(friend.id)
            board.countUp(bestValue, p?.bestMeters ?: 0, { context.getString(R.string.result_meters, it) })
            board.countUp(totalValue, (p?.totalMeters ?: 0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), { context.getString(R.string.result_meters, it) }, 120)
            board.countUp(runsValue, p?.runs ?: 0, { context.getString(R.string.characters_runs_value, it) }, 240)
        }

        // Traits as small pills.
        if (copy != null && copy.traits.isNotEmpty()) {
            val traits = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            copy.traits.forEach { trait ->
                traits.addView(board.pill(context, null, trait, board.GREEN_SOFT),
                    LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = dp(6) })
            }
            page.addView(android.widget.HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; addView(traits) },
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })
        }

        // A's profile copy in one stitched box: short headings and their lines.
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(10))
            background = GradientDrawable().apply {
                setColor(board.TILE); cornerRadius = dp(16).toFloat()
                setStroke(dp(2), board.HONEY, dp(6).toFloat(), dp(4).toFloat())
            }
        }
        fun entry(title: Int, value: String) {
            box.addView(TextView(context).apply {
                text = context.getString(title); textSize = 13f; setTextColor(board.GREEN); setTypeface(typeface, Typeface.BOLD)
                setPadding(0, if (box.childCount > 0) dp(8) else 0, 0, 0)
            })
            box.addView(TextView(context).apply { text = value; textSize = 15f; setTextColor(board.INK); setLineSpacing(0f, 1.15f) })
        }
        if (copy != null) {
            entry(R.string.characters_copy_personality, copy.tagline + "\n" + copy.personality)
            entry(R.string.characters_copy_habits, copy.habits.joinToString("\n") { "· " + it })
            entry(R.string.characters_copy_cherished, copy.cherishedItem)
            entry(R.string.characters_copy_running, copy.runningReason)
            entry(R.string.characters_copy_story, copy.storyTeaser)
        } else {
            entry(R.string.characters_personality, friend.personality)
            entry(R.string.characters_story_outline, friend.summary)
        }
        page.addView(box, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })
        page.addView(TextView(context).apply {
            setText(R.string.characters_copy_mechanics); textSize = 12f; setTextColor(board.SOFT); setPadding(dp(4), dp(8), 0, 0)
        })

        // Right: what to do with this friend.
        // The buttons stay at the top beside the page, which can be taller than the screen and scrolls.
        val right = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(34), 0, 0) }
        fun add(text: String, primary: Boolean, click: () -> Unit) = right.addView(board.button(context, text, primary, click),
            LinearLayout.LayoutParams(MATCH, dp(46)).apply { if (right.childCount > 0) topMargin = dp(8) })
        add(runLabel(friend), true) { chooseStory(friend) }
        add(context.getString(R.string.characters_story_book), false) { context.startActivity(StoryActivity.intent(context, friend.id)) }
        // A square picture card introducing this friend, for other apps.
        add(context.getString(R.string.share_friend_button), false) {
            try {
                val greeting = greetingFor(friend)
                val card = com.tinyrunners.school.share.ShareCard.friendCard(context, friend, greeting)
                com.tinyrunners.school.share.ShareCard.share(context, card, "tiny_runners_" + friend.id + ".png",
                    context.getString(R.string.share_friend_text, com.tinyrunners.school.talk.Josa.with(friend.name, "을", "를"), "“$greeting”"))
            } catch (e: Exception) {
                android.util.Log.w("CharacterScreens", "Share card failed", e)
                android.widget.Toast.makeText(context, R.string.share_failed, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        add(context.getString(R.string.result_choose_friend), false) { showSelection() }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP
            setPadding(dp(24), dp(6), dp(24), dp(16))
        }
        row.addView(pageWrap, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(right, LinearLayout.LayoutParams(dp(250), WRAP).apply { marginStart = dp(22) })
        scroll = com.tinyrunners.school.ui.TunedScrollView(context).apply { isFillViewport = true; addView(row) }
        host.addView(scroll, FrameLayout.LayoutParams(MATCH, MATCH))
        restoreScroll(scrollY)
    }

    private fun screen(next: Route): LinearLayout {
        route = next
        host.removeAllViews()
        backdrop(null)
        scroll = com.tinyrunners.school.ui.TunedScrollView(context).apply { isFillViewport = true }
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
        /** Colours of the separated pack panels. */
        val PANEL_INK = Color.rgb(58, 62, 48)
        /** How long one line stays in the selection bubble, and its fade. */
        const val LINE_MS = 6000L
        const val FADE_MS = 220L
        val PANEL_SOFT = Color.rgb(120, 110, 88)
        val CREAM = Color.rgb(255, 248, 231)
        /** Each friend home behind the selection (A separated pack, widened). */
        val SELECT_BACKGROUNDS = mapOf(
            "mint" to R.drawable.bg_v2_friends_mint_wide, "cheese" to R.drawable.bg_v2_friends_cheese_wide,
            "popo" to R.drawable.bg_v2_friends_popo_wide, "lumi" to R.drawable.bg_v2_friends_lumi_wide,
            "nunu" to R.drawable.bg_v2_friends_nunu_wide, "piko" to R.drawable.bg_v2_friends_piko_wide)
    }
}
