package com.tinyrunners.school.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

/**
 * How strongly lists move under a finger (설정 > 대화·안내 > 스크롤 민감도): 0–100, 50 is the phone's own feel.
 * Lower moves the page less for the same finger movement and flings it less far; higher moves it more.
 */
class ScrollFeel(context: Context) {
    private val prefs = context.getSharedPreferences("scroll_feel", Context.MODE_PRIVATE)

    var percent: Int
        get() = prefs.getInt("percent", DEFAULT)
        set(value) { prefs.edit().putInt("percent", value.coerceIn(0, 100)).apply() }

    /** 0 → 0.4×, 50 → 1×, 100 → 1.6×. */
    val factor: Float get() = factorFor(percent)

    companion object {
        const val DEFAULT = 50
        fun factorFor(percent: Int): Float = 0.4f + percent.coerceIn(0, 100) * 0.012f
    }
}

/**
 * A ScrollView that follows [ScrollFeel]: the finger's movement (and so the fling that follows) is scaled before
 * the ScrollView sees it, so dragging and flinging both feel slower or quicker together.
 */
open class TunedScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ScrollView(context, attrs) {
    private val feel = ScrollFeel(context)
    private var downY = 0f

    private fun scaled(ev: MotionEvent): MotionEvent {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) downY = ev.y
        val copy = MotionEvent.obtain(ev)
        copy.setLocation(ev.x, downY + (ev.y - downY) * feel.factor)
        return copy
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (feel.factor == 1f) return super.onInterceptTouchEvent(ev)
        val e = scaled(ev)
        return try { super.onInterceptTouchEvent(e) } finally { e.recycle() }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (feel.factor == 1f) return super.onTouchEvent(ev)
        val e = scaled(ev)
        return try { super.onTouchEvent(e) } finally { e.recycle() }
    }
}
