# WeatherPaper — Dynamic Weather-Adaptive Wallpaper Engine

A real-time Android wallpaper application that transforms the device's home screen, lock screen, and live wallpaper background into an atmospheric, weather-responsive art canvas that adapts dynamically to live weather conditions, GPS location, and the natural day/night cycle.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The architectural plan outlines the full product experience, background synchronization pipeline, and theme customization system.

- **Real-Time Weather Source**: Open-Meteo REST API is selected as the primary provider because it requires zero API keys, provides high-accuracy worldwide forecasts (temperature, humidity, wind, precipitation, WMO weather codes), and has zero rate-limiting obstacles for users.
- **Wallpaper Engine Architecture**: Both direct bitmap rendering (via Android `WallpaperManager` for static Home and Lock screens) and continuous background animation (via Android `WallpaperService` for native Live Wallpaper) are supported in parallel.
- **Location Strategy**: High-efficiency fused location with runtime permission checks and Android Geocoder reverse-lookup, paired with saved fallback coordinates to guarantee seamless zero-permission offline operation.
- **Theme Visual Design**: 5 curated artistic styles spanning minimalist vector landscapes, lush anime meadows, cyberpunk neon skylines, cinematic ocean horizons, and fluid organic auroras.

---

### 1. Overview & Core Concept

- **What It Does**: WeatherPaper continuously monitors the user's localized weather (or simulated conditions) and renders high-definition, animated atmospheric scenes directly onto the phone's wallpaper. Falling rain streaks splash on the glass, thunderstorms illuminate the sky with sudden lightning bolts, soft snowflakes drift with wind physics, and sun/moon positions reflect true diurnal progression.
- **Target Audience**: Android users who value personalization, aesthetic home screen setups, and ambient at-a-glance awareness of atmospheric conditions throughout their day.
- **Key Value**: Replaces static, disconnected wallpapers with an alive, expressive background that connects the digital device to the physical world outside.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **First-Launch & Ambient Experience**:
   - The user opens the app to an edge-to-edge animated wallpaper canvas immediately showing the current city's weather, time-of-day lighting, and animated weather particles.
   - A floating glass status pill displays current temperature, weather condition, humidity, and wind speed.
   - An interactive "Phone Mockup" overlay can be toggled on or off to preview exactly how the wallpaper looks behind app icons, search widgets, and the system clock.
2. **Location & City Selection**:
   - Tapping the location pill opens a modal sheet allowing one-tap "Use Current Device Location" (with Play Services GPS) or instant search across thousands of global cities with live geocoding.
3. **Weather Theme Studio & Customization**:
   - Tapping the Theme Studio button opens a full-screen drawer presenting interactive cards for each of the 5 art styles.
   - Each card features a live miniature animated canvas, palette swatches, design tags, and mood notes.
   - Users can scrub the Time-of-Day lighting (Dawn, Day, Dusk, Night) and test conditions in the Weather Lab.
4. **Setting Device Wallpaper**:
   - Tapping "Set as Wallpaper" presents options to apply high-resolution renders directly to Home Screen, Lock Screen, or Both.
   - Alternatively, users can launch the native Android Live Wallpaper chooser to enable continuous real-time animation on their device.

#### Visual Identity & Theme Tokens
- **Design Philosophy**: Material Design 3 with dynamic glassmorphism floating overlays, deep ambient obsidian backgrounds (`#0F172A`), and vibrant weather accents.
- **Palette Tokens**:
  - *Minimal Mountain*: Azure Blue (`#38BDF8`), Slate Grey (`#334155`), Obsidian (`#0F172A`).
  - *Anime Meadow*: Emerald Green (`#10B981`), Mint (`#6EE7B7`), Deep Forest (`#064E3B`).
  - *Cyberpunk City*: Neon Rose (`#F43F5E`), Cyan (`#06B6D4`), Deep Void (`#090D16`).
  - *Cinematic Horizon*: Sunset Orange (`#FB923C`), Sky Blue (`#0284C7`), Deep Ocean (`#0C4A6E`).
  - *Flowing Aurora*: Royal Violet (`#A855F7`), Neon Magenta (`#EC4899`), Midnight (`#4C1D95`).
