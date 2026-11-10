package com.tinyrunners.school.progress

import org.json.JSONArray
import org.json.JSONObject

/** One finished run. [runId] makes saving idempotent: the same run is never counted twice. */
data class RunResult(
    val runId: String,
    val characterId: String,
    val meters: Int,
    val score: Int,
    val cleared: Int,
)

/** Everything remembered about running with one friend. Failed runs still add distance; nothing decays. */
data class CharacterProgress(
    val characterId: String,
    val runs: Int = 0,
    val totalMeters: Long = 0,
    val bestMeters: Int = 0,
    val bestScore: Int = 0,
    /** Stories seen to the end (or skipped). Order of first reading. */
    val readStoryIds: List<String> = emptyList(),
) {
    fun toMap(): Map<String, Any> = mapOf(
        "characterId" to characterId, "runs" to runs, "totalMeters" to totalMeters,
        "bestMeters" to bestMeters, "bestScore" to bestScore, "readStoryIds" to readStoryIds,
    )

    fun withRead(storyId: String) = if (storyId in readStoryIds) this else copy(readStoryIds = readStoryIds + storyId)

    companion object {
        fun fromMap(id: String, m: Map<String, Any?>) = CharacterProgress(
            characterId = id,
            runs = (m["runs"] as? Number)?.toInt() ?: 0,
            totalMeters = (m["totalMeters"] as? Number)?.toLong() ?: 0L,
            bestMeters = (m["bestMeters"] as? Number)?.toInt() ?: 0,
            bestScore = (m["bestScore"] as? Number)?.toInt() ?: 0,
            readStoryIds = when (val r = m["readStoryIds"]) {
                is List<*> -> r.filterIsInstance<String>()
                is JSONArray -> List(r.length()) { r.optString(it) }.filter { it.isNotEmpty() }
                else -> emptyList()
            },
        )
    }
}

data class RecordOutcome(val progress: CharacterProgress, val newBest: Boolean, val duplicate: Boolean)

object ProgressRules {
    fun apply(previous: CharacterProgress?, run: RunResult): CharacterProgress {
        val p = previous ?: CharacterProgress(run.characterId)
        return p.copy(
            runs = p.runs + 1,
            totalMeters = p.totalMeters + run.meters.coerceAtLeast(0),
            bestMeters = maxOf(p.bestMeters, run.meters),
            bestScore = maxOf(p.bestScore, run.score),
        )
    }

    fun outcome(previous: CharacterProgress?, run: RunResult): RecordOutcome {
        val next = apply(previous, run)
        return RecordOutcome(next, newBest = run.score > (previous?.bestScore ?: 0), duplicate = false)
    }
}

/** In-memory ledger used by the local store; serialises to JSON for SharedPreferences. */
class ProgressBook(
    private val progress: MutableMap<String, CharacterProgress> = mutableMapOf(),
    private val seenRunIds: LinkedHashSet<String> = linkedSetOf(),
) {
    val all: Map<String, CharacterProgress> get() = progress

    fun record(run: RunResult): RecordOutcome {
        val previous = progress[run.characterId]
        if (run.runId in seenRunIds) {
            return RecordOutcome(previous ?: CharacterProgress(run.characterId), newBest = false, duplicate = true)
        }
        val outcome = ProgressRules.outcome(previous, run)
        progress[run.characterId] = outcome.progress
        seenRunIds += run.runId
        while (seenRunIds.size > MAX_REMEMBERED_RUNS) seenRunIds.remove(seenRunIds.first())
        return outcome
    }

    fun markRead(characterId: String, storyId: String): CharacterProgress {
        val next = (progress[characterId] ?: CharacterProgress(characterId)).withRead(storyId)
        progress[characterId] = next
        return next
    }

    fun toJson(): String = JSONObject().apply {
        put("progress", JSONObject().apply {
            progress.forEach { (id, p) -> put(id, JSONObject(p.toMap() - "readStoryIds").put("readStoryIds", JSONArray(p.readStoryIds))) }
        })
        put("runIds", JSONArray(seenRunIds.toList()))
    }.toString()

    companion object {
        const val MAX_REMEMBERED_RUNS = 500

        /** Corrupt or missing data starts a fresh book instead of crashing. */
        fun fromJson(text: String?): ProgressBook {
            if (text.isNullOrBlank()) return ProgressBook()
            return try {
                val root = JSONObject(text)
                val map = mutableMapOf<String, CharacterProgress>()
                root.optJSONObject("progress")?.let { obj ->
                    obj.keys().forEach { id ->
                        val o = obj.getJSONObject(id)
                        map[id] = CharacterProgress.fromMap(id, o.keys().asSequence().associateWith { o.get(it) })
                    }
                }
                val ids = linkedSetOf<String>()
                root.optJSONArray("runIds")?.let { arr -> for (i in 0 until arr.length()) ids += arr.getString(i) }
                ProgressBook(map, ids)
            } catch (_: Exception) {
                ProgressBook()
            }
        }
    }
}
