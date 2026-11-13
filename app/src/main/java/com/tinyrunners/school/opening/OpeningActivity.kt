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

import android.graphics.Rect

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
import com.tinyrunners.school.R
import com.tinyrunners.school.auth.LoginActivity
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.ui.ScenePainter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * Landscape title screen shown on every launch: A's meadow without the card (bg_opening_wide: bg_login_scene with
 * its patch seams smoothed), the card's title and a "시작하기" button together in the middle, and the six friends
 * standing apart along the grass (drawn from their sprites, breathing). The button Pressing it raises the login card over this screen (LoginActivity is a popup laid out on the same canvas).
 */
class OpeningActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val frame = com.tinyrunners.school.ui.MockupFrame(this, R.drawable.bg_opening_wide)
        // The title from the login card (Tiny Runners, leaves and tagline), just above the start button.
        frame.place(android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ui_opening_title)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.app_name) + ", " + getString(R.string.opening_tagline)
        }, 496, 215, 1176, 437)
        // The start button under the title, between the two groups of friends.
        frame.place(com.tinyrunners.school.ui.MockupFrame.partButton(this, frame, R.drawable.ui_login_btn_primary,
            getString(R.string.opening_start), Color.parseColor("#FFF8E7"), 44f) { onButton() }, 583, 475, 1090, 565)
        // Six friends spread along the grass, three each side of the path, with room between them.
        val friends = CharacterRepository(this).characters
        FRIEND_X.forEachIndexed { i, cx ->
            val id = friends.getOrNull(i)?.id ?: return@forEachIndexed
            frame.place(OpeningFriend(this, id, i), cx - 85, FRIEND_FEET - 170, cx + 85, FRIEND_FEET)
        }
        setContentView(frame)
        hideSystemBars()
    }

    /** Always the login popup first; it offers "continue with this account" when already signed in. */
    private fun onButton() {
        startActivity(Intent(this, LoginActivity::class.java))
        // The popup animates itself (the card rises); no window transition.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private companion object {
        /** Friend centres and feet line on the 1672×941 canvas. */
        val FRIEND_X = intArrayOf(110, 300, 490, 1180, 1370, 1560)
        const val FRIEND_FEET = 805
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

}
/** One friend on the opening meadow: whole-pixel sprite on its feet, a soft shadow, breathing between idle frames. */
@SuppressLint("ViewConstructor")
internal class OpeningFriend(context: Context, id: String, private val index: Int) : View(context) {
    private val sprites = CharacterSprites.load(resources, id)
    private val pixel = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 40, 50, 20) }
    private val dst = android.graphics.RectF()
    private var frame = 0
    private val tick = object : Runnable {
        override fun run() { frame++; invalidate(); postDelayed(this, IDLE_MS) }
    }

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    // Friends breathe a little out of step with each other.
    override fun onAttachedToWindow() { super.onAttachedToWindow(); postDelayed(tick, IDLE_MS + index * 90L) }
    override fun onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        val s = sprites ?: return
        val px = max(1, height / CharacterSprites.SIZE).toFloat()
        val size = CharacterSprites.SIZE * px
        val cx = width / 2f
        val feet = height.toFloat() - px
        canvas.drawOval(cx - size * 0.28f, feet - px * 2.5f, cx + size * 0.28f, feet + px * 1.5f, shadow)
        val left = (cx - CharacterSprites.CENTER_X * px).roundToInt().toFloat()
        val top = feet - CharacterSprites.FOOT_EDGE_Y * px - if (frame % 2 == 0) 0f else px
        dst.set(left, top, left + size, top + size)
        canvas.drawBitmap(s[if (frame % 2 == 0) SpriteFrame.IDLE_0 else SpriteFrame.IDLE_1], null, dst, pixel)
    }

    private companion object { const val IDLE_MS = 560L }
}

/** Draws A's opening artwork with crisp pixels (centre-crop), or the placeholder line-up of all six friends. Shared with the hub. */
@SuppressLint("ViewConstructor")
internal class OpeningBackground(
    context: Context,
    private val showTitle: Boolean = true,
    /** Title baseline as a fraction of the height; the hub raises it to make room for four buttons. */
    private val titleY: Float = 0.2f,
) : View(context) {
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
        // Sky, clouds, hills and ground are shared with the menu screens (ui/Scenery.kt).
        scene.draw(canvas, w, h, ground, h * 0.17f)
        postInvalidateDelayed(ScenePainter.FRAME_MS)

        if (showTitle) drawTitle(canvas, w, h)
        drawFriends(canvas, w, h, ground)
    }

    private fun drawTitle(canvas: Canvas, w: Int, h: Int) {
        text.color = INK
        text.textSize = h * 0.09f
        canvas.drawText(context.getString(R.string.app_name), w / 2f, h * titleY, text)
        text.textSize = h * 0.035f
        text.color = MUTED
        canvas.drawText(context.getString(R.string.opening_tagline), w / 2f, h * (titleY + 0.07f), text)
    }

    private fun drawFriends(canvas: Canvas, w: Int, h: Int, ground: Float) {
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

    private val scene = ScenePainter()

    private companion object {
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
