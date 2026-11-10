package com.tinyrunners.school.talk

import android.content.Context
import com.tinyrunners.school.auth.AuthService

/**
 * Remembers, per account on this device, whether the world prologue and each friend's talk were already seen.
 * New players see the prologue once after their first login and each friend's talk before their first run with
 * that friend; after that the app goes straight to the selection or the run.
 */
class TalkProgress(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("talk_seen", Context.MODE_PRIVATE)
    private val account = AuthService.currentUid(context) ?: GUEST

    val shouldShowPrologue: Boolean get() = !prefs.getBoolean(prologueKey(account), false)
    fun markPrologueSeen() { prefs.edit().putBoolean(prologueKey(account), true).apply() }

    fun shouldShowTalk(characterId: String): Boolean = !prefs.getBoolean(talkKey(account, characterId), false)
    fun markTalkSeen(characterId: String) { prefs.edit().putBoolean(talkKey(account, characterId), true).apply() }

    /** Shows the prologue and every friend talk again for this account (settings, e.g. before a demo). */
    fun resetAll() {
        val mine = prefs.all.keys.filter { it == prologueKey(account) || it.startsWith("talk_${account}_") }
        prefs.edit().apply { mine.forEach { remove(it) } }.apply()
    }

    companion object {
        private const val GUEST = "guest"
        fun prologueKey(account: String) = "prologue_$account"
        fun talkKey(account: String, characterId: String) = "talk_${account}_$characterId"
    }
}
