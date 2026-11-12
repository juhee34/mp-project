package com.tinyrunners.school.talk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.game.StoryBackground
import com.tinyrunners.school.ui.ScenePainter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * RPG-style chat with the chosen friend before the run: the first frame of the friend's running background stands
 * still, the friend stands large in the middle, and a dialogue box at the bottom advances with each tap.
 * It runs before every run (long talk the first time, short lines afterwards), before "한 번 더 달리기", and right
 * after a crash. When the talk ends (or "건너뛰기" is pressed) the next screen opens.
 */
class TalkActivity : Activity(), com.tinyrunners.school.audio.MusicScreen {
    /** Song for the scene being told (stories only); run talks keep the song chosen in the shop. */
    private var sceneTrack: Int? = null
    /** Song for each line of the lines on screen (stories only). */
    private var lineTracks: List<Int> = emptyList()

    override fun musicTrack(): Int? = sceneTrack

    private val isStory: Boolean get() = intent.getIntExtra(EXTRA_MODE, 0).let { it == TalkMode.REPLAY.ordinal || it == TalkMode.VILLAGE.ordinal }

    private var view: TalkView? = null
    private var done = false
    private var root: FrameLayout? = null
    private var skip: View? = null
    private var names: Map<String, String> = emptyMap()
    /** VILLAGE only: the talk split around handing over the friend's items. */
    private var village: VillageParts? = null
    private var handedOver = false
    private var firstTime = false
    /** VILLAGE, after the hand-over: position in [VillageParts.after] and a choice waiting for the player. */
    private var stepAt = 0
    private var pendingChoice: com.tinyrunners.school.story.Step.Choice? = null
    private var choicePanel: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val characterId = intent.getStringExtra(GameActivity.EXTRA_CHARACTER_ID).orEmpty()
        val character = CharacterRepository(this).find(characterId)
        if (character == null) { finish(); return }
        val mode = TalkMode.entries.getOrNull(intent.getIntExtra(EXTRA_MODE, 0)) ?: TalkMode.START
        firstTime = mode == TalkMode.START && TalkProgress(this).shouldShowTalk(character.id)
        val meters = intent.getIntExtra(EXTRA_METERS, 0)
        handedOver = savedInstanceState?.getBoolean(KEY_GIVEN) == true
        if (mode == TalkMode.VILLAGE) {
            val item = com.tinyrunners.school.quest.QuestItems.of(character.id)
                ?.takeIf { intent.getBooleanExtra(EXTRA_ASK, false) }
            val story = villageStory()
            // Dialogue HANDOFF: story_unread opens an unread written story, story_reread one seen again.
            val greeting = when {
                story?.scripted != true -> "select"
                intent.getBooleanExtra(EXTRA_ASK, false) -> "story_unread"
                else -> "story_reread"
            }
            village = TalkPlan.village(character.id, item?.let { getString(it.request) }, item?.let { getString(it.thanks) },
                story?.steps.orEmpty(), greeting) { event -> ShortLineRepository.pick(this, character.id, event) }
        }
        // A restored visit that was past the hand-over starts the story again from its beginning.
        val lines = village?.let { if (handedOver) TalkPlan.segment(it.after, 0).first else it.before } ?: TalkPlan.lines(
            mode, character.id, firstTime,
            fullTalk = TalkRepository.load(this)?.talkFor(character.id).orEmpty(),
            narration = if (mode == TalkMode.STUMBLE) stumbleNarration(character.id, meters) else null,
            storyOutline = if (mode == TalkMode.VILLAGE) villageStory()?.summary.orEmpty() else "",
        ) { event -> ShortLineRepository.pick(this, character.id, event) }
        if (lines.isEmpty()) { finishTalk(character.id, firstTime); return }

