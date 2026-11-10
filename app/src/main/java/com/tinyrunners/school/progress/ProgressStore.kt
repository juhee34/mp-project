package com.tinyrunners.school.progress

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.tinyrunners.school.auth.AuthService

/** Saves and reads run records. Callbacks always arrive on the main thread. */
interface ProgressStore {
    /** Human-readable place the data lives, shown on the records screen. */
    val location: Location
    fun record(run: RunResult, done: (Result<RecordOutcome>) -> Unit)
    fun loadAll(done: (Result<Map<String, CharacterProgress>>) -> Unit)
    /** Remembers that a story was seen. Repeating it is harmless. */
    fun markStoryRead(characterId: String, storyId: String, done: (Result<Unit>) -> Unit)

    enum class Location { CLOUD, DEVICE }

    companion object {
        /** Firestore for a signed-in Firebase user, otherwise this device only. */
        fun forCurrentUser(context: Context): ProgressStore {
            val uid = AuthService.currentUid(context)
            return if (uid != null) FirestoreProgressStore(uid) else LocalProgressStore(context, "guest")
        }
    }
}

class LocalProgressStore(context: Context, owner: String) : ProgressStore {
    private val prefs = context.applicationContext.getSharedPreferences("progress_$owner", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    override val location = ProgressStore.Location.DEVICE

    override fun record(run: RunResult, done: (Result<RecordOutcome>) -> Unit) {
        val book = ProgressBook.fromJson(prefs.getString(KEY, null))
        val outcome = book.record(run)
        if (!outcome.duplicate) prefs.edit().putString(KEY, book.toJson()).apply()
        main.post { done(Result.success(outcome)) }
    }

    override fun loadAll(done: (Result<Map<String, CharacterProgress>>) -> Unit) {
        val all = ProgressBook.fromJson(prefs.getString(KEY, null)).all
        main.post { done(Result.success(all)) }
    }

    override fun markStoryRead(characterId: String, storyId: String, done: (Result<Unit>) -> Unit) {
        val book = ProgressBook.fromJson(prefs.getString(KEY, null))
        book.markRead(characterId, storyId)
        prefs.edit().putString(KEY, book.toJson()).apply()
        main.post { done(Result.success(Unit)) }
    }

    private companion object { const val KEY = "book" }
}

/**
 * users/{uid}/runs/{runId} and users/{uid}/progress/{characterId}.
 * The run document is written in the same transaction as the progress update, so a repeated runId is ignored.
 */
class FirestoreProgressStore(private val uid: String) : ProgressStore {
    private val db = FirebaseFirestore.getInstance()
    override val location = ProgressStore.Location.CLOUD

    override fun record(run: RunResult, done: (Result<RecordOutcome>) -> Unit) {
        val user = db.collection("users").document(uid)
        val runRef = user.collection("runs").document(run.runId)
        val progressRef = user.collection("progress").document(run.characterId)
        db.runTransaction { tx ->
            val existingRun = tx.get(runRef)
            val progressSnap = tx.get(progressRef)
            val previous = progressSnap.data?.let { CharacterProgress.fromMap(run.characterId, it) }
            if (existingRun.exists()) {
                RecordOutcome(previous ?: CharacterProgress(run.characterId), newBest = false, duplicate = true)
            } else {
                val outcome = ProgressRules.outcome(previous, run)
                tx.set(runRef, mapOf(
                    "characterId" to run.characterId, "meters" to run.meters, "score" to run.score,
                    "cleared" to run.cleared, "savedAt" to FieldValue.serverTimestamp(),
                ))
                tx.set(progressRef, outcome.progress.toMap())
                outcome
            }
        }.addOnSuccessListener { done(Result.success(it)) }
            .addOnFailureListener { done(Result.failure(it)) }
    }

    override fun loadAll(done: (Result<Map<String, CharacterProgress>>) -> Unit) {
        db.collection("users").document(uid).collection("progress").get()
            .addOnSuccessListener { snap ->
                done(Result.success(snap.documents.associate { it.id to CharacterProgress.fromMap(it.id, it.data.orEmpty()) }))
            }
            .addOnFailureListener { done(Result.failure(it)) }
    }

    override fun markStoryRead(characterId: String, storyId: String, done: (Result<Unit>) -> Unit) {
        // Merge keeps run statistics; arrayUnion ignores an ID that is already there.
        db.collection("users").document(uid).collection("progress").document(characterId)
            .set(mapOf("characterId" to characterId, "readStoryIds" to FieldValue.arrayUnion(storyId)), SetOptions.merge())
            .addOnSuccessListener { done(Result.success(Unit)) }
            .addOnFailureListener { done(Result.failure(it)) }
    }
}
