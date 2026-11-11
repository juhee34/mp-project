package com.tinyrunners.school.shop

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.firebase.firestore.FirebaseFirestore
import com.tinyrunners.school.auth.AuthService
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.progress.ProgressBook

/** Shop state plus the total distance the coins come from. */
data class Wallet(val state: ShopState, val totalMeters: Long) {
    val coins: Int get() = ShopRules.balance(totalMeters, state)
}

/**
 * Signed-in players keep their shop in Firestore (users/{uid}/shop/state) and buy inside a transaction that also
 * reads their saved distance, so the balance cannot be spent twice. Guests keep it on this device.
 * The worn item is also cached on the device so the game can draw it without waiting for the network.
 */
interface ShopStore {
    fun load(done: (Result<Wallet>) -> Unit)
    fun buy(itemId: String, done: (Result<Pair<ShopRules.Purchase, Wallet>>) -> Unit)
    fun equip(itemId: String, done: (Result<Wallet>) -> Unit)
    /** Saves bonus coins picked up on a run. */
    fun addFound(count: Int, done: (Result<Unit>) -> Unit)
    /** Spends [ShopRules.ITEM_PRICE] coins on one of [friendId]'s items, which goes to the bag. False: not enough coins. */
    fun buyItem(friendId: String, done: (Result<Pair<Boolean, Wallet>>) -> Unit)
    /** Adds a coin pack (demonstration purchase, no payment). */
    fun buyCoins(pack: ShopRules.CoinPack, done: (Result<Wallet>) -> Unit)

    companion object {
        fun forCurrentUser(context: Context): ShopStore {
            val uid = AuthService.currentUid(context)
            val friends = try { CharacterRepository(context).characters.map { it.id } } catch (_: Exception) { emptyList() }
            return if (uid != null) FirestoreShopStore(context, uid, friends) else LocalShopStore(context)
        }

        /** The worn decoration for the current account, read synchronously by the game screen. */
        fun equippedStyle(context: Context): TrailStyle =
            ShopCatalog.style(cache(context).getString(cacheKey(context), ShopCatalog.NONE_ID))

        /** The run music chosen in the shop for the current account (item id), read synchronously by the player. */
        fun equippedMusic(context: Context): String =
            cache(context).getString(cacheKey(context) + "_music", ShopCatalog.MAIN_MUSIC_ID) ?: ShopCatalog.MAIN_MUSIC_ID

        internal fun remember(context: Context, state: ShopState) {
            cache(context).edit().putString(cacheKey(context), state.equipped).putString(cacheKey(context) + "_music", state.music).apply()
        }

        private fun cache(context: Context) = context.applicationContext.getSharedPreferences("shop_equipped", Context.MODE_PRIVATE)
        private fun cacheKey(context: Context) = AuthService.currentUid(context) ?: "guest"
    }
}

class LocalShopStore(context: Context) : ShopStore {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("shop_guest", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())

    private fun state(): ShopState = try {
        prefs.getString(KEY, null)?.let { text ->
            val o = org.json.JSONObject(text)
            val owned = o.optJSONArray("owned")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
            ShopState.fromMap(mapOf("owned" to owned, "equipped" to o.optString("equipped"), "spent" to o.optInt("spent"),
                "music" to o.optString("music"), "found" to o.optInt("found"), "bought" to o.optInt("bought")))
        } ?: ShopState()
    } catch (_: Exception) { ShopState() }

    private fun save(s: ShopState) {
        val json = org.json.JSONObject().put("owned", org.json.JSONArray(s.owned.sorted())).put("equipped", s.equipped).put("spent", s.spent).put("music", s.music).put("found", s.found).put("bought", s.bought)
        prefs.edit().putString(KEY, json.toString()).apply()
        ShopStore.remember(app, s)
    }

    private fun totalMeters(): Long = try {
        val text = app.getSharedPreferences("progress_guest", Context.MODE_PRIVATE).getString("book", null)
        ProgressBook.fromJson(text).all.values.sumOf { it.totalMeters }
    } catch (_: Exception) { 0L }

    override fun load(done: (Result<Wallet>) -> Unit) {
        val wallet = Wallet(state(), totalMeters())
        ShopStore.remember(app, wallet.state)
        main.post { done(Result.success(wallet)) }
    }

    override fun buy(itemId: String, done: (Result<Pair<ShopRules.Purchase, Wallet>>) -> Unit) {
        val meters = totalMeters()
        val outcome = ShopRules.buy(state(), itemId, meters)
        if (outcome is ShopRules.Purchase.Bought) save(outcome.state)
        val wallet = Wallet(state(), meters)
        main.post { done(Result.success(outcome to wallet)) }
    }

    override fun equip(itemId: String, done: (Result<Wallet>) -> Unit) {
        val next = ShopRules.equip(state(), itemId)
        save(next)
        val wallet = Wallet(next, totalMeters())
        main.post { done(Result.success(wallet)) }
    }

