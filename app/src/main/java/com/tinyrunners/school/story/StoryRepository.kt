package com.tinyrunners.school.story

import android.content.Context

/** Loads the story scripts from assets. */
object StoryRepository {
    fun load(context: Context): List<Story> =
        Story.parseAll(context.assets.open("content/village_stories.json").bufferedReader().use { it.readText() })
}

/** Set by the result screen when a run that a story asked for has finished. */
object StoryRunSignal {
    var finishedFor: String? = null
}
