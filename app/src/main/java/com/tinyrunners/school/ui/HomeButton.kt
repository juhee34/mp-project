package com.tinyrunners.school.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Button
import com.tinyrunners.school.R
import com.tinyrunners.school.hub.HubActivity

/**
 * The small "◀ 돌아가기" button at the top-left of the friend selection, shop, settings and other screens. It works as
 * a back button: it closes this screen and shows the one it was opened from (settings opened from the friend
 * selection go back to the selection), or the menu when nothing is underneath. It wears the top-bar pill ([TopPill]).
 */
object HomeButton {
    fun create(context: Context, label: String = context.getString(R.string.common_back_pill), click: () -> Unit = { goBack(context) }): Button =
        Button(context).apply {
            text = label
            TopPill.style(this)
            setOnClickListener { click() }
        }

    /** Back to the screen this one was opened from; the menu when this screen is the first one in the app's stack. */
    fun goBack(context: Context) {
        val activity = context as? Activity
        if (activity != null && !activity.isTaskRoot) activity.finish() else goToMenu(context)
    }

    /** Back to the menu that is already open under this screen (screens above it close), or a fresh menu. */
    fun goToMenu(context: Context) {
        context.startActivity(Intent(context, HubActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        (context as? Activity)?.finish()
    }
}
