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
 * picture, an optional pill and one button. Sizes follow [frame]'s scale, times [zoom] where the shelf is lower.
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
              look: Look, zoom: Float = 1f, click: () -> Unit): View {
        val box = RefBox(activity, frame, 380, 581, zoom).apply { setPadding(0, 0, 0, 0) }
        // Long titles start right of the ribbon so they never run under it.
        val long = title.length > 7
        box.add(ImageView(activity).apply { setImageResource(R.drawable.ui_story_card_frame); scaleType = ImageView.ScaleType.FIT_XY }, 0, 13, 377, 581)
        box.add(TextView(activity).apply {
            text = title; setTextColor(DEEP_GREEN); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            maxLines = if (long) 2 else 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(0, 0, 0, 0)
            setLineSpacing(0f, 0.95f); includeFontPadding = false
        }.also { frame.onScaleText(it, (if (long) 27f else 40f) * zoom) }, if (long) 106 else 24, if (long) 58 else 66, 366, if (long) 128 else 120)
        box.add(ImageView(activity).apply { setImageResource(ribbon); scaleType = ImageView.ScaleType.FIT_XY }, 38, 0, 104, 79)
        box.add(picture, 35, 131, 343, 400)
        if (pill != null) box.add(TextView(activity).apply {
            text = pill; setTextColor(Color.parseColor("#3D3550")); gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(0, 0, 0, 0)
            background = activity.getDrawable(R.drawable.ui_story_count_pill)
        }.also { frame.onScaleText(it, 27f * zoom) }, 67, 416, 310, 460)
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
        }.also { frame.onScaleText(it, 32f * zoom) }, 38, 478, 342, 543)
        if (look != Look.LOCKED) { box.isClickable = true; box.setOnClickListener { click() } }
        return box
    }

    /** A's separated-pack book covers (panels/story-book-<id>), one spine and bookmark colour per friend. */
    private val BOOKS = mapOf(
        "mint" to R.drawable.ui_v2_book_mint, "cheese" to R.drawable.ui_v2_book_cheese, "popo" to R.drawable.ui_v2_book_popo,
        "lumi" to R.drawable.ui_v2_book_lumi, "nunu" to R.drawable.ui_v2_book_nunu, "piko" to R.drawable.ui_v2_book_piko)
    internal val HOMES = mapOf(
        "mint" to R.drawable.bg_v2_friends_mint_wide, "cheese" to R.drawable.bg_v2_friends_cheese_wide,
        "popo" to R.drawable.bg_v2_friends_popo_wide, "lumi" to R.drawable.bg_v2_friends_lumi_wide,
        "nunu" to R.drawable.bg_v2_friends_nunu_wide, "piko" to R.drawable.bg_v2_friends_piko_wide)
    private val homeCache = mutableMapOf<String, android.graphics.Bitmap?>()
    val COVER_INK = Color.rgb(68, 60, 49)
    val COVER_SOFT = Color.rgb(120, 110, 88)

    fun book(friendId: String) = BOOKS[friendId] ?: R.drawable.ui_v2_book_mint

    /** The friend's home from their selection picture (the part under the pale sky), small, with the friend standing in it. */
    fun homePicture(activity: Activity, friendId: String): View {
        val picture = homeCache.getOrPut(friendId) {
            val res = HOMES[friendId] ?: return@getOrPut null
            val full = android.graphics.BitmapFactory.decodeResource(activity.resources, res,
                android.graphics.BitmapFactory.Options().apply { inSampleSize = 4; inScaled = false }) ?: return@getOrPut null
            // The original 16:9 picture is the middle 80 % of the widened one; its home starts under the sky band.
            val x0 = full.width / 10; val y0 = full.height * 23 / 100
            android.graphics.Bitmap.createBitmap(full, x0, y0, full.width - 2 * x0, full.height - y0).also { if (it != full) full.recycle() }
        }
        return HomePicture(activity, picture, friendId)
    }

    /** The home picture with the friend standing small at the bottom middle. */
    private class HomePicture(activity: Activity, picture: android.graphics.Bitmap?, friendId: String) : android.widget.FrameLayout(activity) {
        init {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            addView(ImageView(activity).apply { setImageBitmap(picture); scaleType = ImageView.ScaleType.CENTER_CROP })
            addView(com.tinyrunners.school.opening.OpeningFriend(activity, friendId, 0))
        }
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec); val h = MeasureSpec.getSize(heightMeasureSpec)
            getChildAt(0).measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
            val f = h * 3 / 10
            getChildAt(1).measure(MeasureSpec.makeMeasureSpec(f, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(f, MeasureSpec.EXACTLY))
            setMeasuredDimension(w, h)
        }
        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            val w = r - l; val h = b - t
            getChildAt(0).layout(0, 0, w, h)
            val f = getChildAt(1).measuredWidth
            val bottom = h - h / 9
            getChildAt(1).layout((w - f) / 2, bottom - f, (w + f) / 2, bottom)
        }
    }

    /**
     * One book on the shelf, as in A's separated pack (reference/screen-4): the cover with its spine and bookmark,
     * a framed picture, the title, a small line under it, a rule, and "책을 펼쳐 보기 →" (or why it is closed).
     */
    fun cover(activity: Activity, frame: MockupFrame, book: Int, title: String, sub: String, picture: View, action: String,
              open: Boolean, click: () -> Unit): View {
        val v = MockupFrame.Companion::v2
        // The PNG is 271×387 with a 10 px shadow edge; one more 5 px keeps the books apart (276 apart in the mockup).
        val box = RefBox(activity, frame, v(276), v(387))
        box.add(ImageView(activity).apply { setImageResource(book); scaleType = ImageView.ScaleType.FIT_XY }, 0, 0, v(271), v(387))
        box.add(android.widget.FrameLayout(activity).apply {
            setBackgroundColor(Color.rgb(255, 250, 236))
            val p = v(6); setPadding(p, p, p, p)
            addView(picture, android.widget.FrameLayout.LayoutParams(-1, -1))
            elevation = 2f
        }, v(38), v(47), v(245), v(200))
        fun text(px: Int, color: Int, bold: Boolean = false) = TextView(activity).apply {
            setTextColor(color); includeFontPadding = false; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            if (bold) typeface = Typeface.DEFAULT_BOLD
            frame.onScaleText(this, v(px).toFloat())
        }
        box.add(text(if (title.length > 8) 24 else 28, COVER_INK, bold = true).apply { text = title; gravity = Gravity.CENTER_VERTICAL },
            v(38), v(206), v(250), v(250))
        // The home and how many stories were seen, on up to two lines.
        box.add(text(21, COVER_SOFT).apply { text = sub; gravity = Gravity.CENTER_VERTICAL; maxLines = 2 }, v(38), v(252), v(250), v(306))
        box.add(View(activity).apply { setBackgroundColor(Color.argb(80, 90, 80, 60)) }, v(38), v(312), v(245), v(313))
        box.add(text(21, if (open) COVER_INK else COVER_SOFT).apply { text = action; gravity = Gravity.CENTER_VERTICAL }, v(38), v(320), v(220), v(352))
        if (open) box.add(text(22, COVER_INK).apply { text = "→"; gravity = Gravity.CENTER }, v(220), v(320), v(250), v(352))
        if (!open) box.alpha = 0.82f
        box.contentDescription = "$title. $sub. $action"
        if (open) {
            box.isClickable = true
            box.foreground = RippleDrawable(ColorStateList.valueOf(0x22000000), null, android.graphics.drawable.ColorDrawable(Color.WHITE))
            box.setOnClickListener { click() }
        }
        return box
    }
}
