package com.tinyrunners.school.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * A story told while running (assets/content/run_stories.json): lines at given distances, one crossroads with a
 * question and two routes, and an ending for each route. The friend's lines appear in a speech bubble above their
 * head, the narrator's as a subtitle at the top; nothing stops the run.
 *
 * Choosing: the upper route is the raised path (jump onto it), the lower one the ground (run on). The choice changes
 * the music's colour from the crossroads on and which ending is told when the stage ends.
 */
data class RunLine(val atMeters: Int, val narrator: Boolean, val text: String)

/** [correct]: the right route of this story (true = up), or null when either is fine. */
data class RunFork(val atMeters: Int, val question: String, val up: String, val down: String, val correct: Boolean? = null)

/**
 * [endings] by key: "up" / "down" for an ordinary stage; a finale (the friend's last story) has "happy", "hidden"
 * and "bad", picked by [EndingRule] from this choice and the earlier ones.
 */
data class RunScript(
    val storyId: String?,
    val title: String,
    val lines: List<RunLine>,
    val fork: RunFork?,
    val endings: Map<String, List<RunLine>>,
    val finale: Boolean = false,
)

/**
 * The treasure legend's three endings, decided at the finale. Pure, so it is unit tested.
 * - bad: a wrong route anywhere on the way (an earlier story or the finale) — the treasure is not found (told gently;
 *   the story can be run again).
 * - happy: every route right, the finale run by day — the treasure is found and the wish comes true.
 * - hidden: every route right, the finale run at night (real night, or night picked in settings) — something dearer
 *   than the treasure is found. Only the night opens it.
 */
object EndingRule {
    const val HAPPY = "happy"
    const val HIDDEN = "hidden"
    const val BAD = "bad"

    fun pick(allRight: Boolean, night: Boolean): String = when {
        !allRight -> BAD
        night -> HIDDEN
        else -> HAPPY
    }
}

/** Which route was taken in each story (per account, on this device), for the finale's ending. */
class RunRoutes(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "routes_" + (com.tinyrunners.school.auth.AuthService.currentUid(context) ?: "guest"), Context.MODE_PRIVATE)

    fun get(storyId: String): Boolean? = if (prefs.contains(storyId)) prefs.getBoolean(storyId, false) else null
    fun set(storyId: String, up: Boolean) { prefs.edit().putBoolean(storyId, up).apply() }
    /** The last ending seen for [storyId] (happy/hidden/bad or up/down). */
    fun ending(storyId: String): String? = prefs.getString("ending_$storyId", null)
    fun setEnding(storyId: String, key: String) { prefs.edit().putString("ending_$storyId", key).apply() }
}

object RunScriptParser {
    /** Reads [key] from the pack, or the "default" script with {name} and {title} filled in, or null without either. */
    fun parse(json: String, key: String?, name: String, title: String, storyId: String?): RunScript? {
        val root = JSONObject(json)
        val o = (key?.let { root.optJSONObject(it) } ?: root.optJSONObject("default")) ?: return null
        fun fill(s: String) = s.replace("{name}", name).replace("{title}", title)
        fun lines(a: JSONArray?): List<RunLine> = if (a == null) emptyList() else List(a.length()) { i ->
            val l = a.getJSONObject(i)
            RunLine(l.optInt("at", 0), l.optString("who") == "narrator", fill(l.getString("text")))
        }
        val fork = o.optJSONObject("fork")?.let { f ->
            RunFork(f.getInt("at"), fill(f.getString("question")), fill(f.getString("up")), fill(f.getString("down")),
                when (f.optString("correct")) { "up" -> true; "down" -> false; else -> null })
        }
        val endings = o.optJSONObject("endings")
        val byKey = endings?.keys()?.asSequence()?.associateWith { lines(endings.optJSONArray(it)) }.orEmpty()
        return RunScript(storyId, fill(o.optString("title", title)), lines(o.optJSONArray("lines")), fork, byKey, o.optBoolean("finale", false))
    }

    fun load(context: Context, storyId: String?, name: String, title: String): RunScript? = try {
        context.assets.open("content/run_stories.json").bufferedReader().use { parse(it.readText(), storyId, name, title, storyId) }
    } catch (e: Exception) {
        android.util.Log.w("RunStory", "run_stories.json could not be read", e)
        null
    }
}

/**
 * Plays a [RunScript] against the run's distance and the crossroads choice. Pure, so it is unit tested: call
 * [update] every frame with the run's metres, the engine's choice and the seconds since the last frame, then read
 * what to show.
 */
class RunStoryPlayer(
    val script: RunScript,
    private val endingFor: (Boolean) -> String = { if (it) "up" else "down" },
    /** Added after an ordinary story's ending when the wrong route was taken (a hint before the finale). */
    private val missedLine: String? = null,
) {
    /** The ending being told (up/down, or happy/hidden/bad at a finale), once the crossroads is decided. */
    var endingKey: String? = null; private set
    /** The line on screen now (null: nothing), and how long it has been showing. */
    var current: RunLine? = null; private set
    private var shownFor = 0f
    private val queue = ArrayDeque<RunLine>()
    private var nextLine = 0
    /** True while the crossroads question is up (from when the crossroads comes on screen until it is decided). */
    var asking = false; private set
    var choice: Boolean? = null; private set
    /** True once the chosen ending has been told to the end: the story counts as seen. */
    var finished = false; private set
    private var endingQueued = false

    fun update(meters: Int, forkAhead: Boolean, forkChoice: Boolean?, dt: Float) {
        while (nextLine < script.lines.size && meters >= script.lines[nextLine].atMeters) queue.addLast(script.lines[nextLine++])
        val fork = script.fork
        if (fork != null && choice == null) {
            asking = forkAhead && forkChoice == null
            if (forkChoice != null) {
                choice = forkChoice; asking = false
                // The ending follows right away, after anything still waiting.
                val key = endingFor(forkChoice)
                endingKey = key
                queue.addAll(script.endings[key].orEmpty())
                val right = fork.correct
                if (!script.finale && right != null && right != forkChoice && missedLine != null) queue.addLast(RunLine(0, true, missedLine))
                endingQueued = true
            }
        } else if (fork == null && !endingQueued && nextLine >= script.lines.size) {
            endingQueued = true
        }
        val line = current
        if (line != null) {
            shownFor += dt
            if (shownFor >= durationOf(line)) { current = null; shownFor = 0f }
        }
        if (current == null && queue.isNotEmpty()) { current = queue.removeFirst(); shownFor = 0f }
        if (endingQueued && current == null && queue.isEmpty() && !finished) finished = true
    }

    companion object {
        /** Long enough to read: 1.8 s plus a little per letter, at most 4.5 s. */
        fun durationOf(line: RunLine): Float = (1.8f + line.text.length * 0.07f).coerceAtMost(4.5f)
    }
}
