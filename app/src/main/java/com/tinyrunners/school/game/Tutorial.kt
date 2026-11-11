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
        card.addView(counter)
        card.addView(title, column(4))
        card.addView(picture, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(110)).apply { topMargin = dp(10) })
        card.addView(body, column(10))

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
        card.addView(buttons, column(14))

        addView(card, LayoutParams(dp(520), LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
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

/** Small diagram for each guide page, drawn with the same colours as the game. */
private class TutorialPicture(context: Context) : View(context) {
    var step = TutorialStep.JUMP
        set(value) { field = value; invalidate() }

    private val fill = Paint()
    private val path = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(48, 83, 65)
        pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
    }
    private val curve = Path()
    private val tri = Path()

    /** One triangle tooth from (x0..x1, baseY) to a tip at tipY, with a thin dark edge. */
    private fun teeth(canvas: Canvas, x0: Float, baseY: Float, x1: Float, tipY: Float, color: Int) {
        tri.reset(); tri.moveTo(x0, baseY); tri.lineTo((x0 + x1) / 2f, tipY); tri.lineTo(x1, baseY); tri.close()
        fill.color = Color.rgb(70, 56, 76); canvas.drawPath(tri, fill)
        val inset = (x1 - x0) * 0.18f
        val tipIn = if (tipY < baseY) tipY + inset else tipY - inset
        tri.reset(); tri.moveTo(x0 + inset, baseY); tri.lineTo((x0 + x1) / 2f, tipIn); tri.lineTo(x1 - inset, baseY); tri.close()
        fill.color = color; canvas.drawPath(tri, fill)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val u = h / 110f
        val ground = h - 14 * u
        path.strokeWidth = 4 * u
        fill.color = Color.rgb(250, 246, 236); canvas.drawRect(0f, 0f, w, h, fill)
        fill.color = Color.rgb(196, 170, 128); canvas.drawRect(0f, ground, w, h, fill)
        fill.color = Color.rgb(122, 160, 110); canvas.drawRect(0f, ground, w, ground + 3 * u, fill)

        val runnerX = w * 0.12f
        val objX = w * 0.5f
        fun runner(x: Float) {
            fill.color = Color.rgb(182, 160, 216); canvas.drawRect(x, ground - 26 * u, x + 22 * u, ground, fill)
            fill.color = Color.rgb(38, 52, 43); canvas.drawRect(x + 14 * u, ground - 20 * u, x + 17 * u, ground - 16 * u, fill)
        }
        fun crate(left: Float, top: Float, right: Float, bottom: Float) {
            fill.color = Color.rgb(120, 78, 50); canvas.drawRect(left, top, right, bottom, fill)
            fill.color = Color.rgb(201, 140, 90); canvas.drawRect(left + 3 * u, top + 3 * u, right - 3 * u, bottom, fill)
        }
        curve.reset()
        when (step) {
            TutorialStep.JUMP -> {
                runner(runnerX)
                crate(objX, ground - 34 * u, objX + 26 * u, ground)
                curve.moveTo(runnerX + 22 * u, ground - 26 * u)
                curve.quadTo(objX + 13 * u, ground - 120 * u, w * 0.82f, ground - 26 * u)
                canvas.drawPath(curve, path)
            }
            TutorialStep.DOUBLE_JUMP -> {
                runner(runnerX)
                crate(objX, ground - 80 * u, objX + 22 * u, ground)
                fill.color = Color.rgb(120, 78, 50)
                canvas.drawRect(objX + 3 * u, ground - 54 * u, objX + 19 * u, ground - 52 * u, fill)
                canvas.drawRect(objX + 3 * u, ground - 28 * u, objX + 19 * u, ground - 26 * u, fill)
                // First jump up to a peak, then a second, higher arc over the tower.
                val peakX = w * 0.32f
                curve.moveTo(runnerX + 22 * u, ground - 26 * u)
                curve.quadTo(w * 0.26f, ground - 74 * u, peakX, ground - 60 * u)
                curve.quadTo(objX + 11 * u, ground - 150 * u, w * 0.84f, ground - 26 * u)
                canvas.drawPath(curve, path)
                fill.color = Color.rgb(231, 202, 105)
                canvas.drawRect(peakX - 5 * u, ground - 65 * u, peakX + 5 * u, ground - 55 * u, fill)
            }
            TutorialStep.FLOATING -> {
                runner(runnerX)
                // Thin thorny bar, the same look as in the game.
                var x = objX - 10 * u
                while (x < objX + 60 * u) {
                    teeth(canvas, x, ground - 56 * u, x + 7 * u, ground - 62 * u, Color.rgb(246, 240, 226))
                    teeth(canvas, x, ground - 50 * u, x + 7 * u, ground - 44 * u, Color.rgb(246, 240, 226))
                    x += 7 * u
                }
                fill.color = Color.rgb(96, 66, 44); canvas.drawRect(objX - 10 * u, ground - 56 * u, objX + 60 * u, ground - 50 * u, fill)
                curve.moveTo(runnerX + 22 * u, ground - 12 * u)
                curve.lineTo(w * 0.86f, ground - 12 * u)
                canvas.drawPath(curve, path)
            }
            TutorialStep.PLANK -> {
                runner(runnerX)
                // Spikes on the ground, a hanging plank above; jump up and run across the plank.
                var x = w * 0.32f
                while (x < w * 0.78f) { teeth(canvas, x, ground, x + 9 * u, ground - 14 * u, Color.rgb(196, 202, 220)); x += 9 * u }
                fill.color = Color.rgb(150, 112, 74)
                canvas.drawRect(w * 0.31f, ground - 100 * u, w * 0.31f + 2 * u, ground - 50 * u, fill)
                canvas.drawRect(w * 0.79f - 2 * u, ground - 100 * u, w * 0.79f, ground - 50 * u, fill)
                fill.color = Color.rgb(98, 64, 42); canvas.drawRect(w * 0.29f, ground - 50 * u, w * 0.81f, ground - 42 * u, fill)
                fill.color = Color.rgb(196, 146, 94); canvas.drawRect(w * 0.29f + 2 * u, ground - 48 * u, w * 0.81f - 2 * u, ground - 44 * u, fill)
                curve.moveTo(runnerX + 22 * u, ground - 26 * u)
                curve.quadTo(w * 0.3f, ground - 100 * u, w * 0.4f, ground - 76 * u)
                curve.lineTo(w * 0.8f, ground - 76 * u)
                curve.quadTo(w * 0.86f, ground - 70 * u, w * 0.9f, ground - 26 * u)
                canvas.drawPath(curve, path)
            }
            TutorialStep.PAUSE -> {
                val size = 44 * u
                val left = w / 2 - size / 2; val top = h / 2 - size / 2 - 6 * u
                fill.color = Color.rgb(255, 248, 231); canvas.drawRect(left, top, left + size, top + size, fill)
                fill.color = Color.rgb(43, 59, 47)
                canvas.drawRect(left + size * 0.28f, top + size * 0.25f, left + size * 0.42f, top + size * 0.75f, fill)
                canvas.drawRect(left + size * 0.58f, top + size * 0.25f, left + size * 0.72f, top + size * 0.75f, fill)
            }
        }
    }
}
