package com.tinyrunners.school.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherChoiceTest {
    private val real = WeatherReport(City.SEOUL, Sky.CLEAR, 17.0, isDay = true)

    @Test fun nothingChosenKeepsTheRealWeather() {
        assertEquals(real, WeatherChoice.apply(real, City.SEOUL, null, null))
        assertNull(WeatherChoice.apply(null, City.SEOUL, null, null))
    }

    @Test fun aChosenSkyReplacesOnlyTheSky() {
        val r = WeatherChoice.apply(real, City.SEOUL, Sky.SNOW, null)!!
        assertEquals(Sky.SNOW, r.sky)
        assertTrue(r.isDay)
        assertEquals(17.0, r.temperatureC, 0.0)
        assertTrue(r.chosen)
    }

    @Test fun aChosenNightWorksWithoutTheNetwork() {
        val r = WeatherChoice.apply(null, City.BUSAN, Sky.RAIN, false)!!
        assertEquals(Sky.RAIN, r.sky)
        assertFalse(r.isDay)
        assertTrue(r.temperatureC.isNaN())
        assertEquals(City.BUSAN, r.city)
    }
}
