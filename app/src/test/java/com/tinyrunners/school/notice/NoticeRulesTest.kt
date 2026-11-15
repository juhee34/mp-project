package com.tinyrunners.school.notice

import com.tinyrunners.school.weather.City
import com.tinyrunners.school.weather.Sky
import com.tinyrunners.school.weather.WeatherReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NoticeRulesTest {
    private fun r(sky: Sky, t: Double) = WeatherReport(City.SEOUL, sky, t, true)

    @Test fun onlySpecialDaysHaveAKey() {
        assertNull(NoticeRules.specialKey(null))
        assertNull("an ordinary clear day", NoticeRules.specialKey(r(Sky.CLEAR, 18.0)))
        assertNull(NoticeRules.specialKey(r(Sky.CLOUDY, 10.0)))
        assertNull(NoticeRules.specialKey(r(Sky.FOG, Double.NaN)))
        assertEquals("rain", NoticeRules.specialKey(r(Sky.RAIN, 15.0)))
        assertEquals("rain", NoticeRules.specialKey(r(Sky.STORM, 25.0)))
        assertEquals("snow", NoticeRules.specialKey(r(Sky.SNOW, -8.0)))
        assertEquals("hot", NoticeRules.specialKey(r(Sky.CLEAR, 30.0)))
        assertEquals("cold", NoticeRules.specialKey(r(Sky.CLEAR, -5.0)))
        assertNull(NoticeRules.specialKey(r(Sky.CLEAR, 29.9)))
    }

    @Test fun atMostOnceADayAndOnlyWhenOn() {
        assertTrue(NoticeRules.shouldSend(true, "rain", lastSentDay = 99, today = 100))
        assertFalse("already sent today", NoticeRules.shouldSend(true, "rain", lastSentDay = 100, today = 100))
        assertFalse("switched off", NoticeRules.shouldSend(false, "rain", lastSentDay = 0, today = 100))
        assertFalse("ordinary day", NoticeRules.shouldSend(true, null, lastSentDay = 0, today = 100))
    }

    @Test fun nextTimeIsTodayOrTomorrow() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        cal.clear(); cal.set(2026, Calendar.NOVEMBER, 15, 7, 30)
        val morning = cal.timeInMillis
        cal.clear(); cal.set(2026, Calendar.NOVEMBER, 15, 8, 0)
        assertEquals("8:00 later today", cal.timeInMillis, NoticeRules.nextAt(morning, 8, Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))))
        cal.clear(); cal.set(2026, Calendar.NOVEMBER, 15, 20, 0)
        val evening = cal.timeInMillis
        cal.clear(); cal.set(2026, Calendar.NOVEMBER, 16, 19, 0)
        assertEquals("19:00 tomorrow", cal.timeInMillis, NoticeRules.nextAt(evening, 19, Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))))
    }
}
