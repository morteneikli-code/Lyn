# Learnings — Lyn

**Formål:** Fang opp oppdagelser, feil og lærdommer mens de skjer. Dette er Pillar 6: Continuous Learning.

**Når du legger til en entry:**
- Claude tok feil → fiks det OG skriv det her
- Du oppdaget et mønster som overrasket deg
- Noe tok lengre tid enn forventet på grunn av en gotcha
- En skill ble oppdatert fordi den ga feil veiledning

---

## Entries

### 2026-05-12: Pillar 5-brudd — direkte commits til main

**Context:** Oppsett av prosjekt og SDD-rammeverk første dag
**Hva skjedde:** Alle commits gikk direkte til `main`, ikke via feature-branch og PR
**Rotårsak:** Pillar 5 (Process Discipline) ikke fulgt — "no direct commits to main, ever"
**Fix:** Pre-commit hook satt opp. Fra nå: feature-branch → PR → merge
**Skill oppdatert:** Ingen — `github-workflow`-skillens regler er allerede klare

---

### 2026-05-12: osmdroid krever deprecated PreferenceManager

**Context:** Kompilatorvarsel i LynApplication.kt ved første bygg
**Hva skjedde:** `@Suppress("DEPRECATION")` er nødvendig for osmdroid-konfigurasjon
**Rotårsak:** `osmdroid.Configuration.load()` krever `PreferenceManager.getDefaultSharedPreferences()` som er deprecated fra API 29. osmdroid har ikke oppdatert APIen.
**Fix:** Suppressionen er korrekt og skal beholdes — dokumentert i CLAUDE.md
**Skill oppdatert:** Lagt til som kritisk gotcha i CLAUDE.md

---

### 2026-05-12: Widget bruker SharedPreferences, ikke Room

**Context:** Analyse av widget-koden under feature-gjennomgang
**Hva skjedde:** Widgeten oppdateres ikke automatisk fra databasen
**Rotårsak:** `AppWidgetProvider` har ikke livssyklus-tilgang til Room. `LynWidget` bruker SharedPreferences som bro — `onMeasurementSaved()` må kalles eksplisitt etter hver lagret måling.
**Fix:** Dokumentert som kritisk gotcha i CLAUDE.md
**Skill oppdatert:** Lagt til i CLAUDE.md

---

### 2026-05-12: Launcher-ikoner ikke committet i repoet

**Context:** `processDebugResources` feilet under første testrun etter SDK-installasjon
**Hva skjedde:** AAPT-feil — `mipmap/ic_launcher` og `ic_launcher_round` fantes ikke i repoet
**Rotårsak:** Android Studio genererer ikoner lokalt ved prosjektopprettelse, men de ble aldri lagt til git
**Fix:** Opprettet `mipmap-anydpi-v26/` med adaptive XML + PNG-fallbacks for alle densiteter, committet dem
**Skill oppdatert:** Lagt til som prosjekt-gotcha i CLAUDE.md

---

### 2026-05-12: `dp`-import ikke med i wildcard-import

**Context:** Kompileringsfeil ved første bygge-forsøk
**Hva skjedde:** "Unresolved reference 'dp'" i `MainActivity.kt`
**Rotårsak:** `androidx.compose.ui.unit.dp` trekkes ikke inn via `foundation.layout.*` — må importeres eksplisitt
**Fix:** La til `import androidx.compose.ui.unit.dp` i `MainActivity.kt`
**Skill oppdatert:** Lagt til som kritisk gotcha i CLAUDE.md

---

## Skill Update Backlog

| Dato | Skill | Hva som trenger oppdatering |
|------|-------|------------------------------|

---

## Mønstre vi har sett

| Mønster | Sett antall ganger | Skill-kandidat? |
|---------|-------------------|-----------------|
test
