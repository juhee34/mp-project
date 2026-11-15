package com.tinyrunners.school.notice

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.opening.OpeningActivity
import com.tinyrunners.school.talk.ShortLineRepository
import com.tinyrunners.school.weather.WeatherClient
import com.tinyrunners.school.weather.WeatherReport
import com.tinyrunners.school.weather.WeatherSettings
import com.tinyrunners.school.widget.TodayFriend
import kotlin.math.roundToInt

/** The player's choices for the friend's weather note: off by default, morning (8:00) or evening (19:00). */
class NoticeSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("notice", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }
    var evening: Boolean
        get() = prefs.getBoolean("evening", false)
        set(value) { prefs.edit().putBoolean("evening", value).apply() }
    var lastSentDay: Long
        get() = prefs.getLong("lastSentDay", -1L)
        set(value) { prefs.edit().putLong("lastSentDay", value).apply() }
    val hour: Int get() = if (evening) EVENING_HOUR else MORNING_HOUR

    companion object {
        const val MORNING_HOUR = 8
        const val EVENING_HOUR = 19
    }
}

/**
 * The friend's weather note (guide ④): once a day at the chosen hour the app looks at the real weather of the city
 * chosen in settings, and only on a special day (NoticeRules) today's friend sends one kind word. A tap opens the game.
 *
 * Scheduled as a one-hour window alarm at the chosen hour (no exact-alarm permission; the system may move it within
 * that hour to save battery), set again for the next day each time it rings, and after a restart. A plain daily
 * repeating alarm was not used: the system may move that one by up to 18 hours (a morning note at night). Turning it
 * off cancels the alarm.
 */
object FriendNotice {
    private const val CHANNEL = "friend_weather"
    private const val NOTE_ID = 7001
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val WINDOW_MS = 60L * 60 * 1000

    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val settings = NoticeSettings(context)
        val pending = alarmIntent(context)
        alarms.cancel(pending)
        if (!settings.enabled) return
        alarms.setWindow(AlarmManager.RTC_WAKEUP, NoticeRules.nextAt(System.currentTimeMillis(), settings.hour), WINDOW_MS, pending)
    }

    /** Whether the app may show notifications (asked for on Android 13 and later). */
    fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** The daily check: real weather, then a note when the day is special and none was sent today. */
    fun check(context: Context, done: () -> Unit) {
        val settings = NoticeSettings(context)
        if (!settings.enabled) { done(); return }
        WeatherClient.fetch(WeatherSettings(context).city) { result ->
            try {
                val report = result.getOrNull()
                val key = NoticeRules.specialKey(report)
                val today = System.currentTimeMillis() / DAY_MS
                if (report != null && NoticeRules.shouldSend(settings.enabled, key, settings.lastSentDay, today) && show(context, report, key!!)) {
                    settings.lastSentDay = today
                }
            } finally {
                done()
            }
        }
    }

    /**
     * For the presentation (settings "지금 한 번 보내 보기"): sends a note now with today's real weather even on an
     * ordinary day, and does not count as today's note.
     */
    fun sendTry(context: Context, done: (Boolean) -> Unit) {
        WeatherClient.fetch(WeatherSettings(context).city) { result ->
            val report = result.getOrNull()
            done(show(context, report, NoticeRules.specialKey(report) ?: "plain"))
        }
    }

    private fun show(context: Context, report: WeatherReport?, key: String): Boolean {
        if (!allowed(context)) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notice_channel),
            NotificationManager.IMPORTANCE_DEFAULT).apply { description = context.getString(R.string.notice_channel_description) })
        val friends = CharacterRepository(context).characters
        val id = TodayFriend.choose(friends.map { it.id }, TodayFriend.last(context), System.currentTimeMillis() / DAY_MS)
        val friend = friends.firstOrNull { it.id == id } ?: return false
        // A's note for this weather when there is one, else one of the friend's own at-home lines.
        val line = ShortLineRepository.pick(context, friend.id, "notice_$key")
            ?: ShortLineRepository.pick(context, friend.id, "home") ?: friend.home
        val title = when (key) {
            "rain" -> context.getString(R.string.notice_title_rain, friend.name)
            "snow" -> context.getString(R.string.notice_title_snow, friend.name)
            "hot" -> context.getString(R.string.notice_title_hot, friend.name)
            "cold" -> context.getString(R.string.notice_title_cold, friend.name)
            else -> context.getString(R.string.notice_title_plain, friend.name)
        }
        val weather = report?.let {
            context.getString(it.sky.label) + (if (it.temperatureC.isNaN()) "" else " · " + it.temperatureC.roundToInt() + "°C") +
                " · " + context.getString(it.city.label)
        }
        val open = PendingIntent.getActivity(context, 0,
            Intent(context, OpeningActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val body = "“$line”"
        val note = android.app.Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notice_leaf)
            .setLargeIcon(sprite(context, friend.id))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setSubText(weather)
            .setColor(0xFF1E5A2A.toInt())
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTE_ID, note)
        return true
    }

    private fun alarmIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(context, 0, Intent(context, NoticeReceiver::class.java).setAction(ACTION_CHECK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun sprite(context: Context, friendId: String): Bitmap? {
        val frame = CharacterSprites.load(context.resources, friendId)?.get(SpriteFrame.IDLE_0) ?: return null
        val size = CharacterSprites.SIZE * 4
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
            Canvas(it).drawBitmap(frame, null, Rect(0, 0, size, size), Paint().apply { isFilterBitmap = false })
        }
    }

    const val ACTION_CHECK = "com.tinyrunners.school.notice.CHECK"
}

/** The daily alarm (check the weather, maybe send the note) and the restart (set the alarm again). */
class NoticeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> FriendNotice.schedule(context)
            FriendNotice.ACTION_CHECK -> {
                val pending = goAsync()
                // Tomorrow's alarm first, so a failed check never stops the notes.
                FriendNotice.schedule(context)
                FriendNotice.check(context) { pending.finish() }
            }
        }
    }
}