    override fun addFound(count: Int, done: (Result<Unit>) -> Unit) {
        save(ShopRules.addFound(state(), count))
        main.post { done(Result.success(Unit)) }
    }

    override fun buyItem(friendId: String, done: (Result<Pair<Boolean, Wallet>>) -> Unit) {
        val meters = totalMeters()
        val next = ShopRules.buyItem(state(), meters)
        if (next != null) {
            save(next)
            com.tinyrunners.school.quest.LocalItemBagStore(app).add(friendId, 1) { }
        }
        val wallet = Wallet(state(), meters)
        main.post { done(Result.success((next != null) to wallet)) }
    }

    override fun buyCoins(pack: ShopRules.CoinPack, done: (Result<Wallet>) -> Unit) {
        save(ShopRules.addBought(state(), pack))
        val wallet = Wallet(state(), totalMeters())
        main.post { done(Result.success(wallet)) }
    }

    private companion object { const val KEY = "state" }
}

class FirestoreShopStore(context: Context, private val uid: String, private val friends: List<String>) : ShopStore {
    private val app = context.applicationContext
    private val db = FirebaseFirestore.getInstance()
    private val user get() = db.collection("users").document(uid)
    private val stateRef get() = user.collection("shop").document("state")

    override fun load(done: (Result<Wallet>) -> Unit) {
        stateRef.get().addOnSuccessListener { snap ->
            val state = ShopState.fromMap(snap.data)
            user.collection("progress").get().addOnSuccessListener { progress ->
                val meters = progress.documents.sumOf { (it.getLong("totalMeters") ?: 0L).coerceAtLeast(0L) }
                ShopStore.remember(app, state)
                done(Result.success(Wallet(state, meters)))
            }.addOnFailureListener { done(Result.failure(it)) }
        }.addOnFailureListener { done(Result.failure(it)) }
    }

    override fun buy(itemId: String, done: (Result<Pair<ShopRules.Purchase, Wallet>>) -> Unit) {
        db.runTransaction { tx ->
            val state = ShopState.fromMap(tx.get(stateRef).data)
            // Distance is read inside the same transaction, from each friend's progress document.
            val meters = friends.sumOf { id ->
                (tx.get(user.collection("progress").document(id)).getLong("totalMeters") ?: 0L).coerceAtLeast(0L)
            }
            val outcome = ShopRules.buy(state, itemId, meters)
            val after = if (outcome is ShopRules.Purchase.Bought) { tx.set(stateRef, outcome.state.toMap()); outcome.state } else state
            outcome to Wallet(after, meters)
        }.addOnSuccessListener { result ->
            ShopStore.remember(app, result.second.state)
            done(Result.success(result))
        }.addOnFailureListener { done(Result.failure(it)) }
    }

    override fun addFound(count: Int, done: (Result<Unit>) -> Unit) {
        db.runTransaction { tx ->
            tx.set(stateRef, ShopRules.addFound(ShopState.fromMap(tx.get(stateRef).data), count).toMap())
            Unit
        }.addOnSuccessListener { done(Result.success(Unit)) }.addOnFailureListener { done(Result.failure(it)) }
    }

    override fun buyItem(friendId: String, done: (Result<Pair<Boolean, Wallet>>) -> Unit) {
        val bagRef = user.collection("items").document("bag")
        // Coins and the bag change in one transaction, so a purchase is never half done.
        db.runTransaction { tx ->
            val state = ShopState.fromMap(tx.get(stateRef).data)
            val meters = friends.sumOf { id ->
                (tx.get(user.collection("progress").document(id)).getLong("totalMeters") ?: 0L).coerceAtLeast(0L)
            }
            val bag = com.tinyrunners.school.quest.ItemBag.fromMap(tx.get(bagRef).data)
            val next = ShopRules.buyItem(state, meters)
            if (next != null) {
                tx.set(stateRef, next.toMap())
                tx.set(bagRef, com.tinyrunners.school.quest.QuestRules.add(bag, friendId, 1).toMap())
            }
            (next != null) to Wallet(next ?: state, meters)
        }.addOnSuccessListener { done(Result.success(it)) }.addOnFailureListener { done(Result.failure(it)) }
    }

    override fun buyCoins(pack: ShopRules.CoinPack, done: (Result<Wallet>) -> Unit) {
        db.runTransaction { tx ->
            tx.set(stateRef, ShopRules.addBought(ShopState.fromMap(tx.get(stateRef).data), pack).toMap())
            Unit
        }.addOnSuccessListener { load(done) }.addOnFailureListener { done(Result.failure(it)) }
    }

    override fun equip(itemId: String, done: (Result<Wallet>) -> Unit) {
        db.runTransaction { tx ->
            val next = ShopRules.equip(ShopState.fromMap(tx.get(stateRef).data), itemId)
            tx.set(stateRef, next.toMap())
            next
        }.addOnSuccessListener { next ->
            ShopStore.remember(app, next)
            load(done)
        }.addOnFailureListener { done(Result.failure(it)) }
    }
}
