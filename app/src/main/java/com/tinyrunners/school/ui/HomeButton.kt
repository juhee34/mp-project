package com.tinyrunners.school.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Button
import com.tinyrunners.school.R
import com.tinyrunners.school.hub.HubActivity

/**
 * The small "◀ 처음으로" button at the top-left of the story list, friend selection, shop and settings. It goes
 * back to the menu (hub) the same way from every screen. It wears the top-bar pill ([TopPill]).
 */
object HomeButton {
    fun create(context: Context, label: String = context.getString(R.string.common_to_menu), click: () -> Unit = { goToMenu(context) }): Button =
        Button(context).apply {
            text = label
            TopPill.style(this)
            setOnClickListener { click() }
        }

    /** Back to the menu that is already open under this screen (screens above it close), or a fresh menu. */
    fun goToMenu(context: Context) {
        context.startActivity(Intent(context, HubActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        (context as? Activity)?.finish()
    }
}
