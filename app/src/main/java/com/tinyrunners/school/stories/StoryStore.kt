package com.tinyrunners.school.stories

import android.content.Context
import com.tinyrunners.school.auth.AuthService

/** A-owned reading metadata only. Run records and Firestore schema are untouched. */
class StoryStore internal constructor(private val prefs: android.content.SharedPreferences) {
    constructor(context: Context) : this(context.applicationContext.getSharedPreferences(
        preferenceName(AuthService.currentUid(context)), Context.MODE_PRIVATE))
    private fun ids(key: String): Set<String> = try { prefs.getStringSet(key, emptySet())?.toSet().orEmpty() }
        catch (_: ClassCastException) { emptySet() }
    val read: Set<String> get() = ids("read")
    val unlocked: Set<String> get() = ids("unlocked")
    fun unlock(id: String): Boolean = prefs.edit().putStringSet("unlocked", unlocked + id).commit()
    fun markRead(id: String): Boolean = prefs.edit().putStringSet("read", read + id)
        .putStringSet("unlocked", unlocked + id).remove("page_$id").commit()
    fun page(id: String): Int = try { prefs.getInt("page_$id", 0) } catch (_: ClassCastException) { 0 }
    fun savePage(id: String, page: Int): Boolean = prefs.edit().putInt("page_$id", page).commit()

    companion object {
        internal fun preferenceName(uid: String?): String = "story_reading_v1_" + (uid?.let { "user_$it" } ?: "guest")
    }
}
