package com.tinyrunners.school.game

import com.tinyrunners.school.R
import com.tinyrunners.school.weather.Sky
import com.tinyrunners.school.weather.WeatherReport

/**
 * How hard the road is under today's weather (the same for all six friends): a clear day is the easiest (slower,
 * more room between obstacles), a night of rain or thunderstorm the hardest (faster, a little less room). Every
 * level still keeps one jump enough for a box and the double jump for a tower (RunnerEngineTest). Pure, so it is unit
 * tested.
 */
enum class Difficulty(val speed: Float, val reaction: Float, val label: Int) {
    EASY(0.88f, 0.2f, R.string.difficulty_easy),
    NORMAL(1f, 0f, R.string.difficulty_normal),
    HARDER(1.07f, -0.05f, R.string.difficulty_harder),
    HARD(1.12f, -0.08f, R.string.difficulty_hard),
    HARDEST(1.18f, -0.1f, R.string.difficulty_hardest);

    companion object {
        fun of(report: WeatherReport?): Difficulty {
            report ?: return NORMAL
            val night = !report.isDay
            return when (report.sky) {
                Sky.CLEAR -> if (night) NORMAL else EASY
                Sky.CLOUDY, Sky.FOG -> if (night) HARDER else NORMAL
                Sky.SNOW -> if (night) HARD else HARDER
                Sky.RAIN -> if (night) HARDEST else HARDER
                Sky.STORM -> if (night) HARDEST else HARD
            }
        }
    }
}
