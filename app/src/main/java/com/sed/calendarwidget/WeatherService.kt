package com.sed.calendarwidget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object WeatherService {
    private val executor = Executors.newSingleThreadExecutor()

    fun refresh(context: Context, callback: ((String) -> Unit)? = null) {
        val app = context.applicationContext
        val fine = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            callback?.invoke("{\"error\":\"Location permission is required\"}")
            return
        }
        val manager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val prefs = app.getSharedPreferences("sed_weather", Context.MODE_PRIVATE)
        val cached = runCatching {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            providers.mapNotNull { provider ->
                if (manager.isProviderEnabled(provider)) runCatching { manager.getLastKnownLocation(provider) }.getOrNull() else null
            }.maxByOrNull { it.time }
        }.getOrNull()

        fun useLocation(location: Location?) {
            if (location == null) {
                callback?.invoke(prefs.getString("weather_json", "{\"error\":\"Waiting for GPS location\"}") ?: "{\"error\":\"Waiting for GPS location\"}")
                return
            }
            prefs.edit().putString("latitude", location.latitude.toString()).putString("longitude", location.longitude.toString()).apply()
            fetchWeather(app, location.latitude, location.longitude, callback)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                manager.getCurrentLocation(
                    if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER,
                    null, app.mainExecutor
                ) { location -> useLocation(location ?: cached) }
            } catch (_: SecurityException) { useLocation(cached) }
            catch (_: Exception) { useLocation(cached) }
        } else {
            useLocation(cached)
        }
    }

    private fun fetchWeather(context: Context, lat: Double, lon: Double, callback: ((String) -> Unit)?) {
        executor.execute {
            val result = try {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000
                    readTimeout = 12000
                    requestMethod = "GET"
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val root = JSONObject(body)
                val current = root.getJSONObject("current")
                val daily = root.getJSONObject("daily")
                val code = current.optInt("weather_code", -1)
                val description = when (code) {
                    0 -> "Clear sky"
                    1 -> "Mainly clear"
                    2 -> "Partly cloudy"
                    3 -> "Overcast"
                    45, 48 -> "Fog"
                    51, 53, 55 -> "Drizzle"
                    56, 57 -> "Freezing drizzle"
                    61, 63, 65 -> "Rain"
                    66, 67 -> "Freezing rain"
                    71, 73, 75, 77 -> "Snow"
                    80, 81, 82 -> "Rain showers"
                    85, 86 -> "Snow showers"
                    95, 96, 99 -> "Thunderstorm"
                    else -> "Weather unavailable"
                }
                JSONObject().apply {
                    put("temperature", current.optDouble("temperature_2m"))
                    put("feelsLike", current.optDouble("apparent_temperature"))
                    put("humidity", current.optInt("relative_humidity_2m"))
                    put("wind", current.optDouble("wind_speed_10m"))
                    put("precipitation", current.optDouble("precipitation"))
                    put("code", code)
                    put("description", description)
                    put("isDay", current.optInt("is_day") == 1)
                    put("high", daily.getJSONArray("temperature_2m_max").optDouble(0))
                    put("low", daily.getJSONArray("temperature_2m_min").optDouble(0))
                    put("rainChance", daily.getJSONArray("precipitation_probability_max").optInt(0))
                    put("updatedAt", System.currentTimeMillis())
                    put("latitude", lat)
                    put("longitude", lon)
                }.toString()
            } catch (e: Exception) {
                JSONObject().put("error", "Weather update failed: ${e.message ?: "network unavailable"}").toString()
            }
            context.getSharedPreferences("sed_weather", Context.MODE_PRIVATE).edit().putString("weather_json", result).apply()
            callback?.invoke(result)
            val providers = listOf(
                CalendarWidgetProvider::class.java, WaterCalendarWidgetProvider::class.java,
                BlurCalendarWidgetProvider::class.java, TransparentCalendarWidgetProvider::class.java,
                MiniClockWidgetProvider::class.java, ClockStripWidgetProvider::class.java,
                YearProgressLineWidgetProvider::class.java, CountdownStripWidgetProvider::class.java,
                DateSquareWidgetProvider::class.java, WeekDayCardWidgetProvider::class.java,
                CountdownCardWidgetProvider::class.java, YearProgressCardWidgetProvider::class.java,
                VerticalCountdownWidgetProvider::class.java, YearStatisticsWidgetProvider::class.java,
                MinimalProgressWidgetProvider::class.java, DualInfoWidgetProvider::class.java
            )
            providers.forEach { provider ->
                context.sendBroadcast(android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setClass(context, provider))
            }
        }
    }
}
