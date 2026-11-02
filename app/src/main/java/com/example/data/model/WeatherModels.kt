package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * High level conditions representing weather types for wallpaper rendering.
 */
enum class WeatherCondition(val label: String, val icon: String) {
    CLEAR("Clear Sky", "☀️"),
    PARTLY_CLOUDY("Partly Cloudy", "⛅"),
    OVERCAST("Overcast", "☁️"),
    FOGGY("Fog & Mist", "🌫️"),
    DRIZZLE("Light Drizzle", "🌦️"),
    RAIN("Rain", "🌧️"),
    THUNDERSTORM("Thunderstorm", "⛈️"),
    SNOW("Snow", "❄️"),
    WINDY("Windy", "💨");

    companion object {
        fun fromWmoCode(code: Int): WeatherCondition {
            return when (code) {
                0 -> CLEAR
                1, 2 -> PARTLY_CLOUDY
                3 -> OVERCAST
                45, 48 -> FOGGY
                51, 53, 55, 56, 57 -> DRIZZLE
                61, 63, 65, 80, 81, 82 -> RAIN
                71, 73, 75, 77, 85, 86 -> SNOW
                95, 96, 99 -> THUNDERSTORM
                else -> PARTLY_CLOUDY
            }
        }
    }
}

enum class TimeOfDay(val label: String) {
    AUTO("Real Time"),
    DAWN("Dawn / Sunrise"),
    DAY("Daytime"),
    DUSK("Dusk / Sunset"),
    NIGHT("Starry Night")
}

enum class WallpaperArtStyle(
    val title: String,
    val subtitle: String,
    val accentColorHex: Long,
    val paletteColors: List<Long>,
    val tags: List<String>,
    val mood: String
) {
    MINIMAL_LANDSCAPE(
        title = "Minimal Mountain",
        subtitle = "Layered geometric ridges with soft atmospheric gradients",
        accentColorHex = 0xFF38BDF8,
        paletteColors = listOf(0xFF0F172A, 0xFF334155, 0xFF38BDF8),
        tags = listOf("Geometric", "Ridges", "Atmospheric"),
        mood = "Calm, clean, and modern. Perfectly contrasts phone home screen icons."
    ),
    STUDIO_ANIME(
        title = "Anime Sky & Meadow",
        subtitle = "Lush green rolling hills, vivid anime sky and soft clouds",
        accentColorHex = 0xFF34D399,
        paletteColors = listOf(0xFF065F46, 0xFF10B981, 0xFF6EE7B7),
        tags = listOf("Ghibli Nature", "Rolling Hills", "Breeze"),
        mood = "Vibrant and nostalgic. Wind ripples brush through emerald meadows."
    ),
    CYBERPUNK_NEON(
        title = "Cyberpunk City",
        subtitle = "Futuristic skyline with glowing neon reflections and mist",
        accentColorHex = 0xFFF43F5E,
        paletteColors = listOf(0xFF090D16, 0xFFF43F5E, 0xFF06B6D4),
        tags = listOf("Neon Lights", "Rain-slicked", "Cyber Skyline"),
        mood = "Moody and electric. Shines during rainstorms with neon reflections."
    ),
    HORIZON_ATMOSPHERE(
        title = "Cinematic Horizon",
        subtitle = "Expansive ocean horizon reflecting celestial light",
        accentColorHex = 0xFFFB923C,
        paletteColors = listOf(0xFF0C4A6E, 0xFF0284C7, 0xFFFB923C),
        tags = listOf("Ocean", "Golden Hour", "Reflections"),
        mood = "Expansive and cinematic. Shimmering waves reflect sunrise and moonlight."
    ),
    DYNAMIC_ABSTRACT(
        title = "Flowing Aurora",
        subtitle = "Smooth organic waves that shift with wind and temperature",
        accentColorHex = 0xFFA855F7,
        paletteColors = listOf(0xFF4C1D95, 0xFFA855F7, 0xFFEC4899),
        tags = listOf("Fluid", "Bézier Waves", "Aurora Borealis"),
        mood = "Organic and hypnotic. Ribbons of light flow with wind velocity."
    )
}

data class CurrentWeatherData(
    val cityName: String = "Tokyo",
    val country: String = "Japan",
    val latitude: Double = 35.6762,
    val longitude: Double = 139.6503,
    val temperatureC: Double = 22.0,
    val apparentTemperatureC: Double = 21.0,
    val humidity: Int = 55,
    val windSpeedKmh: Double = 12.0,
    val condition: WeatherCondition = WeatherCondition.CLEAR,
    val isDay: Boolean = true,
    val wmoCode: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

// Retrofit Moshi Data Classes for Open-Meteo

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "current") val current: CurrentUnitsAndData?
)

@JsonClass(generateAdapter = true)
data class CurrentUnitsAndData(
    @Json(name = "temperature_2m") val temperature: Double?,
    @Json(name = "relative_humidity_2m") val humidity: Int?,
    @Json(name = "apparent_temperature") val apparentTemperature: Double?,
    @Json(name = "is_day") val isDay: Int?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "wind_speed_10m") val windSpeed: Double?
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    @Json(name = "results") val results: List<GeocodingResult>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    @Json(name = "id") val id: Long?,
    @Json(name = "name") val name: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "country") val country: String?,
    @Json(name = "admin1") val admin1: String?
)
