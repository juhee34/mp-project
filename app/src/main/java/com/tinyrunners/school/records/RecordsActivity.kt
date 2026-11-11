package com.tinyrunners.school.records

import android.app.Activity
import android.os.Bundle
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.ui.Page

/** Per-friend records for the current account. Friends never run with still appear, without any penalty. */
class RecordsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val characters = CharacterRepository(this).characters
        val page = Page(this).show()
        page.bagButton()
        page.eyebrow(getString(R.string.records_eyebrow))
        page.title(getString(R.string.records_title))
        val store = ProgressStore.forCurrentUser(this)
        page.text(getString(if (store.location == ProgressStore.Location.CLOUD) R.string.records_where_cloud else R.string.records_where_device), 14f)
        val status = page.text(getString(R.string.records_loading), 15f)

        store.loadAll { result ->
            if (isDestroyed) return@loadAll
            result.onSuccess { all ->
                status.text = getString(R.string.records_total, all.values.sumOf { it.runs }, all.values.sumOf { it.totalMeters })
                characters.forEach { friend ->
                    val p = all[friend.id]
                    val card = page.card()
                    page.line(card, friend.name, 18f, Page.INK, bold = true, top = 0)
                    if (p == null || p.runs == 0) {
                        page.line(card, getString(R.string.records_none))
                    } else {
                        page.line(card, getString(R.string.records_line_runs, p.runs, p.totalMeters))
                        page.line(card, getString(R.string.records_line_best, p.bestMeters, p.bestScore))
                    }
                }
                page.button(getString(R.string.common_back)) { finish() }
            }.onFailure {
                status.setTextColor(Page.ERROR)
                status.text = getString(R.string.records_load_failed)
                page.button(getString(R.string.common_back)) { finish() }
            }
        }
    }
}
