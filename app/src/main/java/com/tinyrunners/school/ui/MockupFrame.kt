package com.tinyrunners.school.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import kotlin.math.roundToInt

/**
 * A screen laid out on A's approved mockup canvas (docs/art/ui-screens/README.md, layout.json): 1672×941 reference
 * pixels, scaled by min(width/1672, height/941) and centred. The wide background (2091×941, the original picture
 * in its middle 1672 px) fills the height and is centred, so parts placed with [place] land exactly on it on 16:9
 * and 20:9 phones. Children added without [place] fill the whole frame. With [backgroundRes] 0 nothing is drawn behind
 * the parts (a popup laid out on the same canvas as the screen under it). With [cover] the canvas fills the screen
 * instead (cropped top and bottom on 20:9), for a 16:9 picture that cannot be widened.
 */
open class MockupFrame(context: Context, backgroundRes: Int, private val cover: Boolean = false) : FrameLayout(context) {
    private val art = if (backgroundRes == 0) null else context.getDrawable(backgroundRes)
    private val paint = Paint()

    /** Scale from reference pixels to screen pixels, and where the reference canvas starts. */
    var scale = 1f; private set
    private var offX = 0f
    private var offY = 0f

    init { setWillNotDraw(false) }

    /**
     * When set, the canvas is laid over [anchor] (usually the whole screen) instead of this view, so parts land on the
     * picture behind the screen even when this frame sits below a top bar. Parts outside this view are clipped.
     */
    var anchor: View? = null

    /**
     * When true, parts spread over the whole width on phones wider than 16:9 (positions and widths use the width's
     * own scale; heights and text keep [scale]), instead of keeping a 16:9 block in the middle.
     */
    var stretchX = false
    private var sx = 1f

    private class Ref(val x0: Int, val y0: Int, val x1: Int, val y1: Int)

    /** Adds [view] at a rect given in the 1280×720 coordinates of A's separated design pack. */
    fun placeV2(view: View, x0: Int, y0: Int, x1: Int, y1: Int) = place(view, v2(x0), v2(y0), v2(x1), v2(y1))

    /** Adds [view] at the reference rect [x0, y0, x1, y1]. */
    fun place(view: View, x0: Int, y0: Int, x1: Int, y1: Int) {
        view.setTag(TAG, Ref(x0, y0, x1, y1))
        addView(view, LayoutParams(0, 0))
    }

    /** Moves a view added with [place] to a new reference rect. */
    fun move(view: View, x0: Int, y0: Int, x1: Int, y1: Int) {
        view.setTag(TAG, Ref(x0, y0, x1, y1))
        view.requestLayout()
    }

    /** The reference x at the screen's left and right edges (beyond 0..1672 on phones wider than 16:9). */
    val visibleLeft get() = -offX / sx
    val visibleRight get() = (width - offX) / sx

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val ownW = MeasureSpec.getSize(widthMeasureSpec); val ownH = MeasureSpec.getSize(heightMeasureSpec)
        val a = anchor
        val w = if (a != null && a.measuredWidth > 0) a.measuredWidth else ownW
        val h = if (a != null && a.measuredHeight > 0) a.measuredHeight else ownH
        // Cover: the canvas fills the screen and is cropped top and bottom on wide phones (a picture with no plain
        // sides to widen, like the menu book); otherwise it fits inside and the wide picture fills the sides.
        val newScale = if (cover) maxOf(w / REF_W, h / REF_H) else minOf(w / REF_W, h / REF_H)
        if (newScale != scale && newScale > 0f) {
            scale = newScale
            scaledTexts.forEach { (v, p) -> v.setTextSize(TypedValue.COMPLEX_UNIT_PX, p * scale) }
        }
        sx = if (stretchX) w / REF_W else scale
        offX = (w - REF_W * sx) / 2f
        offY = (h - REF_H * scale) / 2f
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            val r = c.getTag(TAG) as? Ref
            if (r == null) {
                c.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
            } else {
                c.measure(MeasureSpec.makeMeasureSpec(((r.x1 - r.x0) * sx).roundToInt(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(((r.y1 - r.y0) * scale).roundToInt(), MeasureSpec.EXACTLY))
            }
        }
        setMeasuredDimension(ownW, ownH)
    }

    private val here = IntArray(2)
    private val there = IntArray(2)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val a = anchor
        var dx = 0f; var dy = 0f
        if (a != null) {
            getLocationInWindow(here); a.getLocationInWindow(there)
            dx = (here[0] - there[0]).toFloat(); dy = (here[1] - there[1]).toFloat()
            offX = (a.width.coerceAtLeast(a.measuredWidth) - REF_W * sx) / 2f - dx
            offY = (a.height.coerceAtLeast(a.measuredHeight) - REF_H * scale) / 2f - dy
        }
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            val ref = c.getTag(TAG) as? Ref
            if (ref == null) { c.layout(0, 0, c.measuredWidth, c.measuredHeight); continue }
            val x = (offX + ref.x0 * sx).roundToInt(); val y = (offY + ref.y0 * scale).roundToInt()
            c.layout(x, y, x + c.measuredWidth, y + c.measuredHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val art = art ?: return
        // Fill behind everything with the picture's edge colour, then the wide picture at the canvas height.
        paint.color = EDGE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        val h = REF_H * scale
        val w = h * art.intrinsicWidth / art.intrinsicHeight
        val left = ((width - w) / 2f).roundToInt(); val top = offY.roundToInt()
        art.setBounds(left, top, left + w.roundToInt(), top + h.roundToInt())
        art.draw(canvas)
    }

    companion object {
        const val REF_W = 1672f
        const val REF_H = 941f

        /**
         * A's separated design pack (TinyRunners-separated-assets, 2026-11-16) is laid out on 1280×720; this turns
         * one of its coordinates into this canvas's (same 16:9 shape).
         */
        fun v2(v: Int): Int = (v * REF_W / 1280f).roundToInt()
        private val TAG = com.tinyrunners.school.R.id.mockup_rect
        private val EDGE = android.graphics.Color.rgb(246, 236, 214)

        /** A button on one of the separated pack's skins ([normal], darker [pressed]), its label at [textPx] reference px. */
        fun skinButton(context: Context, frame: MockupFrame, normal: Int, pressed: Int, text: String, textColor: Int,
                       textPx: Float, click: () -> Unit): Button = Button(context).apply {
            this.text = text; isAllCaps = false; stateListAnimator = null; setTextColor(textColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = android.graphics.drawable.StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), context.getDrawable(pressed))
                addState(intArrayOf(), context.getDrawable(normal))
            }
            setOnClickListener { click() }
            frame.onScaleText(this, textPx)
        }

        /** A button drawn with one of A's part pictures, its label in [textColor] at [textPx] reference pixels. */
        fun partButton(context: Context, frame: MockupFrame, part: Int, text: String, textColor: Int, textPx: Float,
                       click: () -> Unit): Button = Button(context).apply {
            this.text = text
            isAllCaps = false
            stateListAnimator = null
            setTextColor(textColor)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), context.getDrawable(part), null)
            setOnClickListener { click() }
            frame.onScaleText(this, textPx)
        }
    }

    private val scaledTexts = mutableListOf<Pair<android.widget.TextView, Float>>()

    /** Keeps [view]'s text at [px] reference pixels whatever the scale. */
    fun onScaleText(view: android.widget.TextView, px: Float) {
        scaledTexts += view to px
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, px * scale)
    }
}