        // Every friend can speak in a village story, so all names are known.
        names = CharacterRepository(this).characters.associate { it.id to it.name } +
            (TalkScript.PLAYER to (AuthService.currentName(this) ?: getString(R.string.talk_player_default)))
        com.tinyrunners.school.audio.Sfx.init(this)
        // Start on the first scene's song right away, so the music does not change twice as the story opens.
        if (isStory) sceneTrack = com.tinyrunners.school.audio.Music.friendTrack(this,
            TalkMusic.sceneLeaders(lines, character.id).firstOrNull() ?: character.id)
        val skip = TextView(this).apply {
            setText(R.string.talk_skip)
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            minHeight = (48 * resources.displayMetrics.density).roundToInt()
            gravity = Gravity.CENTER
            val pad = (16 * resources.displayMetrics.density).roundToInt()
            setPadding(pad, 0, pad, 0)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(150, 38, 52, 43))
                cornerRadius = 24 * resources.displayMetrics.density
            }
            setOnClickListener {
                // In a village story, skipping ends the story (it still counts as seen); the request is never skipped.
                if (village != null && handedOver) { stepAt = Int.MAX_VALUE; pendingChoice = null }
                finishTalk(character.id, firstTime)
            }
        }
        this.skip = skip
        val margin = (12 * resources.displayMetrics.density).roundToInt()
        val frame = FrameLayout(this).apply {
            addView(skip, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END).apply { setMargins(0, margin, margin, 0) })
        }
        root = frame
        setContentView(frame)
        if (handedOver) stepAt = TalkPlan.segment(village?.after.orEmpty(), 0).second
        showLines(character.id, lines, savedInstanceState?.getInt(KEY_INDEX) ?: 0)
    }

    /** Puts a talk scene for [lines] behind the skip button, replacing any earlier one. */
    private fun showLines(characterId: String, lines: List<TalkLine>, at: Int = 0) {
        val frame = root ?: return
        val guest = view?.guest
        view?.let { frame.removeView(it) }
        val player = TalkPlayer(lines)
        if (at > 0) player.restore(at)
        // After a crash the scene shows the spot where the friend fell, with the friend in their tumble pose.
        // Stories: each scene plays the song of the friend who leads it (speaks most), changing at scene cards.
        lineTracks = if (isStory) TalkMusic.sceneLeaders(lines, characterId).map { com.tinyrunners.school.audio.Music.friendTrack(this, it) } else emptyList()
        val talk = TalkView(this, characterId, player, names, guest = guest,
            distance = intent.getFloatExtra(EXTRA_DISTANCE, 0f),
            fallen = intent.getIntExtra(EXTRA_MODE, 0) == TalkMode.STUMBLE.ordinal) { finishTalk(characterId, firstTime) }
        talk.onLine = { i ->
            lineTracks.getOrNull(i)?.takeIf { it != sceneTrack }?.let { track ->
                sceneTrack = track
                com.tinyrunners.school.audio.Music.play(this, track)
            }
        }
        view = talk
        frame.addView(talk, 0, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    /**
     * The friend asked for their items: shows how many the bag holds and lets the player hand them over, or tells
     * them where to find more. Handing over continues the talk with the friend's thanks.
     */
    private fun showGate(characterId: String) {
        val frame = root ?: return
        val item = com.tinyrunners.school.quest.QuestItems.of(characterId) ?: return handOver(characterId)
        val storyId = intent.getStringExtra(EXTRA_STORY) ?: return handOver(characterId)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).roundToInt()
        skip?.visibility = View.GONE
        val itemName = getString(item.name)
        val friendName = names[characterId] ?: characterId
        val store = com.tinyrunners.school.quest.ItemBagStore.forCurrentUser(this)

        com.tinyrunners.school.audio.Sfx.play(this, com.tinyrunners.school.audio.Sfx.Sound.CHOICE)
        val panel = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(18), dp(24), dp(18))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(245, 255, 253, 246)); cornerRadius = 18 * d; setStroke(dp(3), GATE_LEAF)
            }
            isClickable = true
        }
        val top = android.widget.LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(ItemIconView(this, item), android.widget.LinearLayout.LayoutParams(dp(44), dp(44)))
        val count = TextView(this).apply {
            setTextColor(GATE_INK); textSize = 19f; typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12), 0, 0, 0); setText(R.string.quest_loading)
        }
        top.addView(count)
        panel.addView(top)
        val hint = TextView(this).apply {
            setTextColor(GATE_INK); textSize = 15f; gravity = Gravity.CENTER; visibility = View.GONE
            setPadding(0, dp(8), 0, 0)
        }
        panel.addView(hint)
        val buttons = android.widget.LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, dp(12), 0, 0) }
        val main = gateButton(filled = true).apply { visibility = View.GONE }
        val later = gateButton(filled = false).apply { setText(R.string.quest_later); setOnClickListener { finish() } }
        buttons.addView(main, android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)).apply { marginEnd = dp(10) })
        buttons.addView(later, android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)))
        panel.addView(buttons)
        frame.addView(panel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(16) })

        fun show(bag: com.tinyrunners.school.quest.ItemBag) {
            if (storyId in bag.given) { frame.removeView(panel); handOver(characterId); return }
            val have = bag.count(characterId)
            val needed = com.tinyrunners.school.quest.QuestRules.NEEDED
            count.text = getString(R.string.quest_have, itemName, have, needed)
            main.visibility = View.VISIBLE
            main.isEnabled = true
            if (have >= needed) {
                hint.visibility = View.GONE
                main.text = getString(R.string.quest_give, itemName)
                main.setOnClickListener {
                    main.isEnabled = false
                    store.give(characterId, storyId) { result ->
                        if (isDestroyed) return@give
                        val next = result.getOrNull()
                        when {
                            result.isFailure -> { count.setText(R.string.quest_error); main.isEnabled = true }
                            next == null -> store.load { r -> if (!isDestroyed) r.onSuccess { show(it) } }
                            else -> {
                                com.tinyrunners.school.audio.Sfx.play(this, com.tinyrunners.school.audio.Sfx.Sound.GIVE, volume = 0.7f)
                                frame.removeView(panel); handOver(characterId)
                            }
                        }
                    }
                }
            } else {
                hint.visibility = View.VISIBLE
                hint.text = getString(R.string.quest_short, Josa.with(friendName, "과", "와"), Josa.with(itemName, "을", "를"))
                main.setText(R.string.quest_run)
                main.setOnClickListener {
                    startActivity(GameActivity.intent(this, characterId))
                    finish()
                }
            }
        }
        store.load { result ->
            if (isDestroyed) return@load
            result.onSuccess { show(it) }.onFailure { count.setText(R.string.quest_error) }
        }
    }

    private fun gateButton(filled: Boolean) = TextView(this).apply {
        val d = resources.displayMetrics.density
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding((18 * d).roundToInt(), 0, (18 * d).roundToInt(), 0)
        setTextColor(if (filled) Color.WHITE else GATE_LEAF)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(if (filled) GATE_LEAF else Color.WHITE); cornerRadius = 22 * d; setStroke((2 * d).roundToInt(), GATE_LEAF)
        }
    }

    /** Items given (or not needed after all): the friend says thanks and the story is told. */
    private fun handOver(characterId: String) {
        handedOver = true
        stepAt = 0
        skip?.visibility = View.VISIBLE
        playStory(characterId, emptyList())
    }

    /** Plays [lead] and the story lines up to the next choice; a choice then waits as buttons (see [finishTalk]). */
    private fun playStory(characterId: String, lead: List<TalkLine>) {
        val after = village?.after.orEmpty()
        val (lines, next) = TalkPlan.segment(after, stepAt.coerceAtMost(after.size))
        stepAt = next
        pendingChoice = after.getOrNull(next) as? com.tinyrunners.school.story.Step.Choice
        done = false
        val all = lead + lines
        if (all.isEmpty()) finishTalk(characterId, firstTime) else showLines(characterId, all)
    }

    /** A story choice: two or three answers; each only changes the friend's next reply. */
    private fun showChoice(characterId: String, choice: com.tinyrunners.school.story.Step.Choice) {
        val frame = root ?: return
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).roundToInt()
        skip?.visibility = View.GONE
        com.tinyrunners.school.audio.Sfx.play(this, com.tinyrunners.school.audio.Sfx.Sound.CHOICE)
        val panel = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(235, 255, 253, 246)); cornerRadius = 18 * d; setStroke(dp(3), GATE_LEAF)
            }
            isClickable = true
        }
        choice.options.forEachIndexed { i, option ->
            panel.addView(gateButton(filled = i == 0).apply {
                text = option.text
                setOnClickListener {
                    frame.removeView(panel)
                    choicePanel = null
                    skip?.visibility = View.VISIBLE
                    stepAt++
                    playStory(characterId, option.reply.map { TalkLine(it.speaker, it.text) })
                }
            }, android.widget.LinearLayout.LayoutParams(dp(320), dp(44)).apply { if (i > 0) topMargin = dp(10) })
        }
        choicePanel = panel
        frame.addView(panel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(16) })
    }

    /** Talk finished or skipped: go on to the run (start/retry) or to the result screen (after a crash). */
    private fun finishTalk(characterId: String, firstTime: Boolean) {
        if (done) return
        done = true
        if (intent.getIntExtra(EXTRA_MODE, 0) == TalkMode.REPLAY.ordinal) { finish(); return }
        if (intent.getIntExtra(EXTRA_MODE, 0) == TalkMode.VILLAGE.ordinal) {
            // The friend's request was heard (or skipped): the items are asked for before the story goes on.
            if (!handedOver) { if (village?.askItems == true) showGate(characterId) else handOver(characterId); return }
            pendingChoice?.let { if (choicePanel == null) showChoice(characterId, it); return }
            // Heard to the end (or skipped): the village story counts as seen.
            villageStory()?.let { com.tinyrunners.school.progress.ProgressStore.forCurrentUser(this).markStoryRead(characterId, it.id) { } }
            finish(); return
        }
        // The long first talk is shown once per friend; later runs get short lines instead.
        if (firstTime) TalkProgress(this).markTalkSeen(characterId)
        @Suppress("DEPRECATION")
        val next = intent.getParcelableExtra<Intent>(EXTRA_NEXT) ?: GameActivity.intent(this, characterId)
        startActivity(next)
        finish()
    }

    private fun stumbleNarration(characterId: String, meters: Int): String {
        val items = intent.getIntExtra(EXTRA_ITEMS, 0)
        val item = com.tinyrunners.school.quest.QuestItems.of(characterId)
        return if (items > 0 && item != null) getString(R.string.talk_stumble_items, meters, getString(item.name), items)
            else getString(R.string.talk_stumble_narration, meters)
    }

    private fun villageStory() = intent.getStringExtra(EXTRA_STORY)?.let { id ->
        com.tinyrunners.school.story.StoryRepository.load(this).firstOrNull { it.id == id }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        view?.let { outState.putInt(KEY_INDEX, it.player.index) }
        outState.putBoolean(KEY_GIVEN, handedOver)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_INDEX = "talkIndex"
        private const val KEY_GIVEN = "talkGiven"
        private const val EXTRA_ASK = "talkAskItems"
        private val GATE_LEAF = Color.rgb(48, 83, 65)
        private val GATE_INK = Color.rgb(38, 52, 43)
        private const val EXTRA_MODE = "talkMode"
        private const val EXTRA_NEXT = "talkNext"
        private const val EXTRA_METERS = "talkMeters"
        private const val EXTRA_DISTANCE = "talkDistance"
        private const val EXTRA_STORY = "talkStory"
        private const val EXTRA_ITEMS = "talkItems"

        /** Before a run chosen on the selection screen. */
        fun intent(context: Context, characterId: String) =
            Intent(context, TalkActivity::class.java).putExtra(GameActivity.EXTRA_CHARACTER_ID, characterId)

        /**
         * A village visit as an RPG-style talk on the friend's own background. With [askItems] the friend first asks
         * for their items and the story is only told once they are handed over. Closes at the end and marks it seen.
         */
        fun village(context: Context, characterId: String, storyId: String, askItems: Boolean = false) =
            intent(context, characterId)
            .putExtra(EXTRA_MODE, TalkMode.VILLAGE.ordinal).putExtra(EXTRA_STORY, storyId)
            .putExtra(EXTRA_ASK, askItems)

        /** A friend's long first talk again, from 이야기 모음; closes at the end without a run. */
        fun replay(context: Context, characterId: String) = intent(context, characterId).putExtra(EXTRA_MODE, TalkMode.REPLAY.ordinal)

        /** Before "한 번 더 달리기"; [next] is the run to start afterwards. */
        fun retry(context: Context, characterId: String, next: Intent) = intent(context, characterId)
            .putExtra(EXTRA_MODE, TalkMode.RETRY.ordinal).putExtra(EXTRA_NEXT, next)

        /** Right after a crash; [result] is the result screen shown afterwards. */
        fun stumble(context: Context, characterId: String, meters: Int, distance: Float, result: Intent, items: Int = 0) =
            intent(context, characterId)
            .putExtra(EXTRA_MODE, TalkMode.STUMBLE.ordinal).putExtra(EXTRA_NEXT, result)
            .putExtra(EXTRA_METERS, meters).putExtra(EXTRA_DISTANCE, distance).putExtra(EXTRA_ITEMS, items)
    }
}

