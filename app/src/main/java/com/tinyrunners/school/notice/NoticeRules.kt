package com.tinyrunners.school.notice

import com.tinyrunners.school.weather.Sky
import com.tinyrunners.school.weather.WeatherReport
import java.util.Calendar

/**
 * When the friend's weather note is sent (guide ④). Pure, so it is unit tested.
 *
 * Only on a special day — rain (or a storm), snow, very hot or very cold — and at most once a day. Never a reminder
 * to play, never anything that is lost by not opening the game.
 */
object NoticeRules {
    const val HOT_C = 30.0
    const val COLD_C = -5.0

    /** "rain", "snow", "hot" or "cold" for a special day, null for an ordinary one (or no report). */
    fun specialKey(report: WeatherReport?): String? {
        report ?: return null
        val t = report.temperatureC
        return when {
            report.sky == Sky.SNOW -> "snow"
            report.sky == Sky.RAIN || report.sky == Sky.STORM -> "rain"
            !t.isNaN() && t >= HOT_C -> "hot"
            !t.isNaN() && t <= COLD_C -> "cold"
            else -> null
        }
    }

    /** Whether to send today: switched on, a special day, and nothing sent yet on [today] (days since 1970). */
    fun shouldSend(enabled: Boolean, key: String?, lastSentDay: Long, today: Long): Boolean =
        enabled && key != null && lastSentDay != today

    /** The next time (ms) at [hour]:00 after [now] in [calendar]'s time zone: today if it is still ahead, else tomorrow. */
    fun nextAt(now: Long, hour: Int, calendar: Calendar = Calendar.getInstance()): Long {
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }
}
