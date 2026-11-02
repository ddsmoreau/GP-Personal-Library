package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.data.model.CurrentWeatherData
import com.example.data.model.UserLocation
import com.example.data.preferences.WallpaperPreferences
import com.example.data.repository.LocationRepository
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class WeatherSyncStatus {
    object Idle : WeatherSyncStatus()
    object AcquiringLocation : WeatherSyncStatus()
    data class FetchingWeather(val location: UserLocation) : WeatherSyncStatus()
    data class Success(val weather: CurrentWeatherData) : WeatherSyncStatus()
    data class Error(val message: String) : WeatherSyncStatus()
}

/**
 * Service that fetches real-time weather data using coordinates from LocationRepository
 * to supply dynamic input for the wallpaper engine.
 */
class WeatherSyncService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var locationRepository: LocationRepository
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var preferences: WallpaperPreferences

    private val _syncStatus = MutableStateFlow<WeatherSyncStatus>(WeatherSyncStatus.Idle)
    val syncStatus: StateFlow<WeatherSyncStatus> = _syncStatus.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): WeatherSyncService = this@WeatherSyncService
    }

    override fun onCreate() {
        super.onCreate()
        locationRepository = LocationRepository(applicationContext)
        weatherRepository = WeatherRepository()
        preferences = WallpaperPreferences(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SYNC_WEATHER) {
            serviceScope.launch {
                syncWeather()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Obtains GPS coordinates from LocationRepository, fetches live weather data
     * from Open-Meteo, and stores the updated weather state in WallpaperPreferences
     * to power both static wallpaper generation and the live wallpaper engine.
     */
    suspend fun syncWeather(preferGps: Boolean = true): Result<CurrentWeatherData> = withContext(Dispatchers.IO) {
        var userLocation: UserLocation? = null

        if (preferGps && locationRepository.hasLocationPermission()) {
            _syncStatus.value = WeatherSyncStatus.AcquiringLocation
            val locationResult = locationRepository.getCurrentLocation()
            userLocation = locationResult.getOrNull()
        }

        // Fall back to saved coordinates if GPS is unavailable or permission is not yet granted
        val effectiveLocation = userLocation ?: UserLocation(
            latitude = preferences.getSavedLatitude(),
            longitude = preferences.getSavedLongitude(),
            cityName = preferences.getSavedCityName(),
            country = preferences.getSavedCountry(),
            isFromGps = false
        )

        _syncStatus.value = WeatherSyncStatus.FetchingWeather(effectiveLocation)

        val cityName = effectiveLocation.cityName ?: preferences.getSavedCityName()
        val country = effectiveLocation.country ?: preferences.getSavedCountry()

        val weatherResult = weatherRepository.fetchWeather(
            cityName = cityName,
            country = country,
            latitude = effectiveLocation.latitude,
            longitude = effectiveLocation.longitude
        )

        weatherResult.fold(
            onSuccess = { weatherData ->
                preferences.saveCity(
                    name = weatherData.cityName,
                    country = weatherData.country,
                    lat = weatherData.latitude,
                    lon = weatherData.longitude
                )
                preferences.saveLastWeather(weatherData)
                _syncStatus.value = WeatherSyncStatus.Success(weatherData)
                Result.success(weatherData)
            },
            onFailure = { error ->
                val errorMsg = error.localizedMessage ?: "Failed to fetch weather data"
                _syncStatus.value = WeatherSyncStatus.Error(errorMsg)
                Result.failure(error)
            }
        )
    }

    companion object {
        const val ACTION_SYNC_WEATHER = "com.example.service.ACTION_SYNC_WEATHER"

        fun startSync(context: Context) {
            val intent = Intent(context, WeatherSyncService::class.java).apply {
                action = ACTION_SYNC_WEATHER
            }
            context.startService(intent)
        }
    }
}
