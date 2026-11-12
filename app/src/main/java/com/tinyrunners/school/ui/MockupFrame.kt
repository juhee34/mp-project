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
 * and 20:9 phones. Children added without [place] fill the whole frame.
 */
class MockupFrame(context: Context, backgroundRes: Int) : FrameLayout(context) {
    private val art = requireNotNull(context.getDrawable(backgroundRes))
    private val paint = Paint()

    /** Scale from reference pixels to screen pixels, and where the reference canvas starts. */
    var scale = 1f; private set
    private var offX = 0f
    private var offY = 0f

    init { setWillNotDraw(false) }

    private class Ref(val x0: Int, val y0: Int, val x1: Int, val y1: Int)

    /** Adds [view] at the reference rect [x0, y0, x1, y1]. */
    fun place(view: View, x0: Int, y0: Int, x1: Int, y1: Int) {
        view.setTag(TAG, Ref(x0, y0, x1, y1))
        addView(view, LayoutParams(0, 0))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec); val h = MeasureSpec.getSize(heightMeasureSpec)
        val newScale = minOf(w / REF_W, h / REF_H)
        if (newScale != scale && newScale > 0f) {
            scale = newScale
            scaledTexts.forEach { (v, p) -> v.setTextSize(TypedValue.COMPLEX_UNIT_PX, p * scale) }
        }
        offX = (w - REF_W * scale) / 2f
        offY = (h - REF_H * scale) / 2f
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            val r = c.getTag(TAG) as? Ref
            if (r == null) {
                c.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
            } else {
                c.measure(MeasureSpec.makeMeasureSpec(((r.x1 - r.x0) * scale).roundToInt(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(((r.y1 - r.y0) * scale).roundToInt(), MeasureSpec.EXACTLY))
            }
        }
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            val ref = c.getTag(TAG) as? Ref
            if (ref == null) { c.layout(0, 0, c.measuredWidth, c.measuredHeight); continue }
            val x = (offX + ref.x0 * scale).roundToInt(); val y = (offY + ref.y0 * scale).roundToInt()
            c.layout(x, y, x + c.measuredWidth, y + c.measuredHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
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
        private val TAG = com.tinyrunners.school.R.id.mockup_rect
        private val EDGE = android.graphics.Color.rgb(246, 236, 214)

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