@SuppressLint("ViewConstructor")
internal class TalkView(
    context: Context,
    characterId: String,
    val player: TalkPlayer,
    private val names: Map<String, String>,
    private val distance: Float = 0f,
    private val fallen: Boolean = false,
    /** Another friend already in the scene (kept when the scene is rebuilt between story parts). */
    var guest: String? = null,
    private val onFinished: () -> Unit,
) : View(context) {
    private val mainId = characterId
    /** Called when a new line starts showing (its index). */
    var onLine: ((Int) -> Unit)? = null
    private val guestSprites = mutableMapOf<String, CharacterSprites?>()
    private val dim = Paint().apply {
        isFilterBitmap = false; isAntiAlias = false
        // Darker, not see-through: the friend who is listening steps back a little.
        colorFilter = android.graphics.LightingColorFilter(0xFF9C9C9C.toInt(), 0)
    }
    private val scenery = StoryBackground.load(context, characterId)
    private val fallback = ScenePainter()
    private val sprites = CharacterSprites.load(resources, characterId)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = dpF(19f); color = INK }
    private val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dpF(16f); color = Color.WHITE; typeface = Typeface.DEFAULT_BOLD
    }
    /** A soft tint of a speaker's colour for the inside of their dialogue box. */
    private fun tint(c: Int) = Color.argb(242, (255 * 0.82f + Color.red(c) * 0.18f).toInt(), (253 * 0.82f + Color.green(c) * 0.18f).toInt(), (246 * 0.82f + Color.blue(c) * 0.18f).toInt())
    /** Typing speed chosen in settings (느리게 / 보통 / 빠르게). */
    private val charMs = com.tinyrunners.school.audio.AudioSettings(context).talkSpeed.charMs
    private val box = RectF()
    private val tag = RectF()
    private val sprite = RectF()
    private var lastTick = SystemClock.uptimeMillis()
    private var typed = 0f
    private var announced = -1
    private var layout: StaticLayout? = null
    private var layoutFor = -1 to -1
    private var lastLine: TalkLine? = null

    init {
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) { performClick(); return true }
        return event.actionMasked == MotionEvent.ACTION_DOWN
    }

    override fun performClick(): Boolean {
        super.performClick()
        player.tap()
        if (player.isFinished) onFinished() else invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        // Same framing as the runner (RunnerView): the background is its first frame, distance 0, and does not move.
        val scale = min(h / WORLD_HEIGHT, w / MIN_WORLD_WIDTH)
        val groundY = h - GROUND_THICKNESS * scale
        if (scenery != null) scenery.draw(canvas, w, h, groundY, distance, scale)
        else fallback.draw(canvas, width, height, groundY, h * 0.17f)

        // Dialogue box along the bottom.
        val margin = dpF(16f)
        val boxHeight = max(h * 0.28f, dpF(110f))
        box.set(margin, h - margin - boxHeight, w - margin, h - margin)

        // The friend, large and centred, standing just above the box; idle frames alternate slowly. When another
        // friend speaks in a story they step in on the right, facing the friend, and stay for the rest of the scene.
        val speaking = (player.current ?: lastLine)?.speaker
        if (speaking != null && speaking != mainId && names.containsKey(speaking) && speaking != TalkScript.PLAYER) guest = speaking
        val frame = if (fallen) SpriteFrame.FAIL_0
            else if ((SystemClock.uptimeMillis() / IDLE_FRAME_MS) % 2 == 0L) SpriteFrame.IDLE_0 else SpriteFrame.IDLE_1
        val px = max(1, ((box.top - dpF(24f)) * 0.62f / CharacterSprites.SIZE).roundToInt()).toFloat()
        val feet = box.top - dpF(6f)
        val other = guest
        sprites?.let {
            val paint = if (other != null && speaking == other) dim else pixel
            drawFriend(canvas, it[frame], if (other == null) w / 2f else w * 0.34f, feet, px, paint, flip = false)
        }
        if (other != null) {
            val art = guestSprites.getOrPut(other) { CharacterSprites.load(resources, other) }
            art?.let { drawFriend(canvas, it[frame], w * 0.66f, feet, px, if (speaking == mainId) dim else pixel, flip = true) }
        }

        // Once the last line is read it stays in the box (the hand-over panel opens over the scene meanwhile).
        val line = player.current ?: lastLine ?: return
        lastLine = line
        val narrator = line.speaker == TalkScript.NARRATOR
        // A friend's line is framed in their colour; the player keeps the warm brown, narration the dark box.
        val accent = SPEAKER_COLORS[line.speaker] ?: LEAF
        fill.color = if (narrator) NARRATOR_BOX else tint(accent)
        canvas.drawRoundRect(box, dpF(14f), dpF(14f), fill)
        fill.style = Paint.Style.STROKE; fill.strokeWidth = dpF(3f); fill.color = accent
        canvas.drawRoundRect(box, dpF(14f), dpF(14f), fill)
        fill.style = Paint.Style.FILL

        if (!narrator) {
            val name = names[line.speaker] ?: line.speaker
            val tagW = namePaint.measureText(name) + dpF(28f)
            tag.set(box.left + dpF(20f), box.top - dpF(18f), box.left + dpF(20f) + tagW, box.top + dpF(18f))
            fill.color = accent
            canvas.drawRoundRect(tag, dpF(18f), dpF(18f), fill)
            canvas.drawText(name, tag.left + dpF(14f), tag.centerY() - (namePaint.ascent() + namePaint.descent()) / 2f, namePaint)
        }

        // Typewriter: about 30 characters a second; a tap shows the whole line at once.
        val now = SystemClock.uptimeMillis()
        if (player.isTyping) {
            typed += (now - lastTick) / charMs
            val whole = typed.toInt()
            if (whole > 0) { player.type(whole); typed -= whole }
        } else typed = 0f
        lastTick = now

        val textLeft = box.left + dpF(24f)
        val textWidth = (box.width() - dpF(48f)).toInt()
        textPaint.color = if (narrator) NARRATOR_TEXT else INK
        textPaint.typeface = if (narrator) Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) else Typeface.DEFAULT
        val shownChars = if (player.current == null) line.text.length else player.shown
        val key = player.index to shownChars
        if (layoutFor != key || layout == null) {
            layout = StaticLayout.Builder.obtain(line.text, 0, shownChars, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.25f).build()
            layoutFor = key
        }
        canvas.save()
        canvas.translate(textLeft, box.top + dpF(if (narrator) 20f else 26f))
        layout?.draw(canvas)
        canvas.restore()

        if (!player.isTyping && (now / BLINK_MS) % 2 == 0L) {
            // ▼ "tap for more" marker in the box corner.
            val cx = box.right - dpF(26f)
            val cy = box.bottom - dpF(20f)
            val s = dpF(7f)
            val path = android.graphics.Path().apply { moveTo(cx - s, cy - s * 0.6f); lineTo(cx + s, cy - s * 0.6f); lineTo(cx, cy + s * 0.6f); close() }
            fill.color = accent
            canvas.drawPath(path, fill)
        }

        if (announced != player.index) {
            announced = player.index
            onLine?.invoke(player.index)
            val who = if (narrator) "" else (names[line.speaker] ?: line.speaker) + ". "
            contentDescription = who + line.text
            announceForAccessibility(contentDescription)
        }
        // Every frame only while letters are typing; otherwise a few times a second is enough for the idle pose
        // and the blinking marker (less work for the phone, and for a software-drawn emulator whose sound crackles
        // when it is busy).
        if (player.isTyping) postInvalidateOnAnimation() else postInvalidateDelayed(IDLE_REDRAW_MS)
    }

    private fun drawFriend(canvas: Canvas, bitmap: android.graphics.Bitmap?, centerX: Float, feet: Float, px: Float, paint: Paint, flip: Boolean) {
        bitmap ?: return
        val size = CharacterSprites.SIZE * px
        val left = (centerX - CharacterSprites.CENTER_X * px).roundToInt().toFloat()
        val top = (feet - CharacterSprites.FOOT_EDGE_Y * px).roundToInt().toFloat()
        sprite.set(left, top, left + size, top + size)
        if (!flip) { canvas.drawBitmap(bitmap, null, sprite, paint); return }
        canvas.save()
        canvas.scale(-1f, 1f, sprite.centerX(), sprite.centerY())
        canvas.drawBitmap(bitmap, null, sprite, paint)
        canvas.restore()
    }

    companion object {
        private const val WORLD_HEIGHT = 360f
        private const val MIN_WORLD_WIDTH = 640f
        private const val GROUND_THICKNESS = 48f
        private const val IDLE_FRAME_MS = 500L
        private const val IDLE_REDRAW_MS = 120L
        private const val BLINK_MS = 450L
        private val PAPER = Color.argb(240, 255, 253, 246)
        private val NARRATOR_BOX = Color.argb(225, 38, 52, 43)
        private val NARRATOR_TEXT = Color.rgb(240, 236, 222)
        private val LEAF = Color.rgb(48, 83, 65)
        /**
         * Each speaker's dialogue colour (frame, name tag, ▼): one clearly different hue per friend, deep enough for a
         * white name. Close to their characters.json colours, but 루미 is redder than 치즈 and 누누 takes the mint of
         * their ribbon so no two friends look alike. The player (러너) is a calm slate.
         */
        internal val SPEAKER_COLORS = mapOf(
            "mint" to Color.rgb(118, 88, 168),
            "cheese" to Color.rgb(204, 122, 40),
            "popo" to Color.rgb(62, 108, 150),
            "lumi" to Color.rgb(186, 76, 70),
            "nunu" to Color.rgb(54, 136, 116),
            "piko" to Color.rgb(170, 136, 26),
            TalkScript.PLAYER to Color.rgb(96, 106, 120),
        )
        private val INK = Color.rgb(38, 52, 43)
    }
}


