package com.tinyrunners.school.widget

import android.content.Context
import com.tinyrunners.school.weather.Sky
import com.tinyrunners.school.weather.WeatherReport

/**
 * Who the home-screen widget shows and what they say. Pure parts ([choose], [weatherKey]) are unit tested.
 *
 * Rule (B's default until A and B settle it, guide "함께 정할 것"): the friend the player last ran with; before any
 * run, the six take turns day by day.
 */
object TodayFriend {
    private const val PREFS = "widget"
    private const val KEY_LAST = "lastFriend"

    /** Remembers the friend of a run that just started. */
    fun remember(context: Context, friendId: String) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAST, friendId).apply()
    }

    fun last(context: Context): String? =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST, null)

    /** The last friend when they are still one of [ids], else a different friend each day ([epochDay] = days since 1970). */
    fun choose(ids: List<String>, last: String?, epochDay: Long): String? {
        if (ids.isEmpty()) return null
        if (last != null && last in ids) return last
        return ids[Math.floorMod(epochDay, ids.size.toLong()).toInt()]
    }

    /**
     * Which widget line fits the weather: hot days (28 °C and up) first, then the sky. Fog counts as cloudy and storms
     * as rain. Null without a report.
     */
    fun weatherKey(report: WeatherReport?): String? {
        report ?: return null
        if (!report.temperatureC.isNaN() && report.temperatureC >= HOT_C) return "hot"
        return when (report.sky) {
            Sky.CLEAR -> "clear"
            Sky.CLOUDY, Sky.FOG -> "cloudy"
            Sky.RAIN, Sky.STORM -> "rain"
            Sky.SNOW -> "snow"
        }
    }

    const val HOT_C = 28.0
}
