# Lyn

## What Is This

Android app for measuring distance to a thunderstorm by timing the gap between flash and thunder, with a live lightning map and storm-trend tracking.

**Type:** Android app
**Language:** Kotlin (Jetpack Compose UI)
**Build:** `./gradlew assembleDebug` · `./gradlew test`

---

## Quick Start

```bash
# Build (debug APK)
./gradlew assembleDebug

# Unit tests
./gradlew test

# Instrumented tests (device/emulator required)
./gradlew connectedAndroidTest

# Install on a connected device/emulator
./gradlew installDebug
```

App ID: `no.lyn.app` · minSdk 26 · targetSdk 35 · JVM target 11.

---

## Project Structure

```
Lyn/
  app/
    src/main/
      java/no/lyn/app/
        MainActivity.kt            Compose nav host, three screens: Timer / History / Map
        LynApplication.kt          Application + Room DB singleton
        LightningData.kt           Pure functions: secondsToKm, getStormTrend, getSafetyInfo
        LynWidget.kt               Home-screen widget
        NotificationHelper.kt      Channel + notifications
        data/
          AppDatabase.kt           Room database
          MeasurementDao.kt        DAO for stored measurements
          Measurement.kt           @Entity: id, timestamp, seconds, distanceKm, safetyLevel
          Converters.kt            Room type converters
          BlitzortungService.kt    OkHttp WebSocket client (live strike feed)
        ui/
          TimerScreen.kt           Tap to start/stop, shows distance + safety level
          HistoryScreen.kt         Past measurements (Room-backed)
          MapScreen.kt             osmdroid map with live strikes
          MapViewModel.kt          Map state + Blitzortung connection
          LightningStrikeOverlay.kt
          Components.kt            Shared composables
      res/
        values/strings.xml         English (default)
        values-nb/strings.xml      Norwegian (Bokmål)
        xml/widget_info.xml
        drawable/, layout/, etc.
  gradle/libs.versions.toml        Version catalog (single source for deps)
```

---

## Key Conventions

**Stack:**
- UI: Jetpack Compose + Material 3 (no XML layouts except widget)
- Navigation: navigation-compose with sealed `Screen` class in `MainActivity.kt`
- Persistence: Room with KSP code generation
- Network: OkHttp WebSocket against Blitzortung
- Map: osmdroid (OpenStreetMap tiles)

**Code style:**
- Code, comments, log strings: English
- User-facing strings: `strings.xml` (English) + `values-nb/strings.xml` (Norwegian). Never hard-code UI strings.
- Pure functions where possible — see `LightningData.kt` (`secondsToKm`, `getStormTrend`, `getSafetyInfo`)
- Data classes for models; `enum class` for closed sets (`SafetyLevel`, `StormTrend`)
- Dependencies referenced through the version catalog (`libs.xxx`), not raw coordinates

**Physics constants:**
- Speed of sound ≈ 343 m/s → `seconds / 2.915` km. If you change this, also update the documentation comment in `LightningData.kt`.

**Safety thresholds** (`getSafetyInfo`): `<3 km` extreme · `<6 km` danger · `<10 km` caution · else low risk. These map to UI colors and notification text.

---

## What NOT To Do

- Don't hard-code user-facing strings — add to both `values/strings.xml` and `values-nb/strings.xml`
- Don't access the database from composables — go through a ViewModel or pass the DAO down
- Don't add new dependencies inline in `app/build.gradle.kts` — add to `gradle/libs.versions.toml` first
- Don't bypass Compose for new screens (widget is the only XML-layout exception)
- Don't commit `local.properties` or anything under `/build`, `/.idea`, `.gradle` (already in `.gitignore`)

---

## Skills

Project-specific skills: `.claude/skills/` (empty — add as patterns emerge)

Methodology skills (global, `~/.claude/skills/common/`):
- Entry point: `sdd-context`
- CLAUDE.md guidance: `claude-md-guidelines`
- Skill routing: `skill-routing-decisions`

---

## Related

- Default branch: `claude/new-session-9Gup8` (unusual — consider renaming to `main` before sharing)
- Lightning data source: [Blitzortung.org](https://www.blitzortung.org/) community network (free, attribution required)
- Map tiles: OpenStreetMap via osmdroid
