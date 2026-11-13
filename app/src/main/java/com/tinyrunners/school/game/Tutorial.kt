package com.tinyrunners.school.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import kotlin.math.roundToInt

/** The first-run guide pages, in order. */
enum class TutorialStep(val title: Int, val body: Int) {
    JUMP(R.string.tutorial_jump_title, R.string.tutorial_jump_body),
    DOUBLE_JUMP(R.string.tutorial_double_title, R.string.tutorial_double_body),
    FLOATING(R.string.tutorial_float_title, R.string.tutorial_float_body),
    PLANK(R.string.tutorial_plank_title, R.string.tutorial_plank_body),
    PICKUPS(R.string.tutorial_pickups_title, R.string.tutorial_pickups_body),
    BAG(R.string.tutorial_bag_title, R.string.tutorial_bag_body),
    PAUSE(R.string.tutorial_pause_title, R.string.tutorial_pause_body),
}

/** Page position for the guide; kept free of Android types so it can be unit-tested. */
class TutorialFlow(private val steps: List<TutorialStep> = TutorialStep.entries) {
    var index = 0; private set
    var finished = false; private set
    val current: TutorialStep get() = steps[index]
    val isLast: Boolean get() = index == steps.lastIndex
    val count: Int get() = steps.size

    /** Next page, or finishes on the last one. */
    fun next() { if (isLast) finished = true else index++ }
    fun skip() { finished = true }
}

/** Remembers whether the guide was completed or skipped; settings can reset it to show it again. */
/** The game guide shows before the first run of each account on this device (guest counts as one account). */
class OnboardingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("onboarding", Context.MODE_PRIVATE)
    private val key = KEY_GAME_GUIDE + "_" + (com.tinyrunners.school.auth.AuthService.currentUid(context) ?: "guest")
    val shouldShowGameGuide: Boolean get() = !prefs.getBoolean(key, false)
    fun markGameGuideDone() { prefs.edit().putBoolean(key, true).apply() }
    fun resetGameGuide() { prefs.edit().remove(key).apply() }

    private companion object { const val KEY_GAME_GUIDE = "gameGuideDone" }
}

/**
 * Card shown over the runner before the first run. It is clickable as a whole, so taps on the guide never reach
 * the game underneath and cannot start a run or jump.
 */
@SuppressLint("ViewConstructor")
class TutorialOverlay(context: Context, private val onDone: () -> Unit) : FrameLayout(context) {
    private val flow = TutorialFlow()
    private val counter = TextView(context)
    private val title = TextView(context)
    private val body = TextView(context)
    private val picture = TutorialPicture(context)
    private val skip = Button(context)
    private val next = Button(context)

    init {
        setBackgroundColor(Color.argb(150, 38, 52, 43))
        isClickable = true
        isFocusable = true

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(18), dp(24), dp(16))
            background = GradientDrawable().apply { setColor(PAPER); cornerRadius = dp(20).toFloat() }
        }
        counter.apply { textSize = 13f; setTextColor(LEAF); typeface = Typeface.DEFAULT_BOLD }
        title.apply { textSize = 22f; setTextColor(INK); typeface = Typeface.DEFAULT_BOLD }
        body.apply { textSize = 16f; setTextColor(MUTED); setLineSpacing(0f, 1.15f) }
        body.textSize = 15f
        card.addView(counter)
        // Landscape: the picture on the left, the title and text on the right, the buttons below. The text scrolls if
        // it is ever too long, so the buttons always stay on screen.
        val middle = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        middle.addView(picture, LinearLayout.LayoutParams(dp(260), dp(140)))
        val words = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        words.addView(title)
        words.addView(android.widget.ScrollView(context).apply { addView(body); isVerticalScrollBarEnabled = true },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
        middle.addView(words, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(18) })
        card.addView(middle, column(8))

        val buttons = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        skip.apply {
            setText(R.string.tutorial_skip); isAllCaps = false; setTextColor(LEAF); minHeight = dp(48)
            background = GradientDrawable().apply { setColor(PAPER); setStroke(dp(1), LINE); cornerRadius = dp(14).toFloat() }
            setOnClickListener { flow.skip(); finish() }
        }
        next.apply {
            isAllCaps = false; setTextColor(Color.WHITE); minHeight = dp(48)
            background = GradientDrawable().apply { setColor(LEAF); cornerRadius = dp(14).toFloat() }
            setOnClickListener { flow.next(); if (flow.finished) finish() else render() }
        }
        buttons.addView(skip, LinearLayout.LayoutParams(dp(120), LinearLayout.LayoutParams.WRAP_CONTENT))
        buttons.addView(next, LinearLayout.LayoutParams(dp(140), LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(10) })
        card.addView(buttons, column(10))

        addView(card, LayoutParams(dp(660), LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
            leftMargin = dp(16); rightMargin = dp(16)
        })
        render()
    }

    private fun render() {
        val step = flow.current
        counter.text = context.getString(R.string.tutorial_counter, flow.index + 1, flow.count)
        title.setText(step.title)
        body.setText(step.body)
        picture.step = step
        next.setText(if (flow.isLast) R.string.tutorial_start else R.string.tutorial_next)
        skip.visibility = if (flow.isLast) View.INVISIBLE else View.VISIBLE
        contentDescription = context.getString(step.title)
    }

    private fun finish() { onDone() }

    private fun column(top: Int) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = dp(top) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private companion object {
        val PAPER = Color.rgb(255, 253, 246)
        val LINE = Color.rgb(221, 224, 209)
        val LEAF = Color.rgb(48, 83, 65)
        val INK = Color.rgb(38, 52, 43)
        val MUTED = Color.rgb(79, 96, 84)
    }
}

