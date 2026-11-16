package com.tinyrunners.school.talk

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.game.StoryBackground
import com.tinyrunners.school.ui.ScenePainter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Korean particle after a word: [withFinal] when the last syllable has a final consonant (라일과), else [without] (치즈와). */
internal object Josa {
    fun with(word: String, withFinal: String, without: String): String {
        val last = word.lastOrNull { it in '\uAC00'..'\uD7A3' } ?: return word + without
        return word + if ((last - '\uAC00') % 28 != 0) withFinal else without
    }
}

/** The friend's item as a still pixel icon (hand-over panel). */
@SuppressLint("ViewConstructor")
internal class ItemIconView(context: Context, private val item: com.tinyrunners.school.quest.QuestItem) : View(context) {
    override fun onDraw(canvas: Canvas) {
        val size = min(width, height).toFloat()
        com.tinyrunners.school.quest.ItemArt.draw(canvas, item, (width - size) / 2f, (height - size) / 2f, size, withGlow = false)
    }
}
