package com.tinyrunners.school.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpecialDayTest {
    private fun report(sky: Sky, t: Double) = WeatherReport(City.entries.first(), sky, t, isDay = true)

    @Test fun rainSnowHeatAndColdAreSpecial() {
        assertEquals("rain", SpecialDay.key(report(Sky.RAIN, 15.0)))
        assertEquals("rain", SpecialDay.key(report(Sky.STORM, 15.0)))
        assertEquals("snow", SpecialDay.key(report(Sky.SNOW, -1.0)))
        assertEquals("hot", SpecialDay.key(report(Sky.CLEAR, 31.0)))
        assertEquals("cold", SpecialDay.key(report(Sky.CLEAR, -6.0)))
    }

    @Test fun anOrdinaryDayOrNoReportIsNot() {
        assertNull(SpecialDay.key(report(Sky.CLEAR, 18.0)))
        assertNull(SpecialDay.key(report(Sky.CLEAR, Double.NaN)))
        assertNull(SpecialDay.key(null))
    }
}
