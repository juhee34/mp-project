package com.tinyrunners.school.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterPortrait
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.weather.Sky
import com.tinyrunners.school.weather.WeatherReport
import kotlin.math.min

/** Draws a [RunnerEngine] and forwards touches. Holds no game rules of its own. */
@SuppressLint("ViewConstructor")
class RunnerView(
    context: Context,
    private val character: CharacterDefinition,
    val engine: RunnerEngine,
) : View(context), Choreographer.FrameCallback {

    private val runnerArt = CharacterPortrait(character.color)
    private val fill = Paint()
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK }
    private val pauseRect = RectF()
    private var lastFrameNanos = 0L
    private var looping = false

    private var scale = 1f
    private var groundY = 0f
    private var animTime = 0f
    private var lastPhase = engine.phase

    /** Current weather from Open-Meteo, or null to draw the default sky. Visual only. */
    var weather: WeatherReport? = null
        set(value) { field = value; invalidate() }

    /** Called once each time a run ends. */
    var onRunOver: (() -> Unit)? = null

    init {
        isFocusable = true
        contentDescription = context.getString(R.string.game_ready_title, character.name)
    }

    fun startLoop() {
        if (looping) return
        looping = true
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stopLoop() {
        looping = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!looping) return
        if (lastFrameNanos != 0L) {
            val dt = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
            engine.update(dt)
            if (engine.phase != RunnerEngine.Phase.PAUSED) animTime += min(dt, 0.05f)
        }
        lastFrameNanos = frameTimeNanos
        if (engine.phase == RunnerEngine.Phase.OVER && lastPhase != RunnerEngine.Phase.OVER) onRunOver?.invoke()
        lastPhase = engine.phase
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); startLoop() }
    override fun onDetachedFromWindow() { stopLoop(); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Keep at least 640 world units visible across and 360 up, whatever the screen ratio.
        scale = min(h / WORLD_HEIGHT, w / MIN_WORLD_WIDTH)
        groundY = h - GROUND_THICKNESS * scale
        val size = 44f * resources.displayMetrics.density
        val margin = 12f * resources.displayMetrics.density
        pauseRect.set(w - margin - size, margin, w - margin, margin + size)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true
        if (engine.phase == RunnerEngine.Phase.RUNNING && pauseRect.contains(event.x, event.y)) {
            engine.pause()
        } else {
            engine.tap()
        }
        performClick()
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(skyColor())
        drawHills(canvas, w)
        fill.color = GROUND; canvas.drawRect(0f, groundY, w, h, fill)
        fill.color = GRASS; canvas.drawRect(0f, groundY, w, groundY + 6f * scale, fill)
        drawGroundMarks(canvas, w)

        for (o in engine.obstacles) drawObstacle(canvas, o)
        drawRunner(canvas)
        drawWeather(canvas, w)
        drawHud(canvas, w)

        when (engine.phase) {
            RunnerEngine.Phase.READY -> overlay(canvas, w, h,
                context.getString(R.string.game_ready_title, character.name),
                context.getString(R.string.game_ready_hint), null)
            RunnerEngine.Phase.PAUSED -> overlay(canvas, w, h,
                context.getString(R.string.game_paused),
                context.getString(R.string.game_paused_hint), null)
            RunnerEngine.Phase.OVER -> overlay(canvas, w, h,
                context.getString(R.string.game_over_title, character.name, engine.meters),
                context.getString(R.string.game_over_score, engine.score, engine.cleared),
                if (onRunOver == null && engine.canRestart) context.getString(R.string.game_over_hint) else null)
            RunnerEngine.Phase.RUNNING -> Unit
        }
    }

    private fun skyColor(): Int {
        val r = weather ?: return SKY
        val base = when (r.sky) {
            Sky.CLEAR -> SKY
            Sky.CLOUDY, Sky.FOG -> SKY_CLOUDY
            Sky.RAIN, Sky.STORM -> SKY_RAIN
            Sky.SNOW -> SKY_SNOW
        }
        return if (r.isDay) base else blend(base, NIGHT, 0.6f)
    }

    /** Rain streaks or snowflakes from a fixed pattern moved by time; no allocation per frame. */
    private fun drawWeather(canvas: Canvas, w: Float) {
        val r = weather ?: return
        val h = groundY
        when (r.sky) {
            Sky.RAIN, Sky.STORM -> {
                fill.color = RAIN_COLOR
                val len = 14f * scale
                for (i in 0 until 60) {
                    val x = ((i * 97.3f + animTime * 90f * scale + engine.distance * 0.6f * scale) % (w + 40f)) - 20f
                    val y = ((i * 53.7f + animTime * 520f * scale) % (h + len)) - len
                    canvas.drawRect(x, y, x + 1.6f * scale, y + len, fill)
                }
            }
            Sky.SNOW -> {
                fill.color = Color.WHITE
                val size = 3f * scale
                for (i in 0 until 50) {
                    val drift = kotlin.math.sin(animTime * 1.3f + i) * 10f * scale
                    val x = ((i * 89.1f + engine.distance * 0.3f * scale) % (w + 20f)) - 10f + drift
                    val y = ((i * 61.3f + animTime * 60f * scale) % (h + size)) - size
                    canvas.drawRect(x, y, x + size, y + size, fill)
                }
            }
            Sky.FOG -> { fill.color = FOG_COLOR; canvas.drawRect(0f, 0f, w, h, fill) }
            Sky.CLEAR, Sky.CLOUDY -> Unit
        }
    }

    private fun blend(a: Int, b: Int, t: Float) = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt())

    private fun sx(worldX: Float) = worldX * scale
    private fun sy(worldY: Float) = groundY - worldY * scale

    private fun drawHills(canvas: Canvas, w: Float) {
        // Slow parallax band so motion reads even without a finished background.
        fill.color = HILL
        val hillW = 220f * scale
        val offset = (engine.distance * 0.2f * scale) % hillW
        var x = -offset
        while (x < w + hillW) {
            canvas.drawRect(x, groundY - 70f * scale, x + hillW * 0.55f, groundY, fill)
            canvas.drawRect(x + hillW * 0.1f, groundY - 95f * scale, x + hillW * 0.42f, groundY, fill)
            x += hillW
        }
    }

    private fun drawGroundMarks(canvas: Canvas, w: Float) {
        fill.color = GROUND_MARK
        val spacing = 48f * scale
        val offset = (engine.distance * scale) % spacing
        var x = -offset
        while (x < w) {
            canvas.drawRect(x, groundY + 16f * scale, x + 14f * scale, groundY + 20f * scale, fill)
            x += spacing
        }
    }

    private fun drawObstacle(canvas: Canvas, o: Obstacle) {
        val l = sx(o.x); val r = sx(o.x + o.width)
        val t = sy(o.height); val b = groundY
        fill.color = CRATE_EDGE; canvas.drawRect(l, t, r, b, fill)
        val p = 3f * scale
        fill.color = CRATE; canvas.drawRect(l + p, t + p, r - p, b, fill)
        fill.color = CRATE_EDGE; canvas.drawRect(l + p, t + (b - t) * 0.45f, r - p, t + (b - t) * 0.45f + p, fill)
    }

    private fun drawRunner(canvas: Canvas) {
        val c = engine.config
        // Small two-frame bob while running on the ground; purely visual, hitbox is unchanged.
        val bob = if (engine.phase == RunnerEngine.Phase.RUNNING && engine.grounded &&
            (engine.distance / 24f).toInt() % 2 == 0) 2f else 0f
        val l = sx(c.playerX).toInt()
        val b = sy(engine.playerY + bob).toInt()
        val size = (c.playerSize * scale).toInt()
        runnerArt.setBounds(l, b - size, l + size, b)
        runnerArt.draw(canvas)
    }

    private fun drawHud(canvas: Canvas, w: Float) {
        val d = resources.displayMetrics.density
        text.textSize = 18f * d
        text.typeface = Typeface.DEFAULT_BOLD
        text.textAlign = Paint.Align.LEFT
        canvas.drawText(character.name, 16f * d, 32f * d, text)
        text.typeface = Typeface.DEFAULT
        text.textSize = 15f * d
        canvas.drawText(context.getString(R.string.game_hud, engine.meters, engine.score), 16f * d, 54f * d, text)
        text.textSize = 11f * d
        text.alpha = 150
        canvas.drawText(context.getString(R.string.game_placeholder_art), 16f * d, 72f * d, text)
        weather?.let {
            text.textAlign = Paint.Align.RIGHT
            val right = if (engine.phase == RunnerEngine.Phase.RUNNING) pauseRect.left - 12f * d else w - 16f * d
            canvas.drawText(context.getString(R.string.weather_hud, context.getString(it.city.label),
                context.getString(it.sky.label), it.temperatureC), right, 40f * d, text)
            text.textAlign = Paint.Align.LEFT
        }
        text.alpha = 255

        if (engine.phase == RunnerEngine.Phase.RUNNING) {
            fill.color = PANEL; canvas.drawRoundRect(pauseRect, 8f * d, 8f * d, fill)
            fill.color = INK
            val bw = pauseRect.width() * 0.14f
            val cx = pauseRect.centerX(); val top = pauseRect.top + pauseRect.height() * 0.28f
            val bottom = pauseRect.bottom - pauseRect.height() * 0.28f
            canvas.drawRect(cx - bw * 1.8f, top, cx - bw * 0.6f, bottom, fill)
            canvas.drawRect(cx + bw * 0.6f, top, cx + bw * 1.8f, bottom, fill)
        }
    }

    private fun overlay(canvas: Canvas, w: Float, h: Float, title: String, line: String, hint: String?) {
        val d = resources.displayMetrics.density
        fill.color = SCRIM; canvas.drawRect(0f, 0f, w, h, fill)
        text.textAlign = Paint.Align.CENTER
        text.typeface = Typeface.DEFAULT_BOLD
        text.textSize = 24f * d
        canvas.drawText(title, w / 2, h * 0.42f, text)
        text.typeface = Typeface.DEFAULT
        text.textSize = 16f * d
        canvas.drawText(line, w / 2, h * 0.42f + 32f * d, text)
        if (hint != null) {
            text.textSize = 14f * d
            canvas.drawText(hint, w / 2, h * 0.42f + 60f * d, text)
        }
    }

    private companion object {
        const val WORLD_HEIGHT = 360f
        const val MIN_WORLD_WIDTH = 640f
        const val GROUND_THICKNESS = 48f
        val SKY = Color.rgb(250, 246, 236)
        val SKY_CLOUDY = Color.rgb(232, 234, 228)
        val SKY_RAIN = Color.rgb(214, 222, 226)
        val SKY_SNOW = Color.rgb(238, 242, 246)
        /** Soft lavender dusk: keeps dark text readable and does not look like an overcast day. */
        val NIGHT = Color.rgb(214, 210, 236)
        val RAIN_COLOR = Color.argb(150, 96, 120, 150)
        val FOG_COLOR = Color.argb(70, 255, 255, 255)
        val HILL = Color.rgb(221, 232, 214)
        val GROUND = Color.rgb(196, 170, 128)
        val GROUND_MARK = Color.rgb(176, 150, 110)
        val GRASS = Color.rgb(122, 160, 110)
        val CRATE = Color.rgb(201, 140, 90)
        val CRATE_EDGE = Color.rgb(120, 78, 50)
        val INK = Color.rgb(43, 59, 47)
        val PANEL = Color.argb(200, 255, 248, 231)
        val SCRIM = Color.argb(170, 250, 246, 236)
    }
}