- **Interactive Feedback & Motion**:
  - Home screen touch ripples expand under finger touches.
  - Parallax horizontal displacement reacts to multi-page home screen swiping.
  - Smooth 30–45 FPS rendering throttled automatically when the screen is hidden.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Zero-Key Open-Meteo API vs Commercial Weather APIs**
  - *Chosen Approach*: Open-Meteo REST API.
  - *Why*: Eliminates friction of API key configuration for users while offering comprehensive WMO condition codes, hourly metrics, and geocoding.
  - *Trade-Off*: Relies on public endpoints, handled gracefully with local fallback caching.
- **Decision 2: Dual Wallpaper Engine (Static Bitmap + Live Wallpaper Service)**
  - *Chosen Approach*: Support both `WallpaperManager.setBitmap` and `WeatherLiveWallpaperService`.
  - *Why*: Some users want zero battery consumption (static render updated on demand), while others desire full interactive motion (live wallpaper with touch ripples and parallax).
  - *Trade-Off*: Requires maintaining both canvas drawing pipelines in `WallpaperHelper`.
- **Decision 3: Offline-First Cached State**
  - *Chosen Approach*: Cache full weather snapshots and coordinates in `WallpaperPreferences`.
  - *Why*: The app and live wallpaper start immediately without displaying blank or loading placeholders even when device is offline or in airplane mode.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        WEATHERPAPER ARCHITECTURE                       │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   ┌────────────────────┐              ┌───────────────────────────┐    │
│   │ LocationRepository │              │     WeatherRepository     │    │
│   │ (FusedLocation/GPS)│              │  (Open-Meteo Forecast/    │    │
│   └─────────┬──────────┘              │     Geocoding API)        │    │
│             │                         └─────────────┬─────────────┘    │
│             ▼                                       ▼                  │
│   ┌───────────────────────────────────────────────────────────────┐    │
│   │                     WeatherSyncService                        │    │
│   │   - Coordinates GPS position & Open-Meteo live weather data   │    │
│   │   - Persists state into WallpaperPreferences                  │    │
│   └───────────────────────────────┬───────────────────────────────┘    │
│                                   │                                    │
│                     ┌─────────────┴─────────────┐                      │
│                     ▼                           ▼                      │
│       ┌──────────────────────────┐ ┌───────────────────────────┐      │
│       │WeatherWallpaperViewModel │ │WeatherLiveWallpaperService│      │
│       │ (UI State & User Events) │ │ (Native Android Wallpaper)│      │
│       └─────────────┬────────────┘ └─────────────┬─────────────┘      │
│                     │                            │                     │
│                     ▼                            ▼                     │
│       ┌──────────────────────────┐ ┌───────────────────────────┐      │
│       │ WeatherWallpaperScreen   │ │   WallpaperHelper Engine  │      │
│       │ - Interactive Canvas     │ │ - High-Res Bitmap Render  │      │
│       │ - Phone Mockup Overlay   │ │ - SurfaceHolder Canvas    │      │
│       │ - Weather Theme Studio   │ │ - Parallax & Touch Ripple │      │
│       │ - City Search & Lab      │ │ - System WallpaperManager │      │
│       └──────────────────────────┘ └───────────────────────────┘      │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Data Entities & State Models
- **`CurrentWeatherData`**: Temperature, apparent temperature, humidity, wind speed, WMO weather condition enum, day/night flag, city name, country, coordinates, and timestamp.
- **`WallpaperArtStyle`**: Enum with titles, subtitles, accent hex colors, color palette swatches, tags, and descriptive mood statements.
- **`TimeOfDay`**: Enum representing `AUTO`, `DAWN`, `DAY`, `DUSK`, and `NIGHT`.
- **`WeatherWallpaperUiState`**: Unified StateFlow emitted to Jetpack Compose containing active weather, selected style, time override, simulation mode flag, bottom sheet visibility toggles, and search results.

#### Interactive Component & State Mapping
- **Location Selector**: `CitySearchBottomSheet` maps search queries to `WeatherRepository.searchCities()` with 350ms debounce. Selecting a city triggers immediate forecast fetch and preference storage.
- **Theme Switcher**: `WeatherThemeStudioSheet` updates `WallpaperPreferences.artStyle`, broadcasting updates to both the preview canvas and the background live wallpaper service.
- **Wallpaper Setter**: `SetWallpaperBottomSheet` coordinates asynchronous background bitmap rendering and dispatches to `WallpaperManager` for Home Screen, Lock Screen, or Both.
