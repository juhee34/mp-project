package com.tinyrunners.school.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.widget.RemoteViews
import com.tinyrunners.school.R
import com.tinyrunners.school.characters.CharacterSprites
import com.tinyrunners.school.characters.SpriteFrame
import com.tinyrunners.school.data.CharacterRepository
import com.tinyrunners.school.opening.OpeningActivity
import com.tinyrunners.school.talk.ShortLineRepository
import com.tinyrunners.school.weather.City
import com.tinyrunners.school.weather.Sky
import com.tinyrunners.school.weather.WeatherClient
import com.tinyrunners.school.weather.WeatherReport
import com.tinyrunners.school.weather.WeatherSettings
import kotlin.math.roundToInt

/**
 * Home-screen widget (guide ③): today's friend, the real weather of the city chosen in settings and a word from the
 * friend; a tap opens the game. Two sizes, [SmallFriendWidget] (2×2) and [WideFriendWidget] (4×2).
 *
 * The weather is asked for only when the system updates the widget (every 3 hours, widget info XML) and kept, so
 * the widget can redraw at once with the last report (for example when a run with another friend starts). With
 * weather turned off in settings, the widget shows the friend and their word only and asks for nothing.
 */
abstract class FriendWidget(private val layout: Int) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        draw(context, manager, ids, layout, WeatherCache.read(context))
        val settings = WeatherSettings(context)
        if (!settings.enabled) return
        // Keep the receiver alive until the answer (at most the client's 5 s timeouts) and draw again with it.
        val pending = goAsync()
        WeatherClient.fetch(settings.city) { result ->
            try {
                result.onSuccess { report ->
                    WeatherCache.write(context, report)
                    draw(context, manager, ids, layout, report)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Redraws every placed widget with the last weather, without the network (a run just changed the friend). */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val report = WeatherCache.read(context)
            listOf(SmallFriendWidget::class.java to R.layout.widget_small, WideFriendWidget::class.java to R.layout.widget_wide).forEach { (cls, layout) ->
                val ids = manager.getAppWidgetIds(ComponentName(context, cls))
                if (ids.isNotEmpty()) draw(context, manager, ids, layout, report)
            }
        }

        private fun draw(context: Context, manager: AppWidgetManager, ids: IntArray, layout: Int, cached: WeatherReport?) {
            val friends = CharacterRepository(context).characters
            val id = TodayFriend.choose(friends.map { it.id }, TodayFriend.last(context), System.currentTimeMillis() / DAY_MS)
            val friend = friends.firstOrNull { it.id == id } ?: return
            val report = if (WeatherSettings(context).enabled) cached else null
            val views = RemoteViews(context.packageName, layout)
            sprite(context, friend.id)?.let { views.setImageViewBitmap(R.id.widget_friend, it) }
            views.setTextViewText(R.id.widget_name, context.getString(R.string.widget_today_friend, friend.name))
            views.setTextViewText(R.id.widget_weather, report?.let { weatherText(context, it) } ?: friend.home)
            views.setTextViewText(R.id.widget_line, "“" + lineFor(context, friend.id, report) + "”")
            views.setContentDescription(R.id.widget_root, context.getString(R.string.widget_description, friend.name))
            val open = Intent(context, OpeningActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            views.setOnClickPendingIntent(R.id.widget_root,
                PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            manager.updateAppWidget(ids, views)
        }

        /** "비 · 12°C · 서울". */
        private fun weatherText(context: Context, r: WeatherReport): String {
            val temp = if (r.temperatureC.isNaN()) "" else " · " + r.temperatureC.roundToInt() + "°C"
            return context.getString(r.sky.label) + temp + " · " + context.getString(r.city.label)
        }

        /**
         * A's weather line for the friend ("widget_rain" and so on in the short-line pack) when there is one, else
         * one of the friend's own at-home lines.
         */
        private fun lineFor(context: Context, friendId: String, report: WeatherReport?): String {
            val key = TodayFriend.weatherKey(report)
            return key?.let { ShortLineRepository.pick(context, friendId, "widget_$it") }
                ?: ShortLineRepository.pick(context, friendId, "home")
                ?: ShortLineRepository.pick(context, friendId, "select")
                ?: ""
        }

        /** The friend's standing frame, enlarged 4× with hard pixel edges (a widget image would blur a 48 px picture). */
        private fun sprite(context: Context, friendId: String): Bitmap? {
            val frame = CharacterSprites.load(context.resources, friendId)?.get(SpriteFrame.IDLE_0) ?: return null
            val size = CharacterSprites.SIZE * 4
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            Canvas(out).drawBitmap(frame, null, Rect(0, 0, size, size), Paint().apply { isFilterBitmap = false })
            return out
        }

        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}

class SmallFriendWidget : FriendWidget(R.layout.widget_small)
class WideFriendWidget : FriendWidget(R.layout.widget_wide)

/** The last real weather report the widget received (not the sky picked by hand in settings). */
internal object WeatherCache {
    private const val PREFS = "widget"

    fun write(context: Context, r: WeatherReport) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("city", r.city.name).putString("sky", r.sky.name)
            .putFloat("temp", r.temperatureC.toFloat()).putBoolean("day", r.isDay).apply()
    }

    fun read(context: Context): WeatherReport? {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val city = p.getString("city", null)?.let { n -> City.entries.firstOrNull { it.name == n } } ?: return null
        // A report for another city than the one chosen now is not shown.
        if (city != WeatherSettings(context).city) return null
        val sky = p.getString("sky", null)?.let { n -> Sky.entries.firstOrNull { it.name == n } } ?: return null
        return WeatherReport(city, sky, p.getFloat("temp", Float.NaN).toDouble(), p.getBoolean("day", true))
    }
}
