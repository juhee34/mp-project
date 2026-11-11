package com.tinyrunners.school.quest

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.firebase.firestore.FirebaseFirestore
import com.tinyrunners.school.auth.AuthService
import org.json.JSONArray
import org.json.JSONObject

/**
 * Collected items for the current account: Firestore users/{uid}/items/bag for signed-in players (updated in a
 * transaction), this device for guests. Callbacks arrive on the main thread.
 */
interface ItemBagStore {
    fun load(done: (Result<ItemBag>) -> Unit)
    fun add(friendId: String, count: Int, done: (Result<ItemBag>) -> Unit)
    /** Gives [QuestRules.NEEDED] items for [storyId]; the result is null when there were not enough. */
    fun give(friendId: String, storyId: String, done: (Result<ItemBag?>) -> Unit)

    companion object {
        fun forCurrentUser(context: Context): ItemBagStore {
            val uid = AuthService.currentUid(context)
            return if (uid != null) FirestoreItemBagStore(uid) else LocalItemBagStore(context)
        }
    }
}

class LocalItemBagStore(context: Context) : ItemBagStore {
    private val prefs = context.applicationContext.getSharedPreferences("items_guest", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())

    private fun read(): ItemBag = try {
        prefs.getString(KEY, null)?.let { text ->
            val o = JSONObject(text)
            val have = o.optJSONObject("have")?.let { h -> h.keys().asSequence().associateWith { h.optInt(it) } }.orEmpty()
            val given = o.optJSONArray("given")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
            ItemBag.fromMap(mapOf("have" to have, "given" to given))
        } ?: ItemBag()
    } catch (_: Exception) { ItemBag() }

    private fun write(bag: ItemBag) {
        val json = JSONObject().put("have", JSONObject(bag.have)).put("given", JSONArray(bag.given.sorted()))
        prefs.edit().putString(KEY, json.toString()).apply()
    }

    override fun load(done: (Result<ItemBag>) -> Unit) { val b = read(); main.post { done(Result.success(b)) } }

    override fun add(friendId: String, count: Int, done: (Result<ItemBag>) -> Unit) {
        val b = QuestRules.add(read(), friendId, count); write(b); main.post { done(Result.success(b)) }
    }

    override fun give(friendId: String, storyId: String, done: (Result<ItemBag?>) -> Unit) {
        val b = QuestRules.give(read(), friendId, storyId)
        if (b != null) write(b)
        main.post { done(Result.success(b)) }
    }

    private companion object { const val KEY = "bag" }
}

class FirestoreItemBagStore(uid: String) : ItemBagStore {
    private val db = FirebaseFirestore.getInstance()
    private val ref = db.collection("users").document(uid).collection("items").document("bag")

    override fun load(done: (Result<ItemBag>) -> Unit) {
        ref.get().addOnSuccessListener { done(Result.success(ItemBag.fromMap(it.data))) }
            .addOnFailureListener { done(Result.failure(it)) }
    }

    override fun add(friendId: String, count: Int, done: (Result<ItemBag>) -> Unit) {
        db.runTransaction { tx ->
            val next = QuestRules.add(ItemBag.fromMap(tx.get(ref).data), friendId, count)
            tx.set(ref, next.toMap())
            next
        }.addOnSuccessListener { done(Result.success(it)) }.addOnFailureListener { done(Result.failure(it)) }
    }

    override fun give(friendId: String, storyId: String, done: (Result<ItemBag?>) -> Unit) {
        db.runTransaction { tx ->
            val next = QuestRules.give(ItemBag.fromMap(tx.get(ref).data), friendId, storyId)
            if (next != null) tx.set(ref, next.toMap())
            next
        }.addOnSuccessListener { done(Result.success(it)) }.addOnFailureListener { done(Result.failure(it)) }
    }
}
