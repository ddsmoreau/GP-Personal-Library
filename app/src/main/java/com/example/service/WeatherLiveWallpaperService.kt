package com.example.service

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.example.data.model.CurrentWeatherData
import com.example.data.model.TimeOfDay
import com.example.data.model.WallpaperArtStyle
import com.example.data.model.WeatherCondition
import com.example.data.preferences.WallpaperPreferences
import com.example.data.repository.LocationRepository
import com.example.data.repository.WeatherRepository
import com.example.util.WallpaperHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android WallpaperService implementation that renders dynamic, custom weather backgrounds
 * based on real-time weather data, time of day, and user-selected art styles.
 */
class WeatherLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return WeatherWallpaperEngine()
    }

    private inner class WeatherWallpaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val handler = Handler(Looper.getMainLooper())
        private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private val repository = WeatherRepository()

        private lateinit var preferences: WallpaperPreferences
        private lateinit var locationRepository: LocationRepository
        private var currentWeather: CurrentWeatherData = CurrentWeatherData()

        private var isEngineVisible = false
        private var surfaceWidth = 1080
        private var surfaceHeight = 2400
        private val startTime = System.currentTimeMillis()
        private var lastWeatherFetchTime = 0L

        // Interactive touch & parallax effects
        private var parallaxOffsetX = 0f
        private var touchX = -1f
        private var touchY = -1f
        private var touchStartTime = 0L
        private val touchDurationMs = 800L

        private val drawRunnable = object : Runnable {
            override fun run() {
                if (isEngineVisible) {
                    drawFrame()
                    handler.postDelayed(this, 33) // ~30 FPS smooth rendering
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            preferences = WallpaperPreferences(applicationContext)
            locationRepository = LocationRepository(applicationContext)
            currentWeather = preferences.loadLastWeather()
            preferences.registerListener(this)
            setTouchEventsEnabled(true)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.isEngineVisible = visible
            if (visible) {
                // Refresh weather from local cache first
                currentWeather = preferences.loadLastWeather()
                // Check if background weather needs a network refresh (> 30 mins)
                checkAndFetchWeather(force = false)
                handler.removeCallbacks(drawRunnable)
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            drawFrame()
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            // Subtle horizontal parallax shifting as user swipes between home screen pages
            parallaxOffsetX = (xOffset - 0.5f) * 60f
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                touchX = event.x
                touchY = event.y
                touchStartTime = System.currentTimeMillis()
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            // Immediately reflect preference changes made in the app
            currentWeather = preferences.loadLastWeather()
            if (isEngineVisible) {
                drawFrame()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isEngineVisible = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onDestroy() {
            super.onDestroy()
            isEngineVisible = false
            handler.removeCallbacks(drawRunnable)
            preferences.unregisterListener(this)
            serviceScope.cancel()
        }

        private fun checkAndFetchWeather(force: Boolean) {
            val now = System.currentTimeMillis()
            val thirtyMinutesMs = 30 * 60 * 1000L
            if (force || (now - lastWeatherFetchTime > thirtyMinutesMs)) {
                serviceScope.launch {
                    val userLocation = if (locationRepository.hasLocationPermission()) {
                        locationRepository.getCurrentLocation().getOrNull()
                    } else null

                    val cityName = userLocation?.cityName ?: preferences.getSavedCityName()
                    val country = userLocation?.country ?: preferences.getSavedCountry()
                    val lat = userLocation?.latitude ?: preferences.getSavedLatitude()
                    val lon = userLocation?.longitude ?: preferences.getSavedLongitude()

                    val result = repository.fetchWeather(cityName, country, lat, lon)
                    result.onSuccess { freshData ->
                        currentWeather = freshData
                        preferences.saveCity(cityName, country, lat, lon)
                        preferences.saveLastWeather(freshData)
                        lastWeatherFetchTime = System.currentTimeMillis()
                    }
                }
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var canvas: android.graphics.Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val timeMs = System.currentTimeMillis() - startTime
                    val isSim = preferences.simulationMode.value
                    val condition = if (isSim) {
                        preferences.simulatedCondition.value
                    } else {
                        currentWeather.condition
                    }
                    val artStyle = preferences.artStyle.value
                    val timeOfDay = preferences.timeOfDay.value
                    val isDay = if (timeOfDay == TimeOfDay.AUTO) currentWeather.isDay else (timeOfDay != TimeOfDay.NIGHT)
                    val temperature = currentWeather.temperatureC
                    val windSpeed = currentWeather.windSpeedKmh

                    val touchElapsed = System.currentTimeMillis() - touchStartTime
                    val touchProgress = if (touchX >= 0 && touchElapsed < touchDurationMs) {
                        touchElapsed.toFloat() / touchDurationMs
                    } else {
                        2f // inactive
                    }

                    WallpaperHelper.drawSceneOnCanvas(
                        canvas = canvas,
                        width = surfaceWidth.toFloat(),
                        height = surfaceHeight.toFloat(),
                        condition = condition,
                        artStyle = artStyle,
                        timeOfDay = timeOfDay,
                        isDay = isDay,
                        windSpeedKmh = windSpeed,
                        timeMs = timeMs,
                        parallaxOffset = parallaxOffsetX,
                        touchRippleX = touchX,
                        touchRippleY = touchY,
                        touchProgress = touchProgress
                    )
                }
            } catch (e: Exception) {
                // Ignore transient draw exceptions during surface reconfiguration
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // Surface was already released
                    }
                }
            }
        }
    }
}
