package com.example.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimeOfDay
import com.example.data.model.WallpaperArtStyle
import com.example.data.model.WeatherCondition
import com.example.ui.components.CitySearchBottomSheet
import com.example.ui.components.PhoneMockupOverlay
import com.example.ui.components.SetWallpaperBottomSheet
import com.example.ui.components.WeatherThemeStudioSheet
import com.example.ui.components.WeatherWallpaperCanvas
import com.google.android.gms.location.LocationServices

@Composable
fun WeatherWallpaperScreen(
    viewModel: WeatherWallpaperViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedControlTab by remember { mutableStateOf(0) } // 0: Themes, 1: Weather Lab, 2: Time of Day
    var showControls by remember { mutableStateOf(true) }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.syncWeatherWithLocation()
        } else {
            viewModel.loadSavedWeather()
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    val activeCondition = if (uiState.isSimulationMode) {
        uiState.simulatedCondition
    } else {
        uiState.weather.condition
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Live Animated Wallpaper Canvas
            WeatherWallpaperCanvas(
                condition = activeCondition,
                artStyle = uiState.artStyle,
                timeOfDay = uiState.timeOfDay,
                temperatureC = uiState.weather.temperatureC,
                windSpeedKmh = uiState.weather.windSpeedKmh,
                isDay = uiState.weather.isDay,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showControls = !showControls }
            )

            // 2. Realistic Phone Mockup Overlay (Home screen icons & clock)
            if (uiState.showPhoneMockup) {
                PhoneMockupOverlay(
                    weather = uiState.weather,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 3. Top Floating Glass Bar
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Location Pill Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.showCitySearch(true) }
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("location_pill_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isSimulationMode) "Simulation Mode" else uiState.weather.cityName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (uiState.isLoadingWeather) {
                            Spacer(modifier = Modifier.width(6.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        }
                    }

                    // Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Phone Mockup Preview Toggle
                        IconButton(
                            onClick = { viewModel.togglePhoneMockup() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("toggle_mockup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = "Toggle Home Screen Mockup",
                                tint = if (uiState.showPhoneMockup) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Weather Theme Studio
                        IconButton(
                            onClick = { viewModel.showThemeStudio(true) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("open_theme_studio_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Open Weather Theme Studio",
                                tint = Color(uiState.artStyle.accentColorHex),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Refresh Weather
                        IconButton(
                            onClick = { viewModel.loadSavedWeather() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("refresh_weather_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Weather",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 4. Bottom Controls & Apply Floating Card
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.88f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                        .padding(16.dp)
                ) {
                    // Quick Weather Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeCondition.icon,
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = activeCondition.label,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${uiState.weather.temperatureC.toInt()}°C • Feels like ${uiState.weather.apparentTemperatureC.toInt()}°C",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WeatherMiniChip(
                                icon = Icons.Default.WaterDrop,
                                value = "${uiState.weather.humidity}%"
                            )
                            WeatherMiniChip(
                                icon = Icons.Default.Air,
                                value = "${uiState.weather.windSpeedKmh.toInt()} km/h"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Selection Tabs (Art Styles / Weather Lab / Time of Day)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ControlTabButton(
                            title = "Art Style",
                            icon = Icons.Default.Palette,
                            isSelected = selectedControlTab == 0,
                            testTag = "tab_art_style",
                            onClick = { selectedControlTab = 0 }
                        )
                        ControlTabButton(
                            title = "Weather Lab",
                            icon = Icons.Default.Tune,
                            isSelected = selectedControlTab == 1,
                            testTag = "tab_weather_lab",
                            onClick = { selectedControlTab = 1 }
                        )
                        ControlTabButton(
                            title = "Time of Day",
                            icon = Icons.Default.WbSunny,
                            isSelected = selectedControlTab == 2,
                            testTag = "tab_time_of_day",
                            onClick = { selectedControlTab = 2 }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Content
                    when (selectedControlTab) {
                        0 -> {
                            // Art Style Carousel & Theme Studio Shortcut
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Current: ${uiState.artStyle.title}",
                                        color = Color(uiState.artStyle.accentColorHex),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.showThemeStudio(true) }
                                            .background(Color(uiState.artStyle.accentColorHex).copy(alpha = 0.2f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .testTag("open_theme_studio_chip_button"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = Color(uiState.artStyle.accentColorHex),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Theme Studio",
                                            color = Color(uiState.artStyle.accentColorHex),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    WallpaperArtStyle.values().forEach { style ->
                                        val isSelected = uiState.artStyle == style
                                        Surface(
                                            onClick = { viewModel.setArtStyle(style) },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) Color(style.accentColorHex).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                                            border = if (isSelected) borderBorder(Color(style.accentColorHex)) else null,
                                            modifier = Modifier.testTag("style_chip_${style.name.lowercase()}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    style.paletteColors.forEach { cHex ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(cHex))
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = style.title,
                                                        color = if (isSelected) Color(style.accentColorHex) else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = style.subtitle.take(28) + "...",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Weather Lab / Simulation selector
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (uiState.isSimulationMode) "Simulation Active" else "Tracking Real Live Weather",
                                        color = if (uiState.isSimulationMode) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (uiState.isSimulationMode) "Reset to Live" else "Override Condition",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .clickable { viewModel.setSimulationMode(!uiState.isSimulationMode) }
                                            .testTag("toggle_simulation_mode")
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    WeatherCondition.values().forEach { cond ->
                                        val isSelected = uiState.isSimulationMode && uiState.simulatedCondition == cond
                                        Surface(
                                            onClick = {
                                                viewModel.setSimulationMode(true)
                                                viewModel.setSimulatedCondition(cond)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                                            border = if (isSelected) borderBorder(Color(0xFF38BDF8)) else null,
                                            modifier = Modifier.testTag("sim_condition_${cond.name.lowercase()}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = cond.icon, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = cond.label,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Time of day overrides
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TimeOfDay.values().forEach { time ->
                                    val isSelected = uiState.timeOfDay == time
                                    Surface(
                                        onClick = { viewModel.setTimeOfDay(time) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFFA855F7).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                                        border = if (isSelected) borderBorder(Color(0xFFA855F7)) else null,
                                        modifier = Modifier.testTag("time_chip_${time.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = time.label,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Apply Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.showSetWallpaper(true) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF38BDF8)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("set_wallpaper_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Set as Wallpaper",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        FilledTonalButton(
                            onClick = { viewModel.launchLiveWallpaper(context) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("launch_live_wallpaper_button")
                        ) {
                            Text(
                                text = "Live Wallpaper",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Bottom Sheets
            if (uiState.showCitySearchSheet) {
                CitySearchBottomSheet(
                    presetCities = viewModel.presetCities,
                    searchResults = uiState.searchResults,
                    isSearching = uiState.isSearchingCities,
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onCitySelected = { viewModel.onCitySelected(it) },
                    onUseCurrentLocation = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onDismiss = { viewModel.showCitySearch(false) }
                )
            }

            if (uiState.showSetWallpaperSheet) {
                SetWallpaperBottomSheet(
                    isApplying = uiState.isApplyingWallpaper,
                    onApplyStatic = { destination ->
                        viewModel.applyWallpaper(context, destination)
                    },
                    onLaunchLiveWallpaper = {
                        viewModel.launchLiveWallpaper(context)
                    },
                    onDismiss = { viewModel.showSetWallpaper(false) }
                )
            }

            if (uiState.showThemeStudioSheet) {
                WeatherThemeStudioSheet(
                    activeStyle = uiState.artStyle,
                    activeTimeOfDay = uiState.timeOfDay,
                    activeCondition = activeCondition,
                    weather = uiState.weather,
                    onStyleSelected = { viewModel.setArtStyle(it) },
                    onTimeOfDaySelected = { viewModel.setTimeOfDay(it) },
                    onDismiss = { viewModel.showThemeStudio(false) }
                )
            }
        }
    }
}

@Composable
private fun WeatherMiniChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ControlTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.25f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun borderBorder(color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.5.dp, color)
}
