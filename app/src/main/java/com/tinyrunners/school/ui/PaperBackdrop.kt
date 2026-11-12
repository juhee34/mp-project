package com.tinyrunners.school.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.tinyrunners.school.R
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The shared paper-and-bench scenery from the approved story-collection design (A art, docs/art/ui-screens).
 * Sits behind the village stories, the story collection, the character screens and the records so they read as
 * one place. Uses the 20:9 version (bg_common_paper_wide, paper continued on both sides) so wide phones show no
 * picture edge; it fills the view and crops evenly from the centre; the middle of the picture is plain paper, so
 * cards and text on top stay readable. The hub and login use their own approved pictures (MockupFrame).
 */
class PaperBackdropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,   // needed when placed in an XML layout (activity_main.xml)
) : View(context, attrs) {
    private val art = requireNotNull(context.getDrawable(R.drawable.bg_common_paper_wide))

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        val iw = art.intrinsicWidth.toFloat(); val ih = art.intrinsicHeight.toFloat()
        if (iw <= 0f || ih <= 0f || width == 0 || height == 0) return
        val scale = max(width / iw, height / ih)
        val dw = (iw * scale).roundToInt(); val dh = (ih * scale).roundToInt()
        val left = (width - dw) / 2; val top = (height - dh) / 2
        art.setBounds(left, top, left + dw, top + dh)
        art.draw(canvas)
    }
}