/** Korean particle after a word: [withFinal] when the last syllable has a final consonant (라일과), else [without] (치즈와). */
internal object Josa {
    fun with(word: String, withFinal: String, without: String): String {
        val last = word.lastOrNull { it in '\uAC00'..'\uD7A3' } ?: return word + without
        return word + if ((last - '\uAC00') % 28 != 0) withFinal else without
    }
}

/** The friend's item as a still pixel icon (hand-over panel). */
@SuppressLint("ViewConstructor")
internal class ItemIconView(context: Context, private val item: com.tinyrunners.school.quest.QuestItem) : View(context) {
    override fun onDraw(canvas: Canvas) {
        val size = min(width, height).toFloat()
        com.tinyrunners.school.quest.ItemArt.draw(canvas, item, (width - size) / 2f, (height - size) / 2f, size, withGlow = false)
    }
}

/** Which friend leads each scene of a story, for its music. Pure, so it is unit tested. */
internal object TalkMusic {
    /**
     * For each line, the friend who speaks most in its scene (scenes start at the "— title —" narration cards);
     * ties and narration-only scenes go to [main].
     */
    fun sceneLeaders(lines: List<TalkLine>, main: String): List<String> {
        val out = ArrayList<String>(lines.size)
        var start = 0
        fun close(end: Int) {
            val counts = lines.subList(start, end).filter { it.speaker != TalkScript.NARRATOR && it.speaker != TalkScript.PLAYER }
                .groupingBy { it.speaker }.eachCount()
            val best = counts.maxOfOrNull { it.value } ?: 0
            val leader = if ((counts[main] ?: 0) >= best) main else counts.entries.first { it.value == best }.key
            repeat(end - start) { out += leader }
            start = end
        }
        lines.forEachIndexed { i, l -> if (i > start && l.speaker == TalkScript.NARRATOR && l.text.startsWith("— ")) close(i) }
        close(lines.size)
        return out
    }
}
