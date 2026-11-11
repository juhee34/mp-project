package com.tinyrunners.school.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Rect
import android.graphics.Typeface
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterPortrait
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.data.CharacterDefinition
import com.tinyrunners.school.weather.Sky
import com.tinyrunners.school.weather.WeatherReport
import kotlin.math.min
import kotlin.math.roundToInt

/** Draws a [RunnerEngine] and forwards touches. Holds no game rules of its own. */
@SuppressLint("ViewConstructor")
class RunnerView(
    context: Context,
    private val character: CharacterDefinition,
    val engine: RunnerEngine,
) : View(context), Choreographer.FrameCallback {

    private val runnerArt = CharacterPortrait(character.color)
    private val sprites = CharacterSprites.load(resources, character.id)
    private val storyBackground = StoryBackground.load(context, character.id)
    /** A's hometown props for boxes and towers; floating branches keep their drawn look. */
    private val obstacleArt = ObstacleArt.load(resources, character.id)
    /** The friend's hometown item scattered on the road (quest/Quest.kt). */
    private val questItem = com.tinyrunners.school.quest.QuestItems.of(character.id)
    private val spritePaint = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        isDither = false
    }
    private val spriteBounds = Rect()
    private var spriteScale = 1
    private val fill = Paint()
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK }
    /** Top bar buttons shown during a run: 정지 (pause) and 진행 (resume). */
    private val stopRect = RectF()
    private val goRect = RectF()
    private var lastFrameNanos = 0L
    private var looping = false

    private var scale = 1f
    private var groundY = 0f
    private var animTime = 0f
    private var lastPhase = engine.phase

    /** Current weather from Open-Meteo, or null to draw the default sky. Visual only. */
    var weather: WeatherReport? = null
    /** Shop decoration drawn behind the friend; looks only, never touches the physics. */
    var trail: com.tinyrunners.school.shop.TrailStyle = com.tinyrunners.school.shop.TrailStyle.NONE
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
        if (engine.phase == RunnerEngine.Phase.READY && lastPhase != RunnerEngine.Phase.READY) animTime = 0f
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
        // One art pixel covers playerSize / 48 world units (1.75); round to whole screen pixels so the art stays sharp.
        spriteScale = (scale * engine.config.playerSize / CharacterSprites.SIZE).roundToInt().coerceAtLeast(1)
        groundY = h - GROUND_THICKNESS * scale
        val d = resources.displayMetrics.density
        val bw = 104f * d; val bh = 44f * d; val margin = 12f * d; val gap = 8f * d
        goRect.set(w - margin - bw, margin, w - margin, margin + bh)
        stopRect.set(goRect.left - gap - bw, margin, goRect.left - gap, margin + bh)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // ACTION_POINTER_DOWN lets a second finger jump while the first rests on the screen.
        val action = event.actionMasked
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_POINTER_DOWN) return true
        val i = event.actionIndex
        val x = event.getX(i); val y = event.getY(i)
        when {
            // The two top buttons never make the runner jump.
            buttonsVisible && stopRect.contains(x, y) -> engine.pause()
            buttonsVisible && goRect.contains(x, y) -> engine.resume()
            // When a result screen follows (onRunOver set), a late tap after the crash must not restart the run.
            engine.phase != RunnerEngine.Phase.OVER || onRunOver == null -> engine.tap()
        }
        performClick()
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(skyColor())
        val background = storyBackground
        if (background != null) {
            background.draw(canvas, w, h, groundY, engine.distance, scale)
        } else {
            drawHills(canvas, w)
            fill.color = GROUND; canvas.drawRect(0f, groundY, w, h, fill)
            fill.color = GRASS; canvas.drawRect(0f, groundY, w, groundY + 6f * scale, fill)
            drawGroundMarks(canvas, w)
        }

        for (o in engine.obstacles) drawObstacle(canvas, o)
        drawPickups(canvas)
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
        // Drawn last so the 진행 button stays clear above the pause overlay.
        if (buttonsVisible) {
            val running = engine.phase == RunnerEngine.Phase.RUNNING
            drawBarButton(canvas, stopRect, context.getString(R.string.game_stop_button), active = running, pauseIcon = true)
            drawBarButton(canvas, goRect, context.getString(R.string.game_go_button), active = !running, pauseIcon = false)
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
        val t = sy(o.top); val b = sy(o.bottom)
        val p = 3f * scale
        val art = obstacleArt
        if (art != null && o.kind == ObstacleKind.BOX) { art.drawBox(canvas, l, t, r, b); return }
        if (art != null && o.kind == ObstacleKind.TOWER) { art.drawTower(canvas, l, t, r, b); return }
        when (o.kind) {
            ObstacleKind.BOX -> {
                fill.color = CRATE_EDGE; canvas.drawRect(l, t, r, b, fill)
                fill.color = CRATE; canvas.drawRect(l + p, t + p, r - p, b, fill)
                fill.color = CRATE_EDGE; canvas.drawRect(l + p, t + (b - t) * 0.45f, r - p, t + (b - t) * 0.45f + p, fill)
            }
            ObstacleKind.TOWER -> {
                // A stack of crates, one band every ~40 units, so it reads as "too tall for one jump".
                fill.color = CRATE_EDGE; canvas.drawRect(l, t, r, b, fill)
                fill.color = TOWER; canvas.drawRect(l + p, t + p, r - p, b, fill)
                fill.color = CRATE_EDGE
                val bands = (o.height / 40f).toInt().coerceAtLeast(2)
                for (k in 1 until bands) {
                    val y = t + (b - t) * k / bands
                    canvas.drawRect(l + p, y, r - p, y + p, fill)
                }
            }
            ObstacleKind.FLOAT -> drawThornBar(canvas, l, t, r, b)
            ObstacleKind.SPIKES -> drawSpikeField(canvas, o)
        }
    }

    private val shape = android.graphics.Path()

    private fun Canvas.tooth(x0: Float, baseY: Float, x1: Float, tipY: Float, color: Int) {
        shape.reset(); shape.moveTo(x0, baseY); shape.lineTo((x0 + x1) / 2f, tipY); shape.lineTo(x1, baseY); shape.close()
        fill.color = color
        drawPath(shape, fill)
    }

    /** A thin branch with pale thorns above and below: clearly not something to land on. Thorns are in the hit box. */
    private fun drawThornBar(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val thorn = (b - t) * 0.3f
        val barTop = t + thorn; val barBottom = b - thorn
        val toothW = 10f * scale
        var x = l
        while (x < r - 1f) {
            val x1 = (x + toothW).coerceAtMost(r)
            val o = 1.2f * scale
            canvas.tooth(x - o, barTop + o, x1 + o, t - o, THORN_EDGE)
            canvas.tooth(x - o, barBottom - o, x1 + o, b + o, THORN_EDGE)
            canvas.tooth(x, barTop, x1, t, THORN)
            canvas.tooth(x, barBottom, x1, b, THORN)
            x += toothW
        }
        fill.color = BRANCH_EDGE; canvas.drawRect(l, barTop, r, barBottom, fill)
        val p = 1.5f * scale
        fill.color = BRANCH; canvas.drawRect(l + p, barTop + p, r - p, barBottom - p, fill)
    }

    /** Spikes along the ground with a hanging plank above them; the plank is the only way across. */
    private fun drawSpikeField(canvas: Canvas, o: Obstacle) {
        val l = sx(o.x); val r = sx(o.x + o.width)
        val ground = sy(0f); val tip = sy(o.top)
        fill.color = SPIKE_BASE; canvas.drawRect(l, ground - 5f * scale, r, ground, fill)
        val toothW = 14f * scale
        var x = l
        while (x < r - 1f) {
            val x1 = (x + toothW).coerceAtMost(r)
            val o2 = 1.2f * scale
            canvas.tooth(x - o2, ground, x1 + o2, tip - o2 * 1.5f, SPIKE_EDGE)
            canvas.tooth(x, ground, x1, tip, SPIKE)
            canvas.tooth(x, ground, (x + x1) / 2f, tip, SPIKE_SHINE)
            x += toothW
        }
        // Plank hanging on two ropes.
        val pl = sx(o.plankLeft); val pr = sx(o.plankRight)
        val pt = sy(o.plankTop); val pb = sy(o.plankTop - engine.config.plankThickness)
        fill.color = ROPE
        val rope = 2f * scale
        for (rx in floatArrayOf(pl + 10f * scale, pr - 10f * scale - rope)) canvas.drawRect(rx, sy(o.plankTop + 90f), rx + rope, pt, fill)
        fill.color = PLANK_EDGE; canvas.drawRect(pl, pt, pr, pb, fill)
        val p = 2f * scale
        fill.color = PLANK; canvas.drawRect(pl + p, pt + p, pr - p, pb - p, fill)
        fill.color = PLANK_TOP; canvas.drawRect(pl + p, pt + p, pr - p, pt + p * 2f, fill)
        fill.color = PLANK_EDGE
        var seam = pl + 40f * scale
        while (seam < pr - 10f * scale) { canvas.drawRect(seam, pt + p, seam + p, pb - p, fill); seam += 40f * scale }
    }

    private fun drawRunner(canvas: Canvas) {
        val c = engine.config
        val art = sprites
        if (art != null) {
            val center = sx(c.playerX + c.playerSize / 2).roundToInt()
            val feet = sy(engine.playerY).roundToInt()
            val left = center - CharacterSprites.CENTER_X * spriteScale
            val top = feet - CharacterSprites.FOOT_EDGE_Y * spriteScale
            val size = CharacterSprites.SIZE * spriteScale
            spriteBounds.set(left, top, left + size, top + size)
            if (engine.phase != RunnerEngine.Phase.READY) com.tinyrunners.school.shop.TrailPainter.draw(
                canvas, trail, left + spriteScale * 10f, top + size * 0.55f, spriteScale * 1.5f, engine.distance / 40f)
            canvas.drawBitmap(art[RunnerAnimation.frame(engine, animTime)], null, spriteBounds, spritePaint)
            return
        }
        val l = sx(c.playerX).toInt()
        val b = sy(engine.playerY).toInt()
        val size = (c.playerSize * scale).toInt()
        runnerArt.setBounds(l, b - size, l + size, b)
        runnerArt.draw(canvas)
    }

    /** Items bob gently and glow, so they read as something to collect rather than something to avoid. */
    private fun drawPickups(canvas: Canvas) {
        val item = questItem ?: return
        for (p in engine.pickups) {
            if (p.taken) continue
            val bob = kotlin.math.sin(animTime * 4f + p.x * 0.02f) * 4f
            val size = p.size * scale
            com.tinyrunners.school.quest.ItemArt.draw(canvas, item, sx(p.x), sy(p.bottom + p.size + bob), size)
        }
    }

    private fun drawHud(canvas: Canvas, w: Float) {
        val d = resources.displayMetrics.density
        // Stable contrast over detailed scenery, including snowy and dusk scenes.
        fill.color = PANEL
        canvas.drawRect(0f, 0f, w, 80f * d, fill)
        text.textSize = 18f * d
        text.typeface = Typeface.DEFAULT_BOLD
        text.textAlign = Paint.Align.LEFT
        canvas.drawText(character.name, 16f * d, 32f * d, text)
        text.typeface = Typeface.DEFAULT
        text.textSize = 15f * d
        val hud = context.getString(R.string.game_hud, engine.meters, engine.score)
        canvas.drawText(hud, 16f * d, 54f * d, text)
        questItem?.let { item ->
            // Items picked up this run, next to the distance.
            val x = 16f * d + text.measureText(hud) + 14f * d
            com.tinyrunners.school.quest.ItemArt.draw(canvas, item, x, 40f * d, 18f * d, withGlow = false)
            canvas.drawText(context.getString(R.string.item_hud, engine.collected), x + 22f * d, 54f * d, text)
        }
        text.textSize = 11f * d
        text.alpha = 150
        if (sprites == null) {
            canvas.drawText(context.getString(R.string.game_placeholder_art), 16f * d, 72f * d, text)
        }
        weather?.let {
            text.textAlign = Paint.Align.RIGHT
            val right = if (buttonsVisible) stopRect.left - 12f * d else w - 16f * d
            canvas.drawText(context.getString(R.string.weather_hud, context.getString(it.city.label),
                context.getString(it.sky.label), it.temperatureC), right, 40f * d, text)
            text.textAlign = Paint.Align.LEFT
        }
        text.alpha = 255

    }

    /** Pause and resume are only meaningful while a run is going or resting. */
    private val buttonsVisible: Boolean
        get() = engine.phase == RunnerEngine.Phase.RUNNING || engine.phase == RunnerEngine.Phase.PAUSED

    /** A pill with an icon and a label; the button that can be pressed now is filled, the other is faint. */
    private fun drawBarButton(canvas: Canvas, r: RectF, label: String, active: Boolean, pauseIcon: Boolean) {
        val d = resources.displayMetrics.density
        fill.color = if (active) BUTTON_ON else PANEL
        fill.alpha = if (active) 255 else 150
        canvas.drawRoundRect(r, r.height() / 2f, r.height() / 2f, fill)
        fill.alpha = 255
        val ink = if (active) android.graphics.Color.WHITE else INK
        fill.color = ink
        if (!active) fill.alpha = 130
        val s = r.height() * 0.36f
        val ix = r.left + r.height() * 0.42f
        val iy = r.centerY()
        if (pauseIcon) {
            canvas.drawRect(ix, iy - s / 2, ix + s * 0.32f, iy + s / 2, fill)
            canvas.drawRect(ix + s * 0.62f, iy - s / 2, ix + s * 0.94f, iy + s / 2, fill)
        } else {
            val p = android.graphics.Path().apply { moveTo(ix, iy - s / 2); lineTo(ix + s * 0.9f, iy); lineTo(ix, iy + s / 2); close() }
            canvas.drawPath(p, fill)
        }
        fill.alpha = 255
        text.color = ink
        if (!active) text.alpha = 130
        text.typeface = Typeface.DEFAULT_BOLD
        text.textSize = 16f * d
        text.textAlign = Paint.Align.LEFT
        canvas.drawText(label, ix + s + 10f * d, iy - (text.ascent() + text.descent()) / 2f, text)
        text.alpha = 255
        text.color = INK
        text.typeface = Typeface.DEFAULT
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
        val TOWER = Color.rgb(183, 120, 76)
        val BRANCH = Color.rgb(150, 112, 78)
        val BRANCH_EDGE = Color.rgb(96, 66, 44)
        val LEAF = Color.rgb(106, 150, 96)
        val INK = Color.rgb(43, 59, 47)
        val PANEL = Color.argb(200, 255, 248, 231)
        val THORN = Color.rgb(246, 240, 226)
        val THORN_EDGE = Color.rgb(92, 58, 70)
        val SPIKE = Color.rgb(196, 202, 220)
        val SPIKE_SHINE = Color.rgb(232, 236, 246)
        val SPIKE_EDGE = Color.rgb(58, 52, 78)
        val SPIKE_BASE = Color.rgb(84, 74, 100)
        val PLANK = Color.rgb(196, 146, 94)
        val PLANK_TOP = Color.rgb(230, 186, 126)
        val PLANK_EDGE = Color.rgb(98, 64, 42)
        val ROPE = Color.rgb(150, 112, 74)
        val BUTTON_ON = Color.rgb(48, 83, 65)
        val SCRIM = Color.argb(170, 250, 246, 236)
    }
}