/**
 * Small scene for each guide page, made from the game's own pieces so it looks like a real run: 라일's road (the
 * running background), 라일's sprite, the same crates, tower, thorny bar, spikes and plank, coins and items. A dashed
 * line shows where to go, with a faint 라일 at the top of each jump.
 */
private class TutorialPicture(context: Context) : View(context) {
    var step = TutorialStep.JUMP
        set(value) { field = value; invalidate() }

    private val fill = Paint()
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val ghost = Paint().apply { isFilterBitmap = false; isAntiAlias = false; alpha = 120 }
    private val path = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.WHITE; strokeCap = Paint.Cap.ROUND }
    private val pathEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.argb(150, 38, 52, 43); strokeCap = Paint.Cap.ROUND }
    private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.rgb(212, 168, 104) }
    private val curve = Path()
    private val clip = Path()
    private val dst = android.graphics.RectF()
    private val road = StoryBackground.load(context, "mint")
    private val sprites = com.tinyrunners.school.characters.CharacterSprites.load(resources, "mint")
    private val obstacles = ObstacleArt.load(resources, "mint")
    private val hazards = HazardArt.load(resources, "mint")

    /** Draws 라일 standing on [ground] with their feet at x..x+size. */
    private fun runner(canvas: Canvas, frameId: com.tinyrunners.school.characters.SpriteFrame, x: Float, feet: Float, size: Float, paint: Paint = pixel) {
        val s = sprites
        if (s == null) {
            fill.color = Color.rgb(182, 160, 216); canvas.drawRect(x, feet - size * 0.7f, x + size * 0.6f, feet, fill); return
        }
        val k = size / com.tinyrunners.school.characters.CharacterSprites.SIZE
        val top = feet - com.tinyrunners.school.characters.CharacterSprites.FOOT_EDGE_Y * k
        dst.set(x, top, x + size, top + size)
        canvas.drawBitmap(s[frameId], null, dst, paint)
    }

    private fun dashed(canvas: Canvas) {
        canvas.drawPath(curve, pathEdge)
        canvas.drawPath(curve, path)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val d = resources.displayMetrics.density
        val u = h / 110f
        val ground = h - 15 * u
        val size = 34 * u
        clip.reset(); clip.addRoundRect(0f, 0f, w, h, 10 * d, 10 * d, Path.Direction.CW)
        canvas.save(); canvas.clipPath(clip)
        // 라일's road a little way from home, as in a run.
        val bg = road
        if (bg != null) bg.draw(canvas, w, h, ground, 5200f, h / 360f)
        else {
            fill.color = Color.rgb(250, 246, 236); canvas.drawRect(0f, 0f, w, h, fill)
            fill.color = Color.rgb(196, 170, 128); canvas.drawRect(0f, ground, w, h, fill)
        }
        path.strokeWidth = 3.5f * u; pathEdge.strokeWidth = 6f * u
        path.pathEffect = DashPathEffect(floatArrayOf(9 * u, 7 * u), 0f); pathEdge.pathEffect = path.pathEffect

        val runnerX = w * 0.07f
        val objX = w * 0.5f
        val run = com.tinyrunners.school.characters.SpriteFrame.RUN_1
        val jump = com.tinyrunners.school.characters.SpriteFrame.JUMP_0
        curve.reset()
        when (step) {
            TutorialStep.JUMP -> {
                obstacles?.drawBox(canvas, objX, ground - 30 * u, objX + 26 * u, ground)
                curve.moveTo(runnerX + size * 0.6f, ground - 24 * u)
                curve.quadTo(objX + 13 * u, ground - 120 * u, w * 0.86f, ground - 12 * u)
                dashed(canvas)
                runner(canvas, jump, objX - size * 0.35f, ground - 56 * u, size, ghost)
                runner(canvas, run, runnerX, ground, size)
            }
            TutorialStep.DOUBLE_JUMP -> {
                obstacles?.drawTower(canvas, objX, ground - 76 * u, objX + 24 * u, ground)
                val peakX = w * 0.3f
                curve.moveTo(runnerX + size * 0.6f, ground - 24 * u)
                curve.quadTo(w * 0.24f, ground - 72 * u, peakX, ground - 58 * u)
                curve.quadTo(objX + 12 * u, ground - 150 * u, w * 0.86f, ground - 12 * u)
                dashed(canvas)
                runner(canvas, jump, peakX - size * 0.4f, ground - 46 * u, size, ghost)
                // The second press, at the top of the first jump.
                fill.color = Color.rgb(231, 202, 105)
                canvas.drawCircle(peakX, ground - 62 * u, 5 * u, fill)
                runner(canvas, run, runnerX, ground, size)
            }
            TutorialStep.FLOATING -> {
                val art = hazards
                if (art != null) art.drawAir(canvas, objX - 10 * u, ground - 64 * u, objX + 64 * u, ground - 42 * u)
                curve.moveTo(runnerX + size * 0.6f, ground - 10 * u)
                curve.lineTo(w * 0.92f, ground - 10 * u)
                dashed(canvas)
                runner(canvas, run, objX + 10 * u, ground, size, ghost)
                runner(canvas, run, runnerX, ground, size)
            }
            TutorialStep.PLANK -> {
                val art = hazards
                if (art != null) {
                    art.drawSpikes(canvas, w * 0.32f, ground, w * 0.8f, ground - 14 * u)
                    art.drawPlatform(canvas, w * 0.3f, ground - 52 * u, w * 0.82f, 16 * u)
                }
                curve.moveTo(runnerX + size * 0.6f, ground - 24 * u)
                curve.quadTo(w * 0.28f, ground - 104 * u, w * 0.4f, ground - 64 * u)
                curve.lineTo(w * 0.8f, ground - 64 * u)
                curve.quadTo(w * 0.87f, ground - 60 * u, w * 0.93f, ground - 12 * u)
                dashed(canvas)
                runner(canvas, run, w * 0.55f, ground - 52 * u, size, ghost)
                runner(canvas, run, runnerX, ground, size)
            }
            TutorialStep.PICKUPS -> {
                CoinArt.draw(canvas, w * 0.38f, ground - 12 * u, 9 * u, spin = 0f)
                com.tinyrunners.school.quest.QuestItems.of("mint")?.let { item ->
                    com.tinyrunners.school.quest.ItemArt.draw(canvas, item, w * 0.54f - 11 * u, ground - 26 * u, 22 * u)
                }
                CoinArt.draw(canvas, w * 0.76f, ground - 70 * u, 9 * u, spin = 0f)
                curve.moveTo(runnerX + size * 0.6f, ground - 10 * u)
                curve.lineTo(w * 0.62f, ground - 10 * u)
                curve.quadTo(w * 0.74f, ground - 124 * u, w * 0.9f, ground - 12 * u)
                dashed(canvas)
                runner(canvas, run, runnerX, ground, size)
            }
            TutorialStep.BAG -> {
                // Coins go to the shop, items to the friends; both are kept in the pouch (bag).
                fill.color = Color.argb(150, 255, 248, 231)
                canvas.drawRoundRect(w * 0.2f, h * 0.12f, w * 0.8f, h * 0.78f, 12 * u, 12 * u, fill)
                val bag = 54 * u
                com.tinyrunners.school.ui.PouchIcon.draw(canvas, resources, w / 2 - bag / 2, h * 0.45f - bag / 2, bag)
                CoinArt.draw(canvas, w * 0.3f, h * 0.45f, 10 * u, spin = 0f)
                com.tinyrunners.school.quest.QuestItems.of("mint")?.let { item ->
                    com.tinyrunners.school.quest.ItemArt.draw(canvas, item, w * 0.7f - 12 * u, h * 0.45f - 12 * u, 24 * u, withGlow = false)
                }
            }
            TutorialStep.PAUSE -> {
                runner(canvas, com.tinyrunners.school.characters.SpriteFrame.IDLE_0, w * 0.18f, ground, size)
                // The game's 정지 button, as it sits at the top right during a run.
                val bw = 74 * u; val bh = 28 * u; val left = w * 0.55f; val top = h * 0.2f
                fill.color = Color.rgb(46, 90, 66)
                canvas.drawRoundRect(left, top, left + bw, top + bh, bh / 2f, bh / 2f, fill)
                fill.color = Color.rgb(255, 248, 231)
                canvas.drawRect(left + 14 * u, top + 8 * u, left + 18 * u, top + bh - 8 * u, fill)
                canvas.drawRect(left + 22 * u, top + 8 * u, left + 26 * u, top + bh - 8 * u, fill)
                fill.isAntiAlias = true; fill.textSize = 13 * u; fill.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText("정지", left + 32 * u, top + bh / 2f + 4.5f * u, fill)
                fill.isAntiAlias = false
            }
        }
        canvas.restore()
        frame.strokeWidth = 2 * d
        canvas.drawRoundRect(d, d, w - d, h - d, 10 * d, 10 * d, frame)
    }
}
