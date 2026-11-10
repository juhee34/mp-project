package com.tinyrunners.school.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherTest {
    @Test fun wmoCodesMapToSky() {
        assertEquals(Sky.CLEAR, Sky.fromWmo(0))
        assertEquals(Sky.CLOUDY, Sky.fromWmo(3))
        assertEquals(Sky.FOG, Sky.fromWmo(45))
        assertEquals(Sky.RAIN, Sky.fromWmo(61))
        assertEquals(Sky.RAIN, Sky.fromWmo(81))
        assertEquals(Sky.SNOW, Sky.fromWmo(73))
        assertEquals(Sky.SNOW, Sky.fromWmo(86))
        assertEquals(Sky.STORM, Sky.fromWmo(95))
        assertEquals("unknown codes fall back to a calm sky", Sky.CLOUDY, Sky.fromWmo(42))
    }

    /** Shape copied from a real Open-Meteo response (Seoul, 2026-11-09). */
    @Test fun parsesOpenMeteoCurrentBlock() {
        val json = """{"latitude":37.55,"longitude":127.0,"timezone":"Asia/Seoul",
            "current_units":{"weather_code":"wmo code","temperature_2m":"°C","is_day":""},
            "current":{"time":"2026-11-09T23:30","interval":900,"weather_code":0,"temperature_2m":5.2,"is_day":0}}"""
        val r = WeatherReport.parse(City.SEOUL, json)
        assertEquals(Sky.CLEAR, r.sky)
        assertEquals(5.2, r.temperatureC, 0.001)
        assertFalse(r.isDay)
    }

    @Test fun requestUrlUsesCityCoordinatesWithoutKey() {
        val url = WeatherClient.url(City.BUSAN)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?"))
        assertTrue(url.contains("latitude=35.1796") && url.contains("longitude=129.0756"))
        assertTrue(url.contains("current=weather_code,temperature_2m,is_day"))
        assertFalse(url.contains("key", ignoreCase = true))
    }
}
