package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CurrentWeatherData
import com.example.data.model.TimeOfDay
import com.example.data.model.WallpaperArtStyle
import com.example.data.model.WeatherCondition
import com.example.data.preferences.WallpaperPreferences
import com.example.data.repository.LocationRepository
import com.example.service.WeatherLiveWallpaperService
import com.example.service.WeatherSyncService
import com.example.util.WallpaperHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WeatherPaper", appName)
    }

    @Test
    fun `weather code maps to correct conditions`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromWmoCode(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCondition.fromWmoCode(1))
        assertEquals(WeatherCondition.OVERCAST, WeatherCondition.fromWmoCode(3))
        assertEquals(WeatherCondition.FOGGY, WeatherCondition.fromWmoCode(45))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWmoCode(61))
        assertEquals(WeatherCondition.SNOW, WeatherCondition.fromWmoCode(71))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromWmoCode(95))
    }

    @Test
    fun `renderToBitmap creates valid bitmap`() {
        val bitmap = WallpaperHelper.renderToBitmap(
            width = 300,
            height = 600,
            condition = WeatherCondition.CLEAR,
            artStyle = WallpaperArtStyle.MINIMAL_LANDSCAPE,
            timeOfDay = TimeOfDay.DAY,
            isDay = true,
            temperatureC = 22.0,
            windSpeedKmh = 10.0
        )
        assertNotNull(bitmap)
        assertEquals(300, bitmap.width)
        assertEquals(600, bitmap.height)
    }

    @Test
    fun `preferences save and restore weather data`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = WallpaperPreferences(context)
        val testWeather = CurrentWeatherData(
            cityName = "Paris",
            country = "France",
            latitude = 48.8566,
            longitude = 2.3522,
            temperatureC = 18.5,
            apparentTemperatureC = 17.0,
            humidity = 65,
            windSpeedKmh = 15.0,
            condition = WeatherCondition.RAIN,
            isDay = false,
            wmoCode = 61,
            timestamp = 123456789L
        )
        prefs.saveLastWeather(testWeather)

        val restored = prefs.loadLastWeather()
        assertEquals("Paris", restored.cityName)
        assertEquals(WeatherCondition.RAIN, restored.condition)
        assertEquals(18.5, restored.temperatureC, 0.1)
        assertEquals(false, restored.isDay)
    }

    @Test
    fun `location repository checks permissions correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val locationRepo = LocationRepository(context)
        // In default test environment without granted permissions, hasLocationPermission is false
        assertFalse(locationRepo.hasLocationPermission())
    }

    @Test
    fun `weather sync service creates and binds successfully`() {
        val serviceController = Robolectric.buildService(WeatherSyncService::class.java).create()
        val service = serviceController.get()
        assertNotNull(service)

        val binder = service.onBind(Intent())
        assertNotNull(binder)
        serviceController.destroy()
    }

    @Test
    fun `wallpaper service creates engine successfully`() {
        val serviceController = Robolectric.buildService(WeatherLiveWallpaperService::class.java).create()
        val service = serviceController.get()
        assertNotNull(service)
        val engine = service.onCreateEngine()
        assertNotNull(engine)
        serviceController.destroy()
    }

    @Test
    fun `wallpaper art styles contain valid palettes and tags`() {
        WallpaperArtStyle.values().forEach { style ->
            assertNotNull(style.title)
            assertNotNull(style.subtitle)
            assertNotNull(style.mood)
            assertEquals(3, style.paletteColors.size)
            assertEquals(3, style.tags.size)
        }
    }
}
