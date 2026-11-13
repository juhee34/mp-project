package com.tinyrunners.school.story

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.View
import com.tinyrunners.school.ui.MockupFrame

/**
 * A screen title drawn the way the story-collection picture draws "이야기 모음": deep green bold words, a short
 * rule on each side at the middle of the words, and under them two rules meeting at a small diamond. Sizes are in
 * the mockup's reference pixels (scaled by [frame]), so the title matches the collection's on the same canvas.
 */
@SuppressLint("ViewConstructor")
internal class TitleBanner(context: Context, private val frame: MockupFrame, private val title: String) : View(context) {
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = StoryBookCard.DEEP_GREEN; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = StoryBookCard.DEEP_GREEN; style = Paint.Style.STROKE }
    private val diamond = Path()

    init {
        contentDescription = title
        if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
    }

    override fun onDraw(canvas: Canvas) {
        val s = frame.scale
        if (s <= 0f) return
        val cx = width / 2f
        text.textSize = 70f * s
        val half = text.measureText(title) / 2f
        val base = 82f * s
        canvas.drawText(title, cx, base, text)
        // Side rules at the middle of the words.
        line.strokeWidth = 3f * s
        val mid = base - 24f * s
        canvas.drawLine(cx - half - 140f * s, mid, cx - half - 40f * s, mid, line)
        canvas.drawLine(cx + half + 40f * s, mid, cx + half + 140f * s, mid, line)
        // Rules under the words meeting at a diamond.
        val under = base + 36f * s
        val d = 9f * s
        canvas.drawLine(cx - half - 12f * s, under, cx - d - 10f * s, under, line)
        canvas.drawLine(cx + d + 10f * s, under, cx + half + 12f * s, under, line)
        diamond.reset()
        diamond.moveTo(cx, under - d); diamond.lineTo(cx + d, under); diamond.lineTo(cx, under + d); diamond.lineTo(cx - d, under); diamond.close()
        line.strokeWidth = 2.5f * s
        canvas.drawPath(diamond, line)
    }
}
