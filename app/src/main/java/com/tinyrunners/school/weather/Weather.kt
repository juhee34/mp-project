package com.tinyrunners.school.weather

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.tinyrunners.school.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

/** Cities offered in settings; a fixed list avoids asking for location permission. */
enum class City(val label: Int, val latitude: Double, val longitude: Double) {
    SEOUL(R.string.city_seoul, 37.5665, 126.9780),
    INCHEON(R.string.city_incheon, 37.4563, 126.7052),
    DAEJEON(R.string.city_daejeon, 36.3504, 127.3845),
    DAEGU(R.string.city_daegu, 35.8714, 128.6014),
    GWANGJU(R.string.city_gwangju, 35.1595, 126.8526),
    BUSAN(R.string.city_busan, 35.1796, 129.0756),
    JEJU(R.string.city_jeju, 33.4996, 126.5312);
}

/** How the runner's sky looks. Mapped from WMO weather codes used by Open-Meteo. */
enum class Sky(val label: Int) {
    CLEAR(R.string.sky_clear), CLOUDY(R.string.sky_cloudy), FOG(R.string.sky_fog),
    RAIN(R.string.sky_rain), SNOW(R.string.sky_snow), STORM(R.string.sky_storm);

    companion object {
        fun fromWmo(code: Int): Sky = when (code) {
            0, 1 -> CLEAR
            2, 3 -> CLOUDY
            45, 48 -> FOG
            in 51..67, in 80..82 -> RAIN
            in 71..77, 85, 86 -> SNOW
            in 95..99 -> STORM
            else -> CLOUDY
        }
    }
}

data class WeatherReport(val city: City, val sky: Sky, val temperatureC: Double, val isDay: Boolean) {
    companion object {
        /** Parses the `current` block of an Open-Meteo forecast response. */
        fun parse(city: City, json: String): WeatherReport {
            val current = JSONObject(json).getJSONObject("current")
            return WeatherReport(
                city = city,
                sky = Sky.fromWmo(current.getInt("weather_code")),
                temperatureC = current.getDouble("temperature_2m"),
                isDay = current.optInt("is_day", 1) == 1,
            )
        }
    }
}

/** Reads and stores the weather settings. */
class WeatherSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("weather", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }
    var city: City
        get() = prefs.getString("city", null)?.let { name -> City.entries.firstOrNull { it.name == name } } ?: City.SEOUL
        set(value) { prefs.edit().putString("city", value.name).apply() }
}

/**
 * Open-Meteo forecast API (https://open-meteo.com). Free for non-commercial use, no API key, so no billing risk.
 * One request per game start; failures simply leave the default sky.
 */
object WeatherClient {
    private val io by lazy { Executors.newSingleThreadExecutor() }
    private val main by lazy { Handler(Looper.getMainLooper()) }

    fun url(city: City): String = String.format(Locale.US,
        "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=weather_code,temperature_2m,is_day&timezone=auto",
        city.latitude, city.longitude)

    fun fetch(city: City, done: (Result<WeatherReport>) -> Unit) {
        io.execute {
            val result = runCatching {
                val connection = URL(url(city)).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
                    WeatherReport.parse(city, connection.inputStream.bufferedReader().use { it.readText() })
                } finally {
                    connection.disconnect()
                }
            }
            main.post { done(result) }
        }
    }
}
