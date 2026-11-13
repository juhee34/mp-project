package com.tinyrunners.school.story

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.tinyrunners.school.R
import com.tinyrunners.school.ui.MockupFrame

/**
 * The storybook card from A's approved story-collection design, shared by 이야기 모음 and 스토리 진행 so the two
 * screens read alike: a book-page frame (380×581 reference pixels, from the 라일 card) with a ribbon, a title, a
 * picture, an optional pill and one button. Sizes follow [frame]'s scale.
 */
internal object StoryBookCard {
    enum class Look { PRIMARY, OUTLINE, LOCKED }

    val DEEP_GREEN = Color.parseColor("#1E5A2A")
    val CREAM = Color.parseColor("#FFF8E7")
    /** Ribbons and thumbnails A has drawn so far (세계·라일·치즈·포포); the others use the world ribbon and the home picture. */
    val RIBBONS = mapOf("mint" to R.drawable.ui_story_ribbon_mint, "cheese" to R.drawable.ui_story_ribbon_cheese, "popo" to R.drawable.ui_story_ribbon_popo)
    val THUMBS = mapOf("mint" to R.drawable.story_thumb_mint, "cheese" to R.drawable.story_thumb_cheese, "popo" to R.drawable.story_thumb_popo)

    fun ribbon(friendId: String) = RIBBONS[friendId] ?: R.drawable.ui_story_ribbon_world

    /** The friend's drawn thumbnail when A has made one, otherwise their home from the running background. */
    fun picture(activity: Activity, friendId: String): View =
        THUMBS[friendId]?.let { part ->
            ImageView(activity).apply {
                setImageResource(part); scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        } ?: SceneThumb(activity, friendId, locked = false)

    fun build(activity: Activity, frame: MockupFrame, ribbon: Int, title: String, picture: View, pill: String?, button: String,
              look: Look, click: () -> Unit): View {
        val box = RefBox(activity, frame, 380, 581).apply { setPadding(0, 0, 0, 0) }
        // Long titles start right of the ribbon so they never run under it.
        val long = title.length > 7
        box.add(ImageView(activity).apply { setImageResource(R.drawable.ui_story_card_frame); scaleType = ImageView.ScaleType.FIT_XY }, 0, 13, 377, 581)
        box.add(TextView(activity).apply {
            text = title; setTextColor(DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            maxLines = if (long) 2 else 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(0, 0, 0, 0)
            setLineSpacing(0f, 0.95f); includeFontPadding = false
        }.also { frame.onScaleText(it, if (long) 27f else 40f) }, if (long) 106 else 24, if (long) 58 else 66, 366, if (long) 128 else 120)
        box.add(ImageView(activity).apply { setImageResource(ribbon); scaleType = ImageView.ScaleType.FIT_XY }, 38, 0, 104, 79)
        box.add(picture, 35, 131, 343, 400)
        if (pill != null) box.add(TextView(activity).apply {
            text = pill; setTextColor(Color.parseColor("#3D3550")); gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(0, 0, 0, 0)
            background = activity.getDrawable(R.drawable.ui_story_count_pill)
        }.also { frame.onScaleText(it, 27f) }, 67, 416, 310, 460)
        val (part, ink) = when (look) {
            Look.PRIMARY -> R.drawable.ui_story_btn_primary to CREAM
            Look.OUTLINE -> R.drawable.ui_story_btn_outline to DEEP_GREEN
            Look.LOCKED -> R.drawable.ui_story_btn_disabled to Color.parseColor("#8A8478")
        }
        box.add(Button(activity).apply {
            text = button; isAllCaps = false; stateListAnimator = null; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            background = RippleDrawable(ColorStateList.valueOf(0x22000000), activity.getDrawable(part), null)
            isEnabled = look != Look.LOCKED
            setOnClickListener { click() }
        }.also { frame.onScaleText(it, 32f) }, 38, 478, 342, 543)
        if (look != Look.LOCKED) { box.isClickable = true; box.setOnClickListener { click() } }
        return box
    }
}
