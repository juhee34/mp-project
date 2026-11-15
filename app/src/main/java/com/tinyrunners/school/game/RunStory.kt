package com.tinyrunners.school.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * A story played in four acts around one run (assets/content/run_stories.json), the way action games with a story
 * tell it between and inside stages:
 * 1. 발단 [opening] — an RPG talk box before the run (who, where, what happened, and today's weather in the legend).
 * 2. 전개·위기 [lines] and [fork] — speech bubbles and subtitles while running, then the crossroads question.
 * 3. 절정 [climax] — the run pauses for an RPG talk that depends on the route taken.
 * 4. 결말 — a last stretch to a goal ([goalMeters]), then an ending picture with an RPG talk: [closing] for an
 *    ordinary story, or the finale's happy / hidden / bad [endings].
 *
 * Choosing: the upper route is the raised path (jump onto it), the lower one the ground (run on). The choice changes
 * the music's colour, the climax and the ending.
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
    val opening: List<RunLine> = emptyList(),
    /** "up" / "down" for an ordinary story; "right" / "wrong" (every route so far right or not) for a finale. */
    val climax: Map<String, List<RunLine>> = emptyMap(),
    /** "up" / "down": the ending picture's talk for an ordinary story. */
    val closing: Map<String, List<RunLine>> = emptyMap(),
    /** Metres to run after the climax to reach the ending. */
    val goalMeters: Int = 45,
)

/** Which weather line fits: "night" at night, else the sky (fog counts as cloudy). Null without a report. */
object StoryWeather {
    fun key(report: com.tinyrunners.school.weather.WeatherReport?): String? {
        report ?: return null
        if (!report.isDay) return "night"
        return when (report.sky) {
            com.tinyrunners.school.weather.Sky.CLEAR -> "clear"
            com.tinyrunners.school.weather.Sky.CLOUDY, com.tinyrunners.school.weather.Sky.FOG -> "cloudy"
            com.tinyrunners.school.weather.Sky.RAIN -> "rain"
            com.tinyrunners.school.weather.Sky.SNOW -> "snow"
            com.tinyrunners.school.weather.Sky.STORM -> "storm"
        }
    }
}

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
    /**
     * Reads [key] from the pack, or the "default" script with {name} and {title} filled in, or null without either.
     * Missing acts are made from what is there, so every story plays in four acts: the opening is the first two lines
     * when a story has none of its own; an ordinary climax is its up/down endings; the finale climax and the ordinary
     * closing come from the pack's shared "finaleClimax" and "closing". With [friendId] and [weather] (StoryWeather
     * key) the legend's line for today's weather joins the opening and the friend's own weather line joins the run.
     */
    fun parse(json: String, key: String?, name: String, title: String, storyId: String?,
              friendId: String? = null, weather: String? = null): RunScript? {
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
        fun map(obj: JSONObject?): Map<String, List<RunLine>> =
            obj?.keys()?.asSequence()?.associateWith { lines(obj.optJSONArray(it)) }.orEmpty()
        val endings = map(o.optJSONObject("endings"))
        val finale = o.optBoolean("finale", false)
        val all = lines(o.optJSONArray("lines"))
        val ownOpening = o.optJSONArray("opening")
        var opening = if (ownOpening != null) lines(ownOpening) else all.take(2)
        var running = if (ownOpening != null) all else all.drop(2)
        if (weather != null) {
            root.optJSONObject("legendWeather")?.optString(weather)?.takeIf { it.isNotEmpty() }?.let { opening = opening + RunLine(0, true, fill(it)) }
            friendId?.let { root.optJSONObject("weather")?.optJSONObject(it)?.optString(weather) }?.takeIf { it.isNotEmpty() }?.let { w ->
                // Today's weather as the friend sees it, a little into the run.
                running = (running + RunLine(WEATHER_LINE_AT, false, fill(w))).sortedBy { it.atMeters }
            }
        }
        val climax = map(o.optJSONObject("climax")).ifEmpty {
            if (finale) map(root.optJSONObject("finaleClimax")) else endings.filterKeys { it == "up" || it == "down" }
        }
        val closing = map(o.optJSONObject("closing")).ifEmpty { map(root.optJSONObject("closing")) }
        return RunScript(storyId, fill(o.optString("title", title)), running, fork, endings, finale, opening, climax, closing,
            o.optInt("goal", root.optInt("goal", 45)))
    }

    /** Where the friend's weather line comes in the run (metres). */
    const val WEATHER_LINE_AT = 32

    fun load(context: Context, storyId: String?, name: String, title: String, friendId: String? = null, weather: String? = null): RunScript? = try {
        context.assets.open("content/run_stories.json").bufferedReader().use { parse(it.readText(), storyId, name, title, storyId, friendId, weather) }
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
    /** False when the ending is told elsewhere (the four-act story: climax and ending are talks, not bubbles). */
    private val tellEndingInRun: Boolean = true,
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
                if (tellEndingInRun) queue.addAll(script.endings[key].orEmpty())
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
