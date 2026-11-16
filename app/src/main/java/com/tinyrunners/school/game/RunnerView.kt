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
    /** The plain road props plus the pretty ones bought in the shop. */
    private val obstacleArt = ObstacleArt.load(resources,
        com.tinyrunners.school.shop.ShopCatalog.roadProps(com.tinyrunners.school.shop.ShopStore.ownedProps(context)))
    /** A's approved thorny branches, ground spikes and landing planks (docs/art/approved/). */
    private val hazardArt = HazardArt.load(resources, character.id)
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
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private var lastPhase = engine.phase
    /** Items picked up so far, to vibrate once per new item. */
    private var lastCollected = 0
    /** Coins earned so far this run (distance + road) and the "+1" coins rising above the friend. */
    private var shownCoins = 0
    private val coinPops = ArrayDeque<Float>()

    /** Current weather from Open-Meteo, or null to draw the default sky. Visual only. */
    var weather: WeatherReport? = null
    /** Shop decoration drawn behind the friend; looks only, never touches the physics. */
    var trail: com.tinyrunners.school.shop.TrailStyle = com.tinyrunners.school.shop.TrailStyle.NONE
        set(value) { field = value; invalidate() }

    /** Called once each time a run ends. */
    var onRunOver: (() -> Unit)? = null

    /** Called when the player pauses with the 정지 button (the run shows its rest card then, not the plain overlay). */
    var onStopTapped: (() -> Unit)? = null

    /** The rest card (PausePanel) is open over the run. */
    var restCard = false

    /** Dust at the feet and the landing shake (looks only). */
    private val feel = RunFeel()
    private var lastJumps = 0
    private var lastLands = 0
    private val storyPainter = RunStoryPainter(resources.displayMetrics.density)
    /** The story told during this run (speech bubble, subtitles, crossroads), or null for a run without one. */
    var story: RunStoryPlayer? = null
    private var toldChoice: Boolean? = null
    private var toldFinished = false
    /** Called once when the crossroads is decided (true = the upper route). */
    var onForkChosen: ((Boolean) -> Unit)? = null
    /** Called once when the chosen ending has been told to the end. */
    var onStoryFinished: (() -> Unit)? = null
    /** Called every frame of running (the story director checks distances here). */
    var onTick: (() -> Unit)? = null
    /** While a story talk is on screen: no pause/ready overlay and no top buttons under it. */
    var cinematic = false
    /** The last stretch: (metres when it began, metres of the goal), shown as a bar to the treasure. */
    var goal: Pair<Int, Int>? = null

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
            if (engine.phase == RunnerEngine.Phase.RUNNING) {
                feel.update(min(dt, 0.05f))
                feelEvents()
                story?.let { s ->
                    s.update(engine.meters, engine.forkAhead, engine.forkChoice, min(dt, 0.05f))
                    val chosen = s.choice
                    if (chosen != null && toldChoice == null) { toldChoice = chosen; onForkChosen?.invoke(chosen) }
                    if (s.finished && !toldFinished) { toldFinished = true; onStoryFinished?.invoke() }
                }
                onTick?.invoke()
            }
        }
        lastFrameNanos = frameTimeNanos
        if (engine.phase == RunnerEngine.Phase.READY && lastPhase != RunnerEngine.Phase.READY) {
            animTime = 0f; feel.clear(); lastJumps = 0; lastLands = 0
        }
        if (engine.phase == RunnerEngine.Phase.OVER && lastPhase != RunnerEngine.Phase.OVER) {
            com.tinyrunners.school.audio.Haptics.crash(this)
            onRunOver?.invoke()
        }
        if (engine.collected > lastCollected) com.tinyrunners.school.audio.Haptics.pickup(this)
        lastCollected = engine.collected
        lastPhase = engine.phase
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    /** Dust and shake for a new jump or landing since the last frame. */
    private fun feelEvents() {
        val c = engine.config
        val feetX = sx(c.playerX + c.playerSize / 2f)
        val px = spriteScale.toFloat()
        if (engine.jumpCount > lastJumps) feel.jump(feetX, sy(engine.floorY), px, DUST)
        if (engine.landCount > lastLands) feel.land(feetX, sy(engine.floorY), px, DUST, hard = engine.lastLandSpeed > c.jumpVelocity * 0.95f)
        lastJumps = engine.jumpCount; lastLands = engine.landCount
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
            buttonsVisible && stopRect.contains(x, y) -> {
                val wasRunning = engine.phase == RunnerEngine.Phase.RUNNING
                engine.pause()
                if (wasRunning) onStopTapped?.invoke()
            }
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

        drawWeatherBehind(canvas, w)
        for (o in engine.obstacles) drawObstacle(canvas, o)
        drawPickups(canvas)
        // The top bar is drawn before the runner, so a high jump shows over it instead of disappearing under it.
        drawHud(canvas, w)
        drawRunner(canvas)
        feel.draw(canvas)
        drawCoinPops(canvas)
        drawWeatherFront(canvas, w)
        if (engine.phase == RunnerEngine.Phase.RUNNING || engine.phase == RunnerEngine.Phase.PAUSED) drawStory(canvas, w)

        if (!cinematic) when (engine.phase) {
            RunnerEngine.Phase.READY -> overlay(canvas, w, h,
                context.getString(R.string.game_ready_title, character.name),
                context.getString(R.string.game_ready_hint), null)
            RunnerEngine.Phase.PAUSED -> if (!restCard) overlay(canvas, w, h,
                context.getString(R.string.game_paused),
                context.getString(R.string.game_paused_hint), null)
            RunnerEngine.Phase.OVER -> overlay(canvas, w, h,
                context.getString(R.string.game_over_title, character.name, engine.meters),
                context.getString(R.string.game_over_score, engine.score, engine.cleared),
                if (onRunOver == null && engine.canRestart) context.getString(R.string.game_over_hint) else null)
            RunnerEngine.Phase.RUNNING -> Unit
        }
        // Drawn last so the 진행 button stays clear above the pause overlay.
        goal?.let { drawGoal(canvas, w, it) }
        if (buttonsVisible && !cinematic && !restCard) {
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

    /**
     * Weather behind the play: washes over the scenery only (clouds grey, rain blue-grey, storm dark, fog a light white
     * haze, night blue), so the runner, obstacles, coins and items keep their own colours and stay easy to read; and
     * the snow settling on the ground as it falls.
     */
    private fun drawWeatherBehind(canvas: Canvas, w: Float) {
        val r = weather ?: return
        val full = height.toFloat()
        when (r.sky) {
            Sky.CLOUDY -> { fill.color = CLOUD_WASH; canvas.drawRect(0f, 0f, w, full, fill) }
            Sky.RAIN -> { fill.color = RAIN_WASH; canvas.drawRect(0f, 0f, w, full, fill) }
            Sky.STORM -> { fill.color = STORM_WASH; canvas.drawRect(0f, 0f, w, full, fill) }
            Sky.FOG -> { fill.color = FOG_WASH; canvas.drawRect(0f, 0f, w, full, fill) }
            Sky.SNOW, Sky.CLEAR -> Unit
        }
        if (!r.isDay) { fill.color = NIGHT_WASH; canvas.drawRect(0f, 0f, w, full, fill) }
        if (r.sky == Sky.SNOW) drawSettledSnow(canvas, w)
    }

    /** Weather in front of the play: rain, snowflakes, drifting fog and lightning. */
    private fun drawWeatherFront(canvas: Canvas, w: Float) {
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
                // A lightning flash now and then.
                if (r.sky == Sky.STORM && ((animTime % 5f) in 0f..0.12f || (animTime % 5f) in 0.22f..0.3f)) {
                    fill.color = Color.argb(110, 255, 255, 240); canvas.drawRect(0f, 0f, w, height.toFloat(), fill)
                }
            }
            Sky.SNOW -> drawSnowflakes(canvas, w, h)
            Sky.FOG -> drawFogDrift(canvas, w, h)
            Sky.CLEAR, Sky.CLOUDY -> Unit
        }
    }

    /** A small stable pseudo-random number in 0..1 for flake/puff/patch [i] and [salt]. */
    private fun noise(i: Int, salt: Int): Float {
        var x = i * 374761393 + salt * 668265263
        x = (x xor (x ushr 13)) * 1274126177
        return ((x xor (x ushr 16)) and 0xFFFF) / 65535f
    }

    /**
     * Snowflakes falling at their own speeds and swaying at their own pace (not one slant), each a little smaller
     * than 라일's eye: a square about 2.5 sprite pixels across, with a lighter centre.
     */
    private fun drawSnowflakes(canvas: Canvas, w: Float, h: Float) {
        val px = spriteScale.toFloat().coerceAtLeast(2f)
        for (i in 0 until SNOWFLAKES) {
            val size = (2.4f + noise(i, 1) * 0.9f) * px
            val fall = (34f + noise(i, 2) * 46f) * scale
            val swayAmp = (4f + noise(i, 3) * 14f) * scale
            val swaySpeed = 0.6f + noise(i, 4) * 1.6f
            val baseX = noise(i, 5) * (w + 60f)
            val y = ((noise(i, 6) * (h + size) + animTime * fall) % (h + size)) - size
            val sway = kotlin.math.sin(animTime * swaySpeed + noise(i, 7) * 6.28f) * swayAmp
            // The flakes drift slowly back past the runner as the road moves.
            val x = ((baseX - engine.distance * 0.18f * scale) % (w + 60f) + (w + 60f)) % (w + 60f) - 30f + sway
            fill.color = SNOW_EDGE; canvas.drawRect(x, y, x + size, y + size, fill)
            fill.color = Color.WHITE; canvas.drawRect(x + size * 0.2f, y + size * 0.2f, x + size * 0.8f, y + size * 0.8f, fill)
        }
    }

    /**
     * Snow lying on the ground: soft white patches that grow for the first half minute of the run and move with the
     * ground (the same travel as the running background).
     */
    private fun drawSettledSnow(canvas: Canvas, w: Float) {
        val amount = (0.45f + animTime / 30f).coerceAtMost(1f)
        val travel = engine.distance * scale * 0.25f
        val period = 64f * scale
        val first = kotlin.math.floor((travel - period) / period).toInt()
        val last = kotlin.math.floor((travel + w + period) / period).toInt()
        for (n in first..last) {
            if (noise(n, 11) < 0.3f) continue
            val width = (14f + noise(n, 12) * 34f) * scale * amount
            val x = n * period - travel + noise(n, 13) * period * 0.5f
            val rise = (3f + noise(n, 14) * 2.2f) * scale * amount
            fill.color = SNOW_SHADE; canvas.drawRect(x, groundY - rise + 1.2f * scale, x + width, groundY + 2.2f * scale, fill)
            fill.color = Color.WHITE; canvas.drawRect(x, groundY - rise, x + width, groundY + 1.2f * scale, fill)
            // A little heap in the middle.
            canvas.drawRect(x + width * 0.25f, groundY - rise * 1.7f, x + width * 0.7f, groundY, fill)
        }
    }

    /**
     * Fog: pale white puffs drifting across the road in front of the runner, like the clouds of the opening and menu,
     * so the way ahead is a little harder to see.
     */
    private fun drawFogDrift(canvas: Canvas, w: Float, h: Float) {
        val span = w + 200f * scale
        for (i in 0 until FOG_PUFFS) {
            val width = (110f + noise(i, 21) * 90f) * scale
            val height = (26f + noise(i, 22) * 22f) * scale
            val speed = (14f + noise(i, 23) * 18f) * scale
            val offset = (noise(i, 24) * span + animTime * speed + engine.distance * 0.35f * scale) % span
            val left = w - offset + 60f * scale
            val bottom = h - noise(i, 25) * 95f * scale
            fogPaint.alpha = (60 + noise(i, 26) * 50).toInt()
            val r = height / 2f
            canvas.drawRoundRect(left, bottom - height, left + width, bottom, r, r, fogPaint)
            canvas.drawRoundRect(left + width * 0.18f, bottom - height * 1.55f, left + width * 0.62f, bottom - height * 0.4f, r, r, fogPaint)
            canvas.drawRoundRect(left + width * 0.45f, bottom - height * 1.25f, left + width * 0.88f, bottom - height * 0.3f, r, r, fogPaint)
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
        if (art != null && o.kind == ObstacleKind.BOX) { art.drawBox(canvas, l, t, r, b, System.identityHashCode(o)); return }
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
            ObstacleKind.FLOAT -> hazardArt?.drawAir(canvas, l, t, r, b) ?: drawThornBar(canvas, l, t, r, b)
            ObstacleKind.SPIKES -> drawSpikeField(canvas, o)
            ObstacleKind.FORK -> drawFork(canvas, o)
        }
    }

    /** The story crossroads: a raised path on posts over safe ground, with an ↑ sign at its start. */
    private fun drawFork(canvas: Canvas, o: Obstacle) {
        val l = sx(o.x); val r = sx(o.x + o.width)
        val top = sy(o.plankTop); val bottom = sy(o.plankTop - engine.config.plankThickness)
        val ground = sy(0f)
        fill.color = PLANK_EDGE
        var post = l + 18f * scale
        while (post < r - 10f * scale) { canvas.drawRect(post, bottom, post + 4f * scale, ground, fill); post += 120f * scale }
        val art = hazardArt
        if (art != null) art.drawPlatform(canvas, l, top, r, engine.config.plankThickness * 2.2f * scale)
        else {
            canvas.drawRect(l, top, r, bottom, fill)
            val p = 2f * scale
            fill.color = PLANK; canvas.drawRect(l + p, top + p, r - p, bottom - p, fill)
            fill.color = PLANK_TOP; canvas.drawRect(l + p, top + p, r - p, top + p * 2f, fill)
        }
        // A little lavender flag marks the upper route.
        fill.color = PLANK_EDGE; canvas.drawRect(l + 6f * scale, top - 40f * scale, l + 9f * scale, top, fill)
        fill.color = FORK_FLAG; canvas.drawRect(l + 9f * scale, top - 40f * scale, l + 30f * scale, top - 26f * scale, fill)
    }

    /** The last stretch: "보물까지 32m" and a bar filling towards the goal, under the top bar. */
    private fun drawGoal(canvas: Canvas, w: Float, g: Pair<Int, Int>) {
        val d = resources.displayMetrics.density
        val (from, to) = g
        val left = (to - engine.meters).coerceAtLeast(0)
        val t = ((engine.meters - from).toFloat() / (to - from).coerceAtLeast(1)).coerceIn(0f, 1f)
        val bw = w * 0.34f; val top = goRect.bottom + 8f * d
        val box = RectF((w - bw) / 2f, top, (w + bw) / 2f, top + 34f * d)
        fill.color = Color.argb(225, 255, 248, 231); canvas.drawRoundRect(box, 17f * d, 17f * d, fill)
        val bar = RectF(box.left + 12f * d, box.bottom - 10f * d, box.right - 12f * d, box.bottom - 6f * d)
        fill.color = Color.rgb(230, 218, 196); canvas.drawRect(bar, fill)
        fill.color = Color.rgb(236, 196, 84); canvas.drawRect(bar.left, bar.top, bar.left + bar.width() * t, bar.bottom, fill)
        text.textSize = 14f * d; text.textAlign = Paint.Align.CENTER; text.color = INK
        canvas.drawText(context.getString(R.string.story_goal_left, left), box.centerX(), box.top + 19f * d, text)
        text.textAlign = Paint.Align.LEFT
    }

    /** The running story: a bubble over the friend, the narrator under the top bar, or the crossroads question. */
    private fun drawStory(canvas: Canvas, w: Float) {
        val s = story ?: return
        if (cinematic) return
        val d = resources.displayMetrics.density
        val top = goRect.bottom + 8f * d + if (goal != null) 42f * d else 0f
        // (The crossroads question is a paused choice with two buttons now — ForkChoiceView in GameActivity.)
        val line = s.current ?: return
        if (line.narrator) { storyPainter.subtitle(canvas, line.text, top, w); return }
        val c = engine.config
        val headX = sx(c.playerX + c.playerSize / 2f)
        val headY = sy(engine.playerY + c.playerSize * 0.75f)
        storyPainter.bubble(canvas, line.text, headX, headY, w, minTop = top)
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
        val art = hazardArt
        if (art != null) {
            art.drawSpikes(canvas, l, ground, r, tip)
            val pl = sx(o.plankLeft); val pr = sx(o.plankRight); val pt = sy(o.plankTop)
            // Ropes from above, then the plank with its flat top at the landing height.
            fill.color = ROPE
            val rope = 2f * scale
            for (rx in floatArrayOf(pl + 14f * scale, pr - 14f * scale - rope)) canvas.drawRect(rx, sy(o.plankTop + 90f), rx + rope, pt + 2f * scale, fill)
            art.drawPlatform(canvas, pl, pt, pr, engine.config.plankThickness * 2.2f * scale)
            return
        }
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
        for (p in engine.pickups) {
            if (p.taken) continue
            val bob = kotlin.math.sin(animTime * 4f + p.x * 0.02f) * 4f
            val size = p.size * scale
            if (p.coin) { CoinArt.draw(canvas, sx(p.x) + size / 2f, sy(p.bottom + p.size / 2f + bob), size * 0.42f, spin = animTime + p.x * 0.01f); continue }
            val item = questItem ?: continue
            com.tinyrunners.school.quest.ItemArt.draw(canvas, item, sx(p.x), sy(p.bottom + p.size + bob), size)
        }
    }

    /** Coins this run: one per 25 m (as the shop counts them) plus coins picked up on the road. */
    private fun runCoins() = com.tinyrunners.school.shop.ShopRules.coinsForRun(engine.meters) + engine.coins

    /** Every coin earned rises from the friend's head as "+1" and fades, so earning is visible while running. */
    private fun drawCoinPops(canvas: Canvas) {
        val now = runCoins()
        if (engine.phase == RunnerEngine.Phase.READY) { shownCoins = 0; coinPops.clear() }
        while (shownCoins < now) { shownCoins++; coinPops.addLast(animTime) }
        while (coinPops.isNotEmpty() && animTime - coinPops.first() > POP_SECONDS) coinPops.removeFirst()
        val c = engine.config
        val cx = sx(c.playerX + c.playerSize / 2f)
        val head = sy(engine.playerY + c.playerSize)
        val d = resources.displayMetrics.density
        coinPops.forEachIndexed { i, start ->
            val t = ((animTime - start) / POP_SECONDS).coerceIn(0f, 1f)
            val y = head - (14f + 56f * t) * d - i * 6f * d
            val alpha = (255 * (1f - t * t * t)).toInt()
            CoinArt.draw(canvas, cx - 12f * d, y, 13f * d, spin = animTime * 3f, alpha = alpha)
            text.textSize = 20f * d; text.typeface = Typeface.DEFAULT_BOLD; text.textAlign = Paint.Align.LEFT
            text.color = COIN_INK; text.alpha = alpha
            canvas.drawText("+1", cx + 4f * d, y + 7f * d, text)
            text.color = INK; text.alpha = 255; text.typeface = Typeface.DEFAULT
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
        // Coins earned this run, after the items.
        run {
            val itemsText = questItem?.let { context.getString(R.string.item_hud, engine.collected) }
            var x = 16f * d + text.measureText(hud) + 14f * d
            if (itemsText != null) x += 22f * d + text.measureText(itemsText) + 14f * d
            CoinArt.draw(canvas, x + 9f * d, 49f * d, 9f * d, spin = 0f)
            canvas.drawText(context.getString(R.string.item_hud, runCoins()), x + 22f * d, 54f * d, text)
        }
        text.textSize = 11f * d
        text.alpha = 150
        if (sprites == null) {
            canvas.drawText(context.getString(R.string.game_placeholder_art), 16f * d, 72f * d, text)
        }
        weather?.let {
            text.textAlign = Paint.Align.RIGHT
            val right = if (buttonsVisible) stopRect.left - 12f * d else w - 16f * d
            val time = context.getString(if (it.isDay) R.string.weather_day else R.string.weather_night)
            val label = when {
                it.chosen && it.temperatureC.isNaN() -> context.getString(R.string.weather_hud_chosen, context.getString(it.sky.label), time)
                it.chosen -> context.getString(R.string.weather_hud_mixed, context.getString(it.city.label), context.getString(it.sky.label), time, it.temperatureC)
                else -> context.getString(R.string.weather_hud, context.getString(it.city.label), context.getString(it.sky.label), it.temperatureC)
            }
            canvas.drawText(label, right, 40f * d, text)
            // Under it, how hard today's road is (clear day easiest, a stormy night hardest).
            canvas.drawText(context.getString(R.string.difficulty_hud, context.getString(Difficulty.of(it).label)), right, 62f * d, text)
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
        private const val POP_SECONDS = 1.1f
        private val COIN_INK = Color.rgb(140, 96, 20)
        /** Sky above the road: the head at the jump ceiling (RunnerConfig.ceiling + the body) fits on screen. */
        const val WORLD_HEIGHT = 385f
        const val MIN_WORLD_WIDTH = 640f
        const val GROUND_THICKNESS = 48f
        val SKY = Color.rgb(250, 246, 236)
        val SKY_CLOUDY = Color.rgb(232, 234, 228)
        val SKY_RAIN = Color.rgb(214, 222, 226)
        val SKY_SNOW = Color.rgb(238, 242, 246)
        /** Soft lavender dusk: keeps dark text readable and does not look like an overcast day. */
        val NIGHT = Color.rgb(214, 210, 236)
        val RAIN_COLOR = Color.argb(150, 96, 120, 150)
        val FOG_WASH = Color.argb(70, 255, 255, 255)
        val SNOW_EDGE = Color.rgb(214, 226, 240)
        val SNOW_SHADE = Color.rgb(196, 212, 230)
        const val SNOWFLAKES = 70
        const val FOG_PUFFS = 7
        val CLOUD_WASH = Color.argb(55, 120, 126, 134)
        val RAIN_WASH = Color.argb(60, 70, 86, 110)
        val STORM_WASH = Color.argb(105, 40, 44, 60)
        /** Night over the scenery only (the runner and obstacles keep their colours), lighter than before. */
        val NIGHT_WASH = Color.argb(95, 18, 26, 70)
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
        /** Pale earth dust at the feet, and the lavender flag on a story crossroads. */
        val DUST = Color.rgb(222, 204, 168)
        val FORK_FLAG = Color.rgb(176, 148, 220)
        val PLANK_EDGE = Color.rgb(98, 64, 42)
        val ROPE = Color.rgb(150, 112, 74)
        val BUTTON_ON = Color.rgb(48, 83, 65)
        val SCRIM = Color.argb(170, 250, 246, 236)
    }
}

/** A gold coin that turns slowly (its width shrinks and grows), with a darker rim and a small shine. */
internal object CoinArt {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val oval = android.graphics.RectF()

    fun draw(canvas: Canvas, cx: Float, cy: Float, radius: Float, spin: Float, alpha: Int = 255) {
        val squeeze = 0.35f + 0.65f * kotlin.math.abs(kotlin.math.cos(spin * 2.2f))
        val rx = radius * squeeze
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(196, 140, 30); paint.alpha = alpha
        oval.set(cx - rx, cy - radius, cx + rx, cy + radius); canvas.drawOval(oval, paint)
        paint.color = Color.rgb(250, 204, 72); paint.alpha = alpha
        oval.set(cx - rx * 0.78f, cy - radius * 0.78f, cx + rx * 0.78f, cy + radius * 0.78f); canvas.drawOval(oval, paint)
        paint.color = Color.rgb(255, 244, 190); paint.alpha = alpha
        oval.set(cx - rx * 0.45f, cy - radius * 0.6f, cx - rx * 0.1f, cy - radius * 0.1f); canvas.drawOval(oval, paint)
    }
}
