package com.example.data.repository

import com.example.data.api.NetworkModule
import com.example.data.model.CurrentWeatherData
import com.example.data.model.GeocodingResult
import com.example.data.model.WeatherCondition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository {
    private val weatherApi = NetworkModule.weatherApi
    private val geocodingApi = NetworkModule.geocodingApi

    val presetCities = listOf(
        GeocodingResult(1, "Tokyo", 35.6762, 139.6503, "Japan", "Tokyo"),
        GeocodingResult(2, "New York", 40.7128, -74.0060, "United States", "New York"),
        GeocodingResult(3, "London", 51.5074, -0.1278, "United Kingdom", "England"),
        GeocodingResult(4, "Paris", 48.8566, 2.3522, "France", "Île-de-France"),
        GeocodingResult(5, "Reykjavik", 64.1466, -21.9426, "Iceland", "Capital Region"),
        GeocodingResult(6, "Sydney", -33.8688, 151.2093, "Australia", "New South Wales"),
        GeocodingResult(7, "San Francisco", 37.7749, -122.4194, "United States", "California"),
        GeocodingResult(8, "Honolulu", 21.3069, -157.8583, "United States", "Hawaii"),
        GeocodingResult(9, "Cairo", 30.0444, 31.2357, "Egypt", "Cairo Governorate")
    )

    suspend fun fetchWeather(
        cityName: String,
        country: String,
        latitude: Double,
        longitude: Double
    ): Result<CurrentWeatherData> = withContext(Dispatchers.IO) {
        try {
            val response = weatherApi.getForecast(latitude = latitude, longitude = longitude)
            val current = response.current

            val temp = current?.temperature ?: 20.0
            val apparent = current?.apparentTemperature ?: temp
            val humidity = current?.humidity ?: 50
            val wind = current?.windSpeed ?: 10.0
            val code = current?.weatherCode ?: 0
            val isDay = (current?.isDay ?: 1) == 1
            val condition = WeatherCondition.fromWmoCode(code)

            Result.success(
                CurrentWeatherData(
                    cityName = cityName,
                    country = country,
                    latitude = latitude,
                    longitude = longitude,
                    temperatureC = temp,
                    apparentTemperatureC = apparent,
                    humidity = humidity,
                    windSpeedKmh = wind,
                    condition = condition,
                    isDay = isDay,
                    wmoCode = code,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            // Provide resilient fallback data
            Result.failure(e)
        }
    }

    suspend fun searchCities(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        try {
            val response = geocodingApi.searchCity(name = query.trim())
            response.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
