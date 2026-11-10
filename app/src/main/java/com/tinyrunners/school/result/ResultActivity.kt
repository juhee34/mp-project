package com.tinyrunners.school.result

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tinyrunners.school.R
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.game.GameActivity
import com.tinyrunners.school.progress.ProgressStore
import com.tinyrunners.school.progress.RunResult
import com.tinyrunners.school.records.RecordsActivity
import com.tinyrunners.school.story.StoryRunSignal
import com.tinyrunners.school.ui.Page

/**
 * Shows one finished run and saves it. Saving uses the run's ID, so recreating this screen (rotation,
 * returning from the background) never counts the same run twice.
 */
class ResultActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val run = RunResult(
            runId = intent.getStringExtra(EXTRA_RUN_ID).orEmpty(),
            characterId = intent.getStringExtra(GameActivity.EXTRA_CHARACTER_ID).orEmpty(),
            meters = intent.getIntExtra(EXTRA_METERS, 0),
            score = intent.getIntExtra(EXTRA_SCORE, 0),
            cleared = intent.getIntExtra(EXTRA_CLEARED, 0),
        )
        val character = CharacterRepository(this).find(run.characterId)
        if (character == null || run.runId.isEmpty()) { finish(); return }

        val page = Page(this).show()
        page.eyebrow(getString(R.string.result_eyebrow))
        page.title(getString(R.string.result_title, character.name, run.meters))
        val card = page.card(16)
        page.line(card, getString(R.string.result_score, run.score), 20f, Page.INK, bold = true, top = 0)
        page.line(card, getString(R.string.result_cleared, run.cleared))
        val status = page.text(getString(R.string.result_saving), 15f)

        val storyId = intent.getStringExtra(GameActivity.EXTRA_STORY_ID)
        if (storyId != null) {
            // Any result continues the story; there is no "try again to unlock".
            StoryRunSignal.finishedFor = storyId
            page.button(getString(R.string.result_continue_story), primary = true) { finish() }
        }
        page.button(getString(R.string.result_again), primary = storyId == null) {
            startActivity(GameActivity.intent(this, character.id).putExtra(GameActivity.EXTRA_STORY_ID, storyId))
            finish()
        }
        page.button(getString(R.string.result_records)) {
            startActivity(Intent(this, RecordsActivity::class.java))
        }
        if (storyId == null) page.button(getString(R.string.result_choose_friend)) { finish() }

        val store = ProgressStore.forCurrentUser(this)
        store.record(run) { result ->
            if (isDestroyed) return@record
            result.onSuccess { outcome ->
                val p = outcome.progress
                val where = getString(if (store.location == ProgressStore.Location.CLOUD) R.string.records_where_cloud else R.string.records_where_device)
                status.text = buildString {
                    if (outcome.newBest) append(getString(R.string.result_new_best)).append('\n')
                    append(getString(R.string.result_progress, character.name, p.runs, p.totalMeters, p.bestScore))
                    append('\n').append(where)
                }
            }.onFailure {
                status.setTextColor(Page.ERROR)
                status.text = getString(R.string.result_save_failed)
            }
        }
    }

    companion object {
        const val EXTRA_RUN_ID = "runId"
        const val EXTRA_METERS = "meters"
        const val EXTRA_SCORE = "score"
        const val EXTRA_CLEARED = "cleared"

        fun intent(context: Context, run: RunResult) = Intent(context, ResultActivity::class.java)
            .putExtra(EXTRA_RUN_ID, run.runId)
            .putExtra(GameActivity.EXTRA_CHARACTER_ID, run.characterId)
            .putExtra(EXTRA_METERS, run.meters)
            .putExtra(EXTRA_SCORE, run.score)
            .putExtra(EXTRA_CLEARED, run.cleared)
    }
}
