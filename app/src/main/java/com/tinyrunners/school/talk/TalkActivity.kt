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
class TalkActivity : Activity() {
    override fun onPause() {
        com.tinyrunners.school.audio.Music.pauseSoon()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Replays from 이야기 모음 are part of the story, so they get the story music.
        if (intent.getIntExtra(EXTRA_MODE, 0) == TalkMode.REPLAY.ordinal) com.tinyrunners.school.audio.Music.playStory(this)
        else com.tinyrunners.school.audio.Music.playGame(this)
    }

    private var view: TalkView? = null
    private var done = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val characterId = intent.getStringExtra(GameActivity.EXTRA_CHARACTER_ID).orEmpty()
        val character = CharacterRepository(this).find(characterId)
        if (character == null) { finish(); return }
        val mode = TalkMode.entries.getOrNull(intent.getIntExtra(EXTRA_MODE, 0)) ?: TalkMode.START
        val firstTime = mode == TalkMode.START && TalkProgress(this).shouldShowTalk(character.id)
        val meters = intent.getIntExtra(EXTRA_METERS, 0)
        val lines = TalkPlan.lines(
            mode, character.id, firstTime,
            fullTalk = TalkRepository.load(this)?.talkFor(character.id).orEmpty(),
            narration = if (mode == TalkMode.STUMBLE) getString(R.string.talk_stumble_narration, meters) else null,
        ) { event -> ShortLineRepository.pick(this, character.id, event) }
        if (lines.isEmpty()) { finishTalk(character.id, firstTime); return }

