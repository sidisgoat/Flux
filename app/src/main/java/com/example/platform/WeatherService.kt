package com.example.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class WeatherPresetCity(
    val name: String,
    val latitude: Float,
    val longitude: Float
)

data class WeatherSnapshot(
    val city: String,
    val temperatureC: Int,
    val conditionText: String,
    val isLiveNetworkResult: Boolean
)

object WeatherService {

    val presetCities = listOf(
        WeatherPresetCity("San Francisco", 37.7749f, -122.4194f),
        WeatherPresetCity("Surat", 21.1702f, 72.8311f),
        WeatherPresetCity("London", 51.5074f, -0.1278f),
        WeatherPresetCity("Tokyo", 35.6762f, 139.6503f),
        WeatherPresetCity("New York", 40.7128f, -74.0060f),
        WeatherPresetCity("Berlin", 52.5200f, 13.4050f),
        WeatherPresetCity("Mumbai", 19.0760f, 72.8777f),
        WeatherPresetCity("Sydney", -33.8688f, 151.2093f)
    )

    suspend fun fetchCurrentWeather(
        city: String,
        latitude: Float,
        longitude: Float,
        fallbackTempC: Int,
        fallbackSummary: String
    ): WeatherSnapshot = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val urlString =
                "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current_weather=true"
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode == 200) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val current = json.getJSONObject("current_weather")
                val temp = current.getDouble("temperature").roundToInt()
                val code = current.optInt("weathercode", 0)
                val condition = mapWmoCodeToLabel(code)
                return@withContext WeatherSnapshot(
                    city = city,
                    temperatureC = temp,
                    conditionText = condition,
                    isLiveNetworkResult = true
                )
            }
        } catch (_: Exception) {
            // Gracefully fall back to cached weather when offline
        } finally {
            connection?.disconnect()
        }
        WeatherSnapshot(
            city = city,
            temperatureC = fallbackTempC,
            conditionText = fallbackSummary,
            isLiveNetworkResult = false
        )
    }

    fun mapWmoCodeToLabel(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1, 2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Atmospheric Fog"
        51, 53, 55 -> "Light Drizzle"
        61, 63, 65 -> "Rain Showers"
        71, 73, 75 -> "Snowfall"
        80, 81, 82 -> "Passing Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Fair Conditions"
    }
}
