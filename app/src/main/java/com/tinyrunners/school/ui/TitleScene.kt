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
     * The picture for the time of day: the sunny meadow by day (bg_opening_wide, drawn wide already), the lavender
     * village at dusk by night (A's separated pack). The night picture is 16:9 widened with blurred side bands; only
     * its original middle is shown, filling the screen and losing a little sky at the top instead.
     */
    fun background(context: Context): View =
        if (isNight()) CoverPicture(context, R.drawable.bg_v2_login_wide, sideInset = 0.1f)
        else CoverPicture(context, R.drawable.bg_opening_wide, sideInset = 0f)

    /** The screen's short side in pixels: every size here is a share of it. */
    fun unit(context: Context): Int = context.resources.displayMetrics.let { minOf(it.widthPixels, it.heightPixels) }

    /** The logo on a cream plaque with [tagline] in a lavender pill under it. */
    fun header(context: Context, tagline: String): LinearLayout {
        val h = unit(context)
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding((h * 0.05f).roundToInt(), (h * 0.025f).roundToInt(), (h * 0.05f).roundToInt(), (h * 0.03f).roundToInt())
            background = GradientDrawable().apply {
                setColor(Color.argb(232, 255, 248, 232)); cornerRadius = h * 0.04f
                setStroke((h * 0.004f).roundToInt().coerceAtLeast(2), Color.rgb(214, 186, 132))
            }
            elevation = h * 0.01f
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ui_title_logo); scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = context.getString(R.string.app_name)
            }, LinearLayout.LayoutParams((h * 0.15f * 650 / 140).roundToInt(), (h * 0.15f).roundToInt()))
            addView(TextView(context).apply {
                text = tagline; setTextColor(Color.rgb(48, 52, 40)); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
                includeFontPadding = false; setTextSize(TypedValue.COMPLEX_UNIT_PX, h * 0.032f)
                setPadding((h * 0.05f).roundToInt(), 0, (h * 0.05f).roundToInt(), 0)
                background = GradientDrawable().apply {
                    setColor(Color.rgb(205, 182, 234)); cornerRadius = h * 0.04f
                    setStroke((h * 0.003f).roundToInt().coerceAtLeast(2), Color.rgb(176, 150, 214))
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (h * 0.065f).roundToInt()).apply {
                topMargin = (h * 0.012f).roundToInt()
            })
        }
    }

    /** A centred button on one of the separated pack's skins, [heightShare] of the short side tall. */
    fun button(context: Context, text: String, primary: Boolean, width: Float, heightShare: Float, click: () -> Unit): Button {
        val h = unit(context)
        return Button(context).apply {
            this.text = text; isAllCaps = false; stateListAnimator = null; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setTextColor(if (primary) Color.rgb(255, 253, 241) else Color.rgb(56, 82, 62))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, h * heightShare * 0.42f)
            setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = android.graphics.drawable.StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed),
                    context.getDrawable(if (primary) R.drawable.ui_v2_btn_primary_pressed else R.drawable.ui_v2_btn_menu_pressed))
                addState(intArrayOf(), context.getDrawable(if (primary) R.drawable.ui_v2_btn_primary else R.drawable.ui_v2_btn_menu))
            }
            layoutParams = LinearLayout.LayoutParams((h * width).roundToInt(), (h * heightShare).roundToInt())
            setOnClickListener { click() }
        }
    }

    /** The six friends breathing in a row, centred, for the bottom of the screen. */
    fun friends(context: Context): LinearLayout {
        val h = unit(context)
        val size = (h * 0.15f).roundToInt()
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            CharacterRepository(context).characters.forEachIndexed { i, f ->
                addView(OpeningFriend(context, f.id, i), LinearLayout.LayoutParams(size, size).apply {
                    if (i > 0) marginStart = (h * 0.05f).roundToInt()
                })
            }
        }
    }

    /** The whole screen: picture, the centred column (header and [buttons]) and the friends at the bottom. */
    fun screen(context: Context, tagline: String, buttons: List<View>, gap: Float = 0.022f): FrameLayout {
        val h = unit(context)
        return FrameLayout(context).apply {
            setBackgroundColor(Color.rgb(246, 236, 214))
            addView(background(context), FrameLayout.LayoutParams(-1, -1))
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, (h * 0.05f).roundToInt(), 0, 0)
                addView(header(context, tagline), LinearLayout.LayoutParams(-2, -2))
                buttons.forEachIndexed { i, b ->
                    val lp = (b.layoutParams as? LinearLayout.LayoutParams) ?: LinearLayout.LayoutParams(-2, -2)
                    lp.topMargin = (h * if (i == 0) 0.045f else gap).roundToInt()
                    addView(b, lp)
                }
            }
            addView(column, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
            addView(friends(context), FrameLayout.LayoutParams(-1, (h * 0.15f).roundToInt(), Gravity.BOTTOM).apply {
                bottomMargin = (h * 0.04f).roundToInt()
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
