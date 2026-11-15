package com.tinyrunners.school.widget

import com.tinyrunners.school.weather.City
import com.tinyrunners.school.weather.Sky
import com.tinyrunners.school.weather.WeatherReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayFriendTest {
    private val six = listOf("mint", "cheese", "popo", "lumi", "nunu", "piko")

    @Test fun lastRunFriendComesFirst() {
        assertEquals("popo", TodayFriend.choose(six, "popo", 0))
        assertEquals("popo", TodayFriend.choose(six, "popo", 12345))
    }

    @Test fun withoutARunTheFriendsTakeTurnsByDay() {
        val week = (0L until 6L).map { TodayFriend.choose(six, null, 20_000 + it) }
        assertEquals("each friend once in six days", six.toSet(), week.toSet())
        assertEquals(TodayFriend.choose(six, null, 20_000), TodayFriend.choose(six, null, 20_006))
    }

    @Test fun unknownLastFriendFallsBackToTurns() {
        assertEquals(TodayFriend.choose(six, null, 7), TodayFriend.choose(six, "someone", 7))
        assertNull(TodayFriend.choose(emptyList(), "mint", 0))
    }

    @Test fun weatherKeys() {
        fun r(sky: Sky, t: Double) = WeatherReport(City.SEOUL, sky, t, true)
        assertNull(TodayFriend.weatherKey(null))
        assertEquals("hot", TodayFriend.weatherKey(r(Sky.CLEAR, 31.0)))
        assertEquals("clear", TodayFriend.weatherKey(r(Sky.CLEAR, 12.0)))
        assertEquals("clear", TodayFriend.weatherKey(r(Sky.CLEAR, Double.NaN)))
        assertEquals("cloudy", TodayFriend.weatherKey(r(Sky.FOG, 5.0)))
        assertEquals("rain", TodayFriend.weatherKey(r(Sky.STORM, 20.0)))
        assertEquals("snow", TodayFriend.weatherKey(r(Sky.SNOW, -3.0)))
    }
}
