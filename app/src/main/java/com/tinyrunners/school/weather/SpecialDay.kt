package com.tinyrunners.school.weather

/**
 * A special weather day — rain (or a storm), snow, very hot or very cold — for the postcards that are earned on such
 * a day. Pure, so it is unit tested.
 */
object SpecialDay {
    const val HOT_C = 30.0
    const val COLD_C = -5.0

    /** "rain", "snow", "hot" or "cold" for a special day, null for an ordinary one (or no report). */
    fun key(report: WeatherReport?): String? {
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
}
