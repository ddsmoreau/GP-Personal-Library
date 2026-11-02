package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CurrentWeatherData
import com.example.data.model.GeocodingResult
import com.example.data.model.TimeOfDay
import com.example.data.model.WallpaperArtStyle
import com.example.data.model.WeatherCondition
import com.example.data.preferences.WallpaperPreferences
import com.example.data.repository.LocationRepository
import com.example.data.repository.WeatherRepository
import com.example.util.WallpaperDestination
import com.example.util.WallpaperHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherWallpaperUiState(
    val weather: CurrentWeatherData = CurrentWeatherData(),
    val isLoadingWeather: Boolean = false,
    val errorMessage: String? = null,
    val artStyle: WallpaperArtStyle = WallpaperArtStyle.MINIMAL_LANDSCAPE,
    val timeOfDay: TimeOfDay = TimeOfDay.AUTO,
    val isSimulationMode: Boolean = false,
    val simulatedCondition: WeatherCondition = WeatherCondition.CLEAR,
    val showPhoneMockup: Boolean = true,
    val showCitySearchSheet: Boolean = false,
    val showSetWallpaperSheet: Boolean = false,
    val showThemeStudioSheet: Boolean = false,
    val isApplyingWallpaper: Boolean = false,
    val toastMessage: String? = null,
    val searchResults: List<GeocodingResult> = emptyList(),
    val isSearchingCities: Boolean = false
)

class WeatherWallpaperViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository()
    private val locationRepository = LocationRepository(application)
    private val preferences = WallpaperPreferences(application)

    private val _uiState = MutableStateFlow(
        WeatherWallpaperUiState(
            artStyle = preferences.artStyle.value,
            timeOfDay = preferences.timeOfDay.value,
            isSimulationMode = preferences.simulationMode.value,
            simulatedCondition = preferences.simulatedCondition.value
        )
    )
    val uiState: StateFlow<WeatherWallpaperUiState> = _uiState.asStateFlow()

    val presetCities = repository.presetCities
    private var searchJob: Job? = null

    init {
        loadSavedWeather()
    }

    fun loadSavedWeather() {
        val cached = preferences.loadLastWeather()
        _uiState.update { it.copy(weather = cached) }
        val cityName = preferences.getSavedCityName()
        val country = preferences.getSavedCountry()
        val lat = preferences.getSavedLatitude()
        val lon = preferences.getSavedLongitude()
        fetchWeather(cityName, country, lat, lon)
    }

    fun fetchWeather(cityName: String, country: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeather = true, errorMessage = null) }
            val result = repository.fetchWeather(cityName, country, latitude, longitude)
            result.onSuccess { weatherData ->
                _uiState.update {
                    it.copy(
                        weather = weatherData,
                        isLoadingWeather = false
                    )
                }
                preferences.saveCity(cityName, country, latitude, longitude)
                preferences.saveLastWeather(weatherData)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingWeather = false,
                        errorMessage = "Offline or weather service unavailable. Using cached weather."
                    )
                }
            }
        }
    }

    fun onCitySelected(city: GeocodingResult) {
        val country = city.country ?: ""
        fetchWeather(city.name, country, city.latitude, city.longitude)
    }

    fun onLocationReceived(latitude: Double, longitude: Double) {
        fetchWeather("Current Location", "", latitude, longitude)
    }

    fun syncWeatherWithLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeather = true, errorMessage = null) }
            val locationResult = locationRepository.getCurrentLocation()
            locationResult.fold(
                onSuccess = { loc ->
                    val city = loc.cityName ?: "Current Location"
                    val country = loc.country ?: ""
                    fetchWeather(city, country, loc.latitude, loc.longitude)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingWeather = false,
                            toastMessage = "Could not fetch GPS location: ${err.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearchingCities = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingCities = true) }
            delay(350) // debounce
            val results = repository.searchCities(query)
            _uiState.update { it.copy(searchResults = results, isSearchingCities = false) }
        }
    }

    fun setArtStyle(style: WallpaperArtStyle) {
        preferences.setArtStyle(style)
        _uiState.update { it.copy(artStyle = style) }
    }

    fun setTimeOfDay(time: TimeOfDay) {
        preferences.setTimeOfDay(time)
        _uiState.update { it.copy(timeOfDay = time) }
    }

    fun setSimulationMode(enabled: Boolean) {
        preferences.setSimulationMode(enabled)
        _uiState.update { it.copy(isSimulationMode = enabled) }
    }

    fun setSimulatedCondition(condition: WeatherCondition) {
        preferences.setSimulatedCondition(condition)
        _uiState.update { it.copy(simulatedCondition = condition) }
    }

    fun togglePhoneMockup() {
        _uiState.update { it.copy(showPhoneMockup = !it.showPhoneMockup) }
    }

    fun showCitySearch(show: Boolean) {
        _uiState.update { it.copy(showCitySearchSheet = show) }
    }

    fun showSetWallpaper(show: Boolean) {
        _uiState.update { it.copy(showSetWallpaperSheet = show) }
    }

    fun showThemeStudio(show: Boolean) {
        _uiState.update { it.copy(showThemeStudioSheet = show) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun applyWallpaper(context: Context, destination: WallpaperDestination) {
        val currentState = _uiState.value
        val effectiveCondition = if (currentState.isSimulationMode) {
            currentState.simulatedCondition
        } else {
            currentState.weather.condition
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingWallpaper = true) }
            val result = WallpaperHelper.applyWallpaper(
                context = context,
                condition = effectiveCondition,
                artStyle = currentState.artStyle,
                timeOfDay = currentState.timeOfDay,
                isDay = currentState.weather.isDay,
                temperatureC = currentState.weather.temperatureC,
                windSpeedKmh = currentState.weather.windSpeedKmh,
                destination = destination
            )
            result.onSuccess { message ->
                _uiState.update {
                    it.copy(
                        isApplyingWallpaper = false,
                        showSetWallpaperSheet = false,
                        toastMessage = message
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isApplyingWallpaper = false,
                        toastMessage = "Could not set wallpaper: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun launchLiveWallpaper(context: Context) {
        val launched = WallpaperHelper.openLiveWallpaperChooser(context)
        _uiState.update {
            it.copy(
                showSetWallpaperSheet = false,
                toastMessage = if (launched) "Select WeatherPaper in Live Wallpaper preview!" else "Live Wallpaper chooser not available on this device"
            )
        }
    }
}
