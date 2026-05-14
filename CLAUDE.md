# Lyn

Android app for måling av avstand til tordenvær via flash-til-torden-timer, med live lynkart og stormtrend-sporing.

**Stack:** Kotlin · Jetpack Compose · Material 3 · Room · OkHttp WebSocket · osmdroid
**Package:** `no.lyn.app` · minSdk 26 · targetSdk 35 · JVM 11

---

## Quick Start

```bash
./gradlew assembleDebug        # bygg APK
./gradlew test                 # unit-tester (kjører uten enhet)
./gradlew installDebug         # installer på tilkoblet enhet/emulator
./gradlew connectedAndroidTest # instrumenterte tester (krever enhet)
```

**Ny worktree?** `local.properties` er gitignored. Opprett den med `sdk.dir=<sti til Android-SDK>` (på denne maskinen: `/Users/morteneikli/Library/Android/sdk`) før første build.

---

## Prosjektstruktur

```
app/src/main/java/no/lyn/app/
  MainActivity.kt          Nav-host, tre skjermer: Timer / History / Map
  LightningData.kt         Pure functions — secondsToKm, getStormTrend, getSafetyInfo, factForMeasurementCount
  HistoryGrouping.kt       Pure functions — dag-gruppering med eksplisitt TimeZone
  LynApplication.kt        Application-klasse, Room-singleton, osmdroid-init
  LynWidget.kt             Hjemskjerm-widget (SharedPreferences, ikke Room)
  NotificationHelper.kt    Varslingskanal + lokal push-varsling
  data/                    AppDatabase, MeasurementDao, Measurement, BlitzortungService
  ui/                      TimerScreen, HistoryScreen, MapScreen, MapViewModel, Components
res/
  values/strings.xml       Engelsk (default)
  values-nb/strings.xml    Norsk bokmål
  mipmap-*/                Launcher-ikoner (adaptive XML + PNG-fallbacks)
```

---

## Nøkkelkonvensjoner

- **Avstandsformel:** `seconds / 2.915` km (343 m/s lydhastig.). Endre bare i `LightningData.kt` + kommentar der.
- **Sikkerhetsterskler:** `<3 km` EXTREME · `<6 km` DANGER · `<10 km` CAUTION · ellers LOW_RISK. Definisjoner i `getSafetyInfo()` — ikke dupliser disse tallene andre steder.
- **Avhengigheter:** legg til i `gradle/libs.versions.toml` → referer via `libs.xxx` i `build.gradle.kts`. Aldri råkoordinater direkte.
- **Strenger:** ny brukersynlig tekst går i *begge* `values/strings.xml` og `values-nb/strings.xml`.
- **Pure functions:** logikk uten Android-avhengighet hører hjemme i `LightningData.kt` (fysikk/sikkerhet/fakta) eller en egen pure-funksjons-fil (f.eks. `HistoryGrouping.kt`) — da kan den enhetstestes uten emulator.
- **Tidssoner:** pure dato-/tidsfunksjoner skal ta `TimeZone` som parameter (default = `getDefault()`). Tester sender alltid eksplisitt sone for å bli deterministiske. Aldri stol på JVM-default i delt logikk.

---

## Kritiske gotchas

- **Widget bruker SharedPreferences, ikke Room.** Kall `LynWidget.onMeasurementSaved(context, km)` etter *enhver* lagret måling — ellers oppdateres ikke widgeten.
- **Varsler virker bare mens kartfanen er synlig.** Det finnes ingen bakgrunnstjeneste. `MapViewModel` lever kun mens `MapScreen` er i komposisjonen.
- **osmdroid krever deprecated API.** `LynApplication` bruker `@Suppress("DEPRECATION")` for `PreferenceManager` — osmdroid krever dette. Ikke fjern suppressionen.
- **`dp`-import er ikke med i wildcard-import.** Legg til `import androidx.compose.ui.unit.dp` eksplisitt i alle Composable-filer som bruker `dp`.
- **Blitzortung abonnerer globalt.** WebSocket-filteret i `BlitzortungService.onOpen()` sender `-180,180,-90,90`. Skal du filtrere regionalt, endre `send()`-kallet der.
- **Notifikasjons-tillatelse er runtime på API 33+.** `POST_NOTIFICATIONS` er i manifest men må også requestas i kode.

---

## Hva du ikke skal gjøre

- Ikke commit direkte til `main` — bruk feature-branch + PR (Pillar 5)
- Ikke hardkod brukersynlige tekster — bruk `strings.xml`
- Ikke legg databasekall i Composables — gå via ViewModel eller send DAO ned som parameter
- Ikke legg nye avhengigheter direkte i `build.gradle.kts` — start med `libs.versions.toml`
- Ikke dupliser sikkerhetsterskler fra `getSafetyInfo()` — de har én kilde

---

## Skills

**Start her:** `~/.claude/skills/common/sdd-navigator` — finn riktig skill for oppgaven

| Behov | Skill |
|-------|-------|
| Metodikk-oversikt | `sdd-context` |
| Commit / PR-flyt | `github-workflow` |
| Testing og verifisering | `verification-patterns` · `tdd-workflow` |
| Noe føles galt / hallusinasjon | `fear-driven-development` |
| Skrive første domain-skill | `sdd-domain-skill-extractor` |

Prosjekt-spesifikke skills: `.claude/skills/` (tom — legg til når mønstre gjentas)
