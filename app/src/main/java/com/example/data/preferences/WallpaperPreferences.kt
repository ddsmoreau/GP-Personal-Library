package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CurrentWeatherData
import com.example.data.model.TimeOfDay
import com.example.data.model.WallpaperArtStyle
import com.example.data.model.WeatherCondition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WallpaperPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("weather_wallpaper_prefs", Context.MODE_PRIVATE)

    private val _artStyle = MutableStateFlow(loadArtStyle())
    val artStyle: StateFlow<WallpaperArtStyle> = _artStyle.asStateFlow()

    private val _timeOfDay = MutableStateFlow(loadTimeOfDay())
    val timeOfDay: StateFlow<TimeOfDay> = _timeOfDay.asStateFlow()

    private val _simulationMode = MutableStateFlow(loadSimulationMode())
    val simulationMode: StateFlow<Boolean> = _simulationMode.asStateFlow()

    private val _simulatedCondition = MutableStateFlow(loadSimulatedCondition())
    val simulatedCondition: StateFlow<WeatherCondition> = _simulatedCondition.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "art_style" -> _artStyle.value = loadArtStyle()
            "time_of_day" -> _timeOfDay.value = loadTimeOfDay()
            "simulation_mode" -> _simulationMode.value = loadSimulationMode()
            "sim_condition" -> _simulatedCondition.value = loadSimulatedCondition()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun loadArtStyle(): WallpaperArtStyle {
        val name = prefs.getString("art_style", WallpaperArtStyle.MINIMAL_LANDSCAPE.name)
        return try {
            WallpaperArtStyle.valueOf(name ?: WallpaperArtStyle.MINIMAL_LANDSCAPE.name)
        } catch (e: Exception) {
            WallpaperArtStyle.MINIMAL_LANDSCAPE
        }
    }

    fun setArtStyle(style: WallpaperArtStyle) {
        prefs.edit().putString("art_style", style.name).apply()
        _artStyle.value = style
    }

    private fun loadTimeOfDay(): TimeOfDay {
        val name = prefs.getString("time_of_day", TimeOfDay.AUTO.name)
        return try {
            TimeOfDay.valueOf(name ?: TimeOfDay.AUTO.name)
        } catch (e: Exception) {
            TimeOfDay.AUTO
        }
    }

    fun setTimeOfDay(time: TimeOfDay) {
        prefs.edit().putString("time_of_day", time.name).apply()
        _timeOfDay.value = time
    }

    private fun loadSimulationMode(): Boolean {
        return prefs.getBoolean("simulation_mode", false)
    }

    fun setSimulationMode(enabled: Boolean) {
        prefs.edit().putBoolean("simulation_mode", enabled).apply()
        _simulationMode.value = enabled
    }

    private fun loadSimulatedCondition(): WeatherCondition {
        val name = prefs.getString("sim_condition", WeatherCondition.CLEAR.name)
        return try {
            WeatherCondition.valueOf(name ?: WeatherCondition.CLEAR.name)
        } catch (e: Exception) {
            WeatherCondition.CLEAR
        }
    }

    fun setSimulatedCondition(condition: WeatherCondition) {
        prefs.edit().putString("sim_condition", condition.name).apply()
        _simulatedCondition.value = condition
    }

    fun getSavedCityName(): String = prefs.getString("city_name", "Tokyo") ?: "Tokyo"
    fun getSavedCountry(): String = prefs.getString("country_name", "Japan") ?: "Japan"
    fun getSavedLatitude(): Double = prefs.getFloat("city_lat", 35.6762f).toDouble()
    fun getSavedLongitude(): Double = prefs.getFloat("city_lon", 139.6503f).toDouble()

    fun saveCity(name: String, country: String, lat: Double, lon: Double) {
        prefs.edit()
            .putString("city_name", name)
            .putString("country_name", country)
            .putFloat("city_lat", lat.toFloat())
            .putFloat("city_lon", lon.toFloat())
            .apply()
    }

    fun saveLastWeather(weather: CurrentWeatherData) {
        prefs.edit()
            .putString("weather_city", weather.cityName)
            .putString("weather_country", weather.country)
            .putFloat("weather_lat", weather.latitude.toFloat())
            .putFloat("weather_lon", weather.longitude.toFloat())
            .putFloat("weather_temp", weather.temperatureC.toFloat())
            .putFloat("weather_apparent_temp", weather.apparentTemperatureC.toFloat())
            .putInt("weather_humidity", weather.humidity)
            .putFloat("weather_wind", weather.windSpeedKmh.toFloat())
            .putString("weather_condition", weather.condition.name)
            .putBoolean("weather_is_day", weather.isDay)
            .putInt("weather_wmo_code", weather.wmoCode)
            .putLong("weather_timestamp", weather.timestamp)
            .apply()
    }

    fun loadLastWeather(): CurrentWeatherData {
        val city = prefs.getString("weather_city", getSavedCityName()) ?: getSavedCityName()
        val country = prefs.getString("weather_country", getSavedCountry()) ?: getSavedCountry()
        val lat = prefs.getFloat("weather_lat", getSavedLatitude().toFloat()).toDouble()
        val lon = prefs.getFloat("weather_lon", getSavedLongitude().toFloat()).toDouble()
        val temp = prefs.getFloat("weather_temp", 22.0f).toDouble()
        val apparentTemp = prefs.getFloat("weather_apparent_temp", 21.0f).toDouble()
        val humidity = prefs.getInt("weather_humidity", 55)
        val wind = prefs.getFloat("weather_wind", 12.0f).toDouble()
        val condName = prefs.getString("weather_condition", WeatherCondition.CLEAR.name)
        val condition = try {
            WeatherCondition.valueOf(condName ?: WeatherCondition.CLEAR.name)
        } catch (e: Exception) {
            WeatherCondition.CLEAR
        }
        val isDay = prefs.getBoolean("weather_is_day", true)
        val wmoCode = prefs.getInt("weather_wmo_code", 0)
        val timestamp = prefs.getLong("weather_timestamp", 0L)

        return CurrentWeatherData(
            cityName = city,
            country = country,
            latitude = lat,
            longitude = lon,
            temperatureC = temp,
            apparentTemperatureC = apparentTemp,
            humidity = humidity,
            windSpeedKmh = wind,
            condition = condition,
            isDay = isDay,
            wmoCode = wmoCode,
            timestamp = timestamp
        )
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
