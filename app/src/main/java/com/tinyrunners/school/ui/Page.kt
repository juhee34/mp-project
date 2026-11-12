package com.tinyrunners.school.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.text.InputType
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.roundToInt

/** Small builder for B-owned screens, matching the palette of the character screens. */
class Page(private val activity: Activity) {
    val body = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(28))
    }
    /** The screen frame [show] puts the page in; floating buttons go here. */
    private var frame: android.widget.FrameLayout? = null

    val root = ScrollView(activity).apply {
        setBackgroundColor(BACKGROUND)
        isFillViewport = true
        // On wide (landscape) screens keep a readable column in the middle instead of stretching fields edge to edge.
        val wide = activity.resources.configuration.screenWidthDp > MAX_COLUMN_DP + 40
        addView(android.widget.FrameLayout(activity).apply {
            addView(body, android.widget.FrameLayout.LayoutParams(
                if (wide) dp(MAX_COLUMN_DP) else android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER_HORIZONTAL))
        })
        setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
    }

    /**
     * Shows the page over the opening's sky, clouds and hills. Popups pass [scenery] = false because the opening
     * itself is visible behind them. [paper] = true uses the shared paper-and-bench picture instead
     * (PaperBackdropView), as on the story and character screens.
     */
    fun show(scenery: Boolean = true, paper: Boolean = false): Page {
        if (!scenery) { activity.setContentView(root); return this }
        root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        activity.setContentView(android.widget.FrameLayout(activity).also { frame = it }.apply {
            setBackgroundColor(BACKGROUND)
            addView(if (paper) PaperBackdropView(activity) else SceneryView(activity), android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
            addView(root, android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
        })
        return this
    }

    /** Small top-left button (see HomeButton): "◀ 처음으로" to the menu unless another [text]/[click] is given. */
    fun backPill(text: String = activity.getString(com.tinyrunners.school.R.string.common_to_menu),
                 click: () -> Unit = { HomeButton.goToMenu(activity) }): Button =
        HomeButton.create(activity, text, click).also {
            body.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6) })
        }

    /** The pouch button that opens the bag, floating in the screen's top-right corner (below the status bar). */
    fun bagButton(): android.view.View {
        val host = frame ?: return BagButton.create(activity).also { body.addView(it, LinearLayout.LayoutParams(dp(40), dp(40))) }
        return BagButton.floatIn(host)
    }

    fun eyebrow(text: String) = label(text, 13f, LEAF, bold = true)
    fun title(text: String) = label(text, 28f, INK, top = 6, bold = true).apply { isAccessibilityHeading(this) }
    fun text(text: String, size: Float = 16f, color: Int = MUTED, top: Int = 8) = label(text, size, color, top)

    fun label(text: String, size: Float, color: Int, top: Int = 0, bold: Boolean = false): TextView =
        TextView(activity).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(0f, 1.15f)
        }.also { body.addView(it, params(top)) }

    fun field(hint: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText = EditText(activity).apply {
        this.hint = hint
        inputType = type
        // In landscape, keep the form visible while typing instead of Android's full-screen text editor.
        imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI or android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
        textSize = 16f
        setTextColor(INK)
        setHintTextColor(MUTED)
        minHeight = dp(52)
        setPadding(dp(14), dp(8), dp(14), dp(8))
        background = shape(PAPER, 14, LINE)
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
    }.also { body.addView(it, params(10)) }

    fun button(text: String, primary: Boolean = false, click: () -> Unit): Button = Button(activity).apply {
        this.text = text
        textSize = 16f
        isAllCaps = false
        minHeight = dp(52)
        setPadding(dp(14), dp(10), dp(14), dp(10))
        stateListAnimator = null
        style(this, primary)
        setOnClickListener { click() }
    }.also { body.addView(it, params(12)) }

    /** Makes a page button the main (filled) one or a plain one, e.g. once loaded data says which matters most. */
    fun style(button: Button, primary: Boolean) {
        button.setTextColor(if (primary) Color.WHITE else LEAF)
        button.background = RippleDrawable(ColorStateList.valueOf(MINT), shape(if (primary) LEAF else PAPER, 16, if (primary) LEAF else LINE), null)
    }

    /** A rounded card with its own vertical column. */
    fun card(top: Int = 12): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = shape(PAPER, 18, LINE)
    }.also { body.addView(it, params(top)) }

    fun line(card: LinearLayout, text: String, size: Float = 15f, color: Int = MUTED, bold: Boolean = false, top: Int = 4): TextView =
        TextView(activity).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }.also { card.addView(it, params(top)) }

    fun params(top: Int) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = dp(top) }

    fun shape(fill: Int, radius: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (border != null) setStroke(dp(1), border)
    }

    fun dp(value: Int) = (value * activity.resources.displayMetrics.density).roundToInt()

    private fun isAccessibilityHeading(view: TextView) {
        if (Build.VERSION.SDK_INT >= 28) view.isAccessibilityHeading = true
    }

    companion object {
        const val MAX_COLUMN_DP = 560
        val BACKGROUND = Color.rgb(250, 246, 236)
        val LEAF = Color.rgb(48, 83, 65)
        val MINT = Color.rgb(225, 239, 218)
        val PAPER = Color.rgb(255, 253, 246)
        val LINE = Color.rgb(221, 224, 209)
        val INK = Color.rgb(38, 52, 43)
        val MUTED = Color.rgb(79, 96, 84)
        val ERROR = Color.rgb(160, 60, 50)
    }
}
