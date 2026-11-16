package com.tinyrunners.school.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.opening.OpeningFriend
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * The shared look of the opening and the menu: a day or night picture filling the screen, the title logo (the green
 * serif "Tiny Runners" with leaves, from A's login card) on a cream plaque with a lavender pill under it, the buttons
 * in the middle, and the six friends in a row on the ground. Everything is centred and sized from the screen height,
 * so it looks the same on 16:9 and 20:9 phones.
 */
object TitleScene {
    /** Night from 19:00 to 05:59 on the phone's clock. */
    fun isNight(now: Calendar = Calendar.getInstance()): Boolean = now.get(Calendar.HOUR_OF_DAY).let { it < 6 || it >= 19 }

    /**
     * The picture for the time of day: the village in daylight by day (bg_title_day), the same village in lavender dusk by
     * night (bg_title_night), both from the user (2026-11-16). They are 16:9: they fill the screen, losing a little sky.
     */
    fun background(context: Context): View =
        if (isNight()) CoverPicture(context, R.drawable.bg_title_night, sideInset = 0f)
        else CoverPicture(context, R.drawable.bg_title_day, sideInset = 0f)

    /** Height of the title line as a share of the short side. */
    private const val LOGO = 0.21f
    /** Height of each friend (a whole number of sprite pixels is drawn inside). */
    private const val FRIEND = 0.21f

    /** The screen's short side in pixels: every size here is a share of it. */
    fun unit(context: Context): Int = context.resources.displayMetrics.let { minOf(it.widthPixels, it.heightPixels) }

    /** The title "Tiny Runners" in plain white letters with [tagline] in white under it, both straight on the picture. */
    fun header(context: Context, tagline: String, scale: Float = 1f): LinearLayout {
        val h = unit(context) * scale
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            addView(TextView(context).apply {
                setText(R.string.app_name); setTextColor(WHITE); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
                includeFontPadding = false; setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.14f)
                setShadowLayer(h * 0.012f, 0f, h * 0.004f, SHADOW)
                if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (h * LOGO).roundToInt()))
            addView(TextView(context).apply {
                text = tagline; setTextColor(WHITE); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
                includeFontPadding = false; setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.042f)
                setShadowLayer(h * 0.008f, 0f, h * 0.003f, SHADOW)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (h * 0.07f).roundToInt()))
        }
    }

    private val WHITE = Color.rgb(255, 251, 240)
    private val SHADOW = Color.argb(150, 40, 34, 52)

    /** A centred button on one of the separated pack's skins, [heightShare] of the short side tall. */
    fun button(context: Context, text: String, primary: Boolean, width: Float, heightShare: Float, click: () -> Unit): Button {
        val h = unit(context)
        return Button(context).apply {
            this.text = text; isAllCaps = false; stateListAnimator = null; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setTextColor(if (primary) Color.WHITE else Color.rgb(56, 82, 62))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, h * heightShare * 0.42f)
            setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = if (primary) {
                // 시작하기: a soft lavender, like the blanket and the book in both pictures, with a pale rim.
                fun pill(fill: Int) = GradientDrawable().apply {
                    setColor(fill); cornerRadius = h * heightShare / 2f
                    setStroke((h * 0.005f).roundToInt().coerceAtLeast(2), Color.argb(230, 246, 238, 255))
                }
                android.graphics.drawable.StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_pressed), pill(Color.rgb(132, 106, 186)))
                    addState(intArrayOf(), pill(Color.rgb(156, 128, 206)))
                }
            } else android.graphics.drawable.StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), context.getDrawable(R.drawable.ui_v2_btn_menu_pressed))
                addState(intArrayOf(), context.getDrawable(R.drawable.ui_v2_btn_menu))
            }
            if (primary) setShadowLayer(h * 0.006f, 0f, h * 0.002f, Color.argb(90, 50, 30, 80))
            layoutParams = LinearLayout.LayoutParams((h * width).roundToInt(), (h * heightShare).roundToInt())
            setOnClickListener { click() }
        }
    }

    /** The six friends breathing in a row, centred, for the bottom of the screen. */
    fun friends(context: Context): LinearLayout {
        val h = unit(context)
        val size = (h * FRIEND).roundToInt()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            CharacterRepository(context).characters.forEachIndexed { i, f ->
                addView(OpeningFriend(context, f.id, i), LinearLayout.LayoutParams(size, size).apply {
                    if (i > 0) marginStart = (h * 0.05f).roundToInt()
                })
            }
        }
    }

    /**
     * The whole screen: picture, the column (header and [buttons]) in the middle and the friends at the bottom. With
     * [aboveFriends] the column is centred in the space above the friends (a tall column), otherwise on the screen.
     */
    fun screen(context: Context, tagline: String, buttons: List<View>, gap: Float = 0.022f, firstGap: Float = 0.045f,
               titleScale: Float = 1f, aboveFriends: Boolean = false): FrameLayout {
        val h = unit(context)
        return FrameLayout(context).apply {
            setBackgroundColor(Color.rgb(246, 236, 214))
            addView(background(context), FrameLayout.LayoutParams(-1, -1))
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                addView(header(context, tagline, titleScale), LinearLayout.LayoutParams(-2, -2))
                buttons.forEachIndexed { i, b ->
                    val lp = (b.layoutParams as? LinearLayout.LayoutParams) ?: LinearLayout.LayoutParams(-2, -2)
                    lp.topMargin = (h * if (i == 0) firstGap else gap).roundToInt()
                    addView(b, lp)
                }
            }
            addView(column, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER).apply {
                // FrameLayout centres and then shifts by the margin, so half the friends' band centres it in the space above them.
                if (aboveFriends) bottomMargin = (h * (FRIEND + 0.035f) / 2f).roundToInt()
            })
            addView(friends(context), FrameLayout.LayoutParams(-1, (h * FRIEND).roundToInt(), Gravity.BOTTOM).apply {
                bottomMargin = (h * 0.035f).roundToInt()
            })
        }
    }
}

/**
 * A picture that fills the view (cut evenly from the sides when wider, from the top when taller, so the ground
 * always shows). [sideInset] leaves out that share of each side first (the blurred bands of a widened picture).
 */
@SuppressLint("ViewConstructor")
class CoverPicture(context: Context, res: Int, private val sideInset: Float) : View(context) {
    private val art: Bitmap? = BitmapFactory.decodeResource(resources, res, BitmapFactory.Options().apply { inScaled = false })
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val src = Rect()
    private val dst = RectF()

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        val art = art ?: return
        if (width == 0 || height == 0) return
        val left = art.width * sideInset; val right = art.width * (1 - sideInset)
        val aw = right - left; val ah = art.height.toFloat()
        val k = maxOf(width / aw, height / ah)
        val sw = width / k; val sh = height / k
        val x0 = left + (aw - sw) / 2f
        val y0 = ah - sh   // keep the bottom (the ground), lose sky at the top
        src.set(x0.roundToInt(), y0.roundToInt(), (x0 + sw).roundToInt(), ah.roundToInt())
        dst.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(art, src, dst, paint)
    }
}
