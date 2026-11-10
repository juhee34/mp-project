package com.tinyrunners.school.opening

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.FrameLayout
import com.tinyrunners.school.MainActivity
import com.tinyrunners.school.R
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.auth.LoginActivity
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterRepository
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Landscape title screen shown on every launch: the friends' artwork fills the screen and one button sits in the
 * centre. Signed out it says "로그인" and opens the login form; signed in (or guest) it says "시작하기" and opens
 * the friend selection.
 *
 * The artwork is A's `res/drawable-nodpi/opening_background.png` when present (see docs/OPENING_SPEC.md);
 * until then a placeholder with all six friends' idle sprites is drawn.
 */
class OpeningActivity : Activity() {
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        root.addView(OpeningBackground(this), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        button = Button(this).apply {
            textSize = 20f
            isAllCaps = false
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            minHeight = dp(56)
            background = GradientDrawable().apply {
                setColor(LEAF)
                cornerRadius = dp(28).toFloat()
                setStroke(dp(3), Color.WHITE)
            }
            stateListAnimator = null
            setOnClickListener { onButton() }
        }
        root.addView(button, FrameLayout.LayoutParams(dp(220), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        setContentView(root)
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        // Re-evaluated on return, e.g. after signing out from settings or backing out of the login form.
        button.setText(if (AuthService.hasSession(this)) R.string.opening_start else R.string.opening_login)
    }

    private fun onButton() {
        if (AuthService.hasSession(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private companion object { val LEAF = Color.rgb(48, 83, 65) }
}

/** Draws A's opening artwork with crisp pixels (centre-crop), or the placeholder line-up of all six friends. */
@SuppressLint("ViewConstructor")
private class OpeningBackground(context: Context) : View(context) {
    private val art: Bitmap? = loadArt(context)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false; isDither = false }
    private val fill = Paint()
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    private val friends = CharacterRepository(context).characters.mapNotNull { CharacterSprites.load(resources, it.id) }
    private val dst = Rect()

    override fun onDraw(canvas: Canvas) {
        val w = width; val h = height
        val image = art
        if (image != null) {
            // Centre-crop: fill the screen, keep the middle of the picture.
            val scale = max(w.toFloat() / image.width, h.toFloat() / image.height)
            val dw = (image.width * scale).roundToInt(); val dh = (image.height * scale).roundToInt()
            dst.set((w - dw) / 2, (h - dh) / 2, (w - dw) / 2 + dw, (h - dh) / 2 + dh)
            canvas.drawBitmap(image, null, dst, pixel)
            return
        }
        drawPlaceholder(canvas, w, h)
    }

    private fun drawPlaceholder(canvas: Canvas, w: Int, h: Int) {
        val ground = h * 0.78f
        val px = max(2f, (h / 90f).roundToInt().toFloat())          // one "pixel" of the scenery
        val hillBase = ground - h * 0.17f

        // Sky: cream at the top fading into pale green just above the hills, so there is no hard edge.
        if (skyShaderHeight != h) {
            sky.shader = LinearGradient(0f, 0f, 0f, hillBase, SKY, SKY_LOW, Shader.TileMode.CLAMP)
            skyShaderHeight = h
        }
        canvas.drawRect(0f, 0f, w.toFloat(), ground, sky)

        drawClouds(canvas, w, h, px)

        // One continuous band of hills across the whole width, with a stepped pixel skyline on top.
        fill.color = HILL
        canvas.drawRect(0f, hillBase, w.toFloat(), ground, fill)
        var x = 0f
        while (x < w) {
            val bump = ((sin(x / (w * 0.11f)) + 1f) * 0.5f * 7f + (sin(x / (w * 0.043f) + 1.3f) + 1f) * 2f).toInt()
            canvas.drawRect(x, hillBase - bump * px, x + px * 4, hillBase, fill)
            x += px * 4
        }
        fill.color = GROUND; canvas.drawRect(0f, ground, w.toFloat(), h.toFloat(), fill)
        fill.color = GRASS; canvas.drawRect(0f, ground, w.toFloat(), ground + h * 0.015f, fill)

        text.color = INK
        text.textSize = h * 0.09f
        canvas.drawText(context.getString(R.string.app_name), w / 2f, h * 0.2f, text)
        text.textSize = h * 0.035f
        text.color = MUTED
        canvas.drawText(context.getString(R.string.opening_tagline), w / 2f, h * 0.27f, text)

        // Six friends in a row on the ground, leaving the centre clear for the button.
        if (friends.isEmpty()) return
        val scale = max(1, (h * 0.22f / CharacterSprites.SIZE).roundToInt())
        val size = CharacterSprites.SIZE * scale
        val half = friends.size / 2
        val gap = min(size * 1.1f, (w * 0.38f) / half)
        friends.forEachIndexed { i, sprites ->
            val slot = if (i < half) i - half else i - half + 1   // ..., -2, -1 | +1, +2, ...
            val cx = w / 2f + slot * gap + (if (slot < 0) -w * 0.04f else w * 0.04f)
            val left = (cx - CharacterSprites.CENTER_X * scale).roundToInt()
            val top = (ground - CharacterSprites.FOOT_EDGE_Y * scale).roundToInt()
            dst.set(left, top, left + size, top + size)
            canvas.drawBitmap(sprites[SpriteFrame.IDLE_0], null, dst, pixel)
        }
    }

    /**
     * Blocky pixel clouds drifting slowly to the right. Each cloud is a small grid of on/off cells scaled by
     * [px]; a slightly darker underside row gives them a soft pixel-art shading.
     */
    private fun drawClouds(canvas: Canvas, w: Int, h: Int, px: Float) {
        val seconds = (System.nanoTime() - startNanos) / 1_000_000_000f
        val cell = px * 2
        CLOUDS.forEach { (shape, yFrac, xFrac, speed) ->
            val cloudW = shape[0].length * cell
            val span = w + cloudW
            val left = ((xFrac * w + seconds * speed * px) % span) - cloudW
            val top = h * yFrac
            shape.forEachIndexed { row, line ->
                fill.color = if (row == shape.lastIndex) CLOUD_SHADE else CLOUD
                line.forEachIndexed { col, c ->
                    if (c == '#') canvas.drawRect(left + col * cell, top + row * cell, left + (col + 1) * cell, top + (row + 1) * cell, fill)
                }
            }
        }
        // Keep the clouds moving; a light 15 fps redraw is enough for this slow drift.
        postInvalidateDelayed(66)
    }

    private val sky = Paint()
    private var skyShaderHeight = -1
    private val startNanos = System.nanoTime()

    private data class Cloud(val shape: List<String>, val yFrac: Float, val xFrac: Float, val speed: Float)

    private companion object {
        val SKY = Color.rgb(250, 246, 236)
        /** Sky colour just above the hills: halfway between cream and the hill green. */
        val SKY_LOW = Color.rgb(236, 240, 226)
        val CLOUD = Color.rgb(255, 255, 252)
        val CLOUD_SHADE = Color.rgb(228, 232, 226)
        val CLOUDS = listOf(
            Cloud(listOf("...####.....", ".########...", "###########.", ".#########.."), 0.08f, 0.05f, 6f),
            Cloud(listOf("..###...", ".######.", "########"), 0.33f, 0.62f, 4f),
            Cloud(listOf("....###......", "..########...", ".###########.", "#############", ".###########."), 0.13f, 0.78f, 5f),
            Cloud(listOf(".####..", "#######", ".#####."), 0.42f, 0.30f, 3f),
        )
        val HILL = Color.rgb(221, 232, 214)
        val GROUND = Color.rgb(196, 170, 128)
        val GRASS = Color.rgb(122, 160, 110)
        val INK = Color.rgb(43, 59, 47)
        val MUTED = Color.rgb(79, 96, 84)

        @SuppressLint("DiscouragedApi")
        fun loadArt(context: Context): Bitmap? {
            // Looked up by name so the app builds before A adds the file.
            val id = context.resources.getIdentifier("opening_background", "drawable", context.packageName)
            if (id == 0) return null
            return BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false })
        }
    }
}