        val player = TalkPlayer(lines)
        savedInstanceState?.let { player.restore(it.getInt(KEY_INDEX)) }
        val names = mapOf(
            character.id to character.name,
            TalkScript.PLAYER to (AuthService.currentName(this) ?: getString(R.string.talk_player_default)),
        )
        // After a crash the scene shows the spot where the friend fell, with the friend in their tumble pose.
        val talk = TalkView(this, character.id, player, names,
            distance = intent.getFloatExtra(EXTRA_DISTANCE, 0f), fallen = mode == TalkMode.STUMBLE) { finishTalk(character.id, firstTime) }
        view = talk

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
            setOnClickListener { finishTalk(character.id, firstTime) }
        }
        val margin = (12 * resources.displayMetrics.density).roundToInt()
        setContentView(FrameLayout(this).apply {
            addView(talk, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(skip, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END).apply { setMargins(0, margin, margin, 0) })
        })
    }

    /** Talk finished or skipped: go on to the run (start/retry) or to the result screen (after a crash). */
    private fun finishTalk(characterId: String, firstTime: Boolean) {
        if (done) return
        done = true
        if (intent.getIntExtra(EXTRA_MODE, 0) == TalkMode.REPLAY.ordinal) { finish(); return }
        // The long first talk is shown once per friend; later runs get short lines instead.
        if (firstTime) TalkProgress(this).markTalkSeen(characterId)
        @Suppress("DEPRECATION")
        val next = intent.getParcelableExtra<Intent>(EXTRA_NEXT) ?: GameActivity.intent(this, characterId)
        startActivity(next)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        view?.let { outState.putInt(KEY_INDEX, it.player.index) }
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_INDEX = "talkIndex"
        private const val EXTRA_MODE = "talkMode"
        private const val EXTRA_NEXT = "talkNext"
        private const val EXTRA_METERS = "talkMeters"
        private const val EXTRA_DISTANCE = "talkDistance"

        /** Before a run chosen on the selection screen. */
        fun intent(context: Context, characterId: String) =
            Intent(context, TalkActivity::class.java).putExtra(GameActivity.EXTRA_CHARACTER_ID, characterId)

        /** A friend's long first talk again, from 이야기 모음; closes at the end without a run. */
        fun replay(context: Context, characterId: String) = intent(context, characterId).putExtra(EXTRA_MODE, TalkMode.REPLAY.ordinal)

        /** Before "한 번 더 달리기"; [next] is the run to start afterwards. */
        fun retry(context: Context, characterId: String, next: Intent) = intent(context, characterId)
            .putExtra(EXTRA_MODE, TalkMode.RETRY.ordinal).putExtra(EXTRA_NEXT, next)

        /** Right after a crash; [result] is the result screen shown afterwards. */
        fun stumble(context: Context, characterId: String, meters: Int, distance: Float, result: Intent) = intent(context, characterId)
            .putExtra(EXTRA_MODE, TalkMode.STUMBLE.ordinal).putExtra(EXTRA_NEXT, result)
            .putExtra(EXTRA_METERS, meters).putExtra(EXTRA_DISTANCE, distance)
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
    private val onFinished: () -> Unit,
) : View(context) {
    private val scenery = StoryBackground.load(context, characterId)
    private val fallback = ScenePainter()
    private val sprites = CharacterSprites.load(resources, characterId)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = dpF(19f); color = INK }
    private val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dpF(16f); color = Color.WHITE; typeface = Typeface.DEFAULT_BOLD
    }
    private val box = RectF()
    private val tag = RectF()
    private val sprite = RectF()
    private var lastTick = SystemClock.uptimeMillis()
    private var typed = 0f
    private var announced = -1
    private var layout: StaticLayout? = null
    private var layoutFor = -1 to -1

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

        // The friend, large and centred, standing just above the box; idle frames alternate slowly.
        sprites?.let {
            val frame = if (fallen) SpriteFrame.FAIL_0
                else if ((SystemClock.uptimeMillis() / IDLE_FRAME_MS) % 2 == 0L) SpriteFrame.IDLE_0 else SpriteFrame.IDLE_1
            val px = max(1, ((box.top - dpF(24f)) * 0.62f / CharacterSprites.SIZE).roundToInt()).toFloat()
            val size = CharacterSprites.SIZE * px
            val feet = box.top - dpF(6f)
            val left = (w / 2f - CharacterSprites.CENTER_X * px).roundToInt().toFloat()
            val top = (feet - CharacterSprites.FOOT_EDGE_Y * px).roundToInt().toFloat()
            sprite.set(left, top, left + size, top + size)
            canvas.drawBitmap(it[frame], null, sprite, pixel)
        }

        val line = player.current ?: return
        val narrator = line.speaker == TalkScript.NARRATOR
        fill.color = if (narrator) NARRATOR_BOX else PAPER
        canvas.drawRoundRect(box, dpF(14f), dpF(14f), fill)
        fill.style = Paint.Style.STROKE; fill.strokeWidth = dpF(3f); fill.color = LEAF
        canvas.drawRoundRect(box, dpF(14f), dpF(14f), fill)
        fill.style = Paint.Style.FILL

        if (!narrator) {
            val name = names[line.speaker] ?: line.speaker
            val tagW = namePaint.measureText(name) + dpF(28f)
            tag.set(box.left + dpF(20f), box.top - dpF(18f), box.left + dpF(20f) + tagW, box.top + dpF(18f))
            fill.color = if (line.speaker == TalkScript.PLAYER) PLAYER_TAG else LEAF
            canvas.drawRoundRect(tag, dpF(18f), dpF(18f), fill)
            canvas.drawText(name, tag.left + dpF(14f), tag.centerY() - (namePaint.ascent() + namePaint.descent()) / 2f, namePaint)
        }

        // Typewriter: about 30 characters a second; a tap shows the whole line at once.
        val now = SystemClock.uptimeMillis()
        if (player.isTyping) {
            typed += (now - lastTick) / CHAR_MS
            val whole = typed.toInt()
            if (whole > 0) { player.type(whole); typed -= whole }
        } else typed = 0f
        lastTick = now

        val textLeft = box.left + dpF(24f)
        val textWidth = (box.width() - dpF(48f)).toInt()
        textPaint.color = if (narrator) NARRATOR_TEXT else INK
        textPaint.typeface = if (narrator) Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) else Typeface.DEFAULT
        val key = player.index to player.shown
        if (layoutFor != key || layout == null) {
            layout = StaticLayout.Builder.obtain(line.text, 0, player.shown, textPaint, textWidth)
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
            fill.color = LEAF
            canvas.drawPath(path, fill)
        }

        if (announced != player.index) {
            announced = player.index
            val who = if (narrator) "" else (names[line.speaker] ?: line.speaker) + ". "
            contentDescription = who + line.text
            announceForAccessibility(contentDescription)
        }
        postInvalidateOnAnimation()
    }

    companion object {
        private const val WORLD_HEIGHT = 360f
        private const val MIN_WORLD_WIDTH = 640f
        private const val GROUND_THICKNESS = 48f
        private const val CHAR_MS = 33f
        private const val IDLE_FRAME_MS = 500L
        private const val BLINK_MS = 450L
        private val PAPER = Color.argb(240, 255, 253, 246)
        private val NARRATOR_BOX = Color.argb(225, 38, 52, 43)
        private val NARRATOR_TEXT = Color.rgb(240, 236, 222)
        private val LEAF = Color.rgb(48, 83, 65)
        private val PLAYER_TAG = Color.rgb(176, 112, 64)
        private val INK = Color.rgb(38, 52, 43)
    }
}
