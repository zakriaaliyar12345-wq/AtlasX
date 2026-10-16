# Atlas X — Test Report

**Date:** 2026-10-16
**Build:** versionName 2.0 (versionCode 2), debug APK
**Toolchain:** Gradle 9.5.0 · AGP 9.3.3 · JDK 24 · compileSdk/targetSdk 37 · minSdk 24
**Device under test:** emulator `Pixel_16K`, Android 17 (API 37), 1080×1920

---

## 1. Automated unit tests — 30/30 passing

Command: `./gradlew test` → `BUILD SUCCESSFUL` (0 failures, 0 errors)

| Test class | Tests | Covers |
|---|---|---|
| `geo/GeoMathTest` | 14 | Haversine distance (known-value checks: ~394 km Tehran→Isfahan range, ~338 km for the tighter band used in assertions), zero/self distance, path length, polygon area and perimeter (square & degenerate cases), vertex-count guards |
| `geo/GeoFormatTest` | 8 | Locale-aware number formatting (English & Persian digits), DMS formatting and parsing (round-trips), decimal↔DMS conversion accuracy, missing-symbol repair (`٫`→`.`), lat/lon range validation |
| `geo/GeoJsonTest` | 7 | FeatureCollection read (properties, coordinates, project id), write round-trip, malformed-document detection, empty/no-point detection, unicode names |
| `ExampleUnitTest` | 1 | Sanity placeholder from the project template |

The `geo/` package has zero Android imports, so the suite runs on a plain JVM in
~2 s. `org.json:json:20260814` is on the test classpath for the GeoJSON tests.

## 2. Static analysis (Android Lint)

Command: `./gradlew lintDebug` → **0 errors, 4 warnings**

| Warning | Count | Disposition |
|---|---|---|
| `AppBundleLocaleChanges` | 1 | **Accepted.** The app switches language at runtime with on-device locale APIs; the Play Core language-download flow does not apply (this project ships as an APK, not a split AAB) |
| `AndroidGradlePluginVersion` | 1 | **Accepted.** AGP 9.4.1 requires Gradle ≥ 9.6.0; the wrapper stays on Gradle 9.5.0 + AGP 9.3.3 (verified working). Upgrade blocked deliberately |
| `NotifyDataSetChanged` | 2 | **Accepted.** Point/project lists use a full-reload `submit()` model; datasets are small (≤ hundreds) and reloaded after each DB change, so DiffUtil would add complexity without measurable benefit |

Lint was cleaned up from an initial **131 warnings** during the polish pass:
97 unused resources pruned (strings in both locales, colours, dimens, styles,
3 drawables), all `SetTextI18n` concatenations replaced with resource formats,
hardcoded layout texts moved to `tools:`, redundant root backgrounds removed
(theme already paints them), dead `SDK_INT` guards deleted, manifest feature
requirement corrected, and stale `build.gradle.kts` removed.

## 3. On-device smoke tests (Android 17 emulator)

Every flow below was executed against the installed debug APK with Logcat watched
for `FATAL EXCEPTION` — **no crashes in any flow**.

| # | Flow | Result | Evidence |
|---|---|---|---|
| 1 | Cold start → dashboard (Persian, RTL) | ✅ | `01-dashboard-fa.png`, summary shows `۰ پروژه · ۱ نقطه` |
| 2 | Dashboard → Map: tiles load, tap shows coordinate readout | ✅ | `02-map-fa.png` |
| 3 | Map measure mode: two taps → distance + vertices shown | ✅ | `09-measure-en.png` |
| 4 | Map layers dialog (OSM / CARTO Positron / CARTO Voyager + label note) | ✅ | `10-layers-dialog-en.png` |
| 5 | Points list (search field, item actions, count `۱ نقطه`) | ✅ | `03-points-fa.png` |
| 6 | FAB → Point editor → fill name/lat/lon → Save → list + SQLite row | ✅ | `04-point-editor-fa.png`; DB `SELECT COUNT(*) FROM points` = 1 |
| 7 | Projects screen (empty state + create flow) | ✅ | `05-projects-fa.png` |
| 8 | Analysis screen (converter + calculators) | ✅ | `06-analysis-fa.png` |
| 9 | Settings screen (all rows render) | ✅ | `07-settings-fa.png` |
| 10 | Language switch Persian → English (whole task recreated) | ✅ | `08-dashboard-en.png`, dashboard reads `0 projects · 1 point` |
| 11 | Language switch back to Persian | ✅ | dashboard returns to `۰ پروژه · ۱ نقطه` |
| 12 | Bottom navigation between all five top-level screens | ✅ | (part of walks above) |
| 13 | Recreation/relaunch with new APK (install -r over existing data) | ✅ | data intact, no migration errors |

Point-creation detail (flow 6): the row `Test Point / 35.500000, 51.500060` was
created through the form, survived activity recreation, appears in the list with
Persian digits, and is persisted in `databases/atlasx_gis.db` (verified with
`run-as … sqlite3`).

## 4. Bugs found and fixed during testing

| # | Symptom | Root cause | Fix |
|---|---|---|---|
| 1 | Crash on app start after first DB upgrade | Migration 1→2 never created the `projects` table (`no such table: projects`) | `DATABASE_VERSION = 3` with an idempotent `CREATE TABLE IF NOT EXISTS projects` migration; verified upgrade on the emulator's existing DB |
| 2 | NPE crash instantiating `SettingsRepository` during `Application.attachBaseContext` | `getApplicationContext()` is null before `Application.onCreate` | Repository falls back to the provided context |
| 3 | Only the dashboard rendered in Persian; all other screens stayed English | Only `MainActivity` wrapped its base context with `LocaleHelper` | Every activity now overrides `attachBaseContext` → `LocaleHelper.updateBaseContext` |
| 4 | Restored map jumped to zoom 2 after relaunch | `restorePosition()` treated any saved zoom as valid | Treat zoom ≤ 3 as "unset" and fall back to 15.0 |
| 5 | `getQuantityString` compile errors on API 37 | The method was **removed from `Context`** in API 37 (still on `Resources`) | All call sites use `getResources().getQuantityString(...)` |
| 6 | "1 points" grammar errors in English | Countable strings were plain `%d …` formats | Proper `<plurals>` resources (points, projects, vertices, import/export texts) verified in both languages on-device |
| 7 | Save button appeared to do nothing during automation | Soft keyboard covered the button while the automation tapped | Product code fine; automation now closes the IME first — persisted-write re-verified |
| 8 | Stale lint warning about a hardcoded osmdroid coordinate | Leftover `app/build.gradle.kts` from the old project still contained the old dependency line | File removed (archived in backup `superseded-files/`) |
| 9 | Dashboard summary "0 projects · 0 points" was invisible (reported by user) | White text (`atlas_text_inverse`) on a near-white pill (`bg_pill`); the same color also inverted wrongly in dark mode on the navy card, map readout and FAB icons | Summary text now `atlas_text_primary` + bold; new always-white `atlas_text_on_dark` color for all text/icons on dark surfaces (verified with pixel sampling: 276 dark text pixels on the light pill) |

## 5. Build verification matrix

| Command | Result |
|---|---|
| `./gradlew clean assembleDebug` | ✅ BUILD SUCCESSFUL |
| `./gradlew test` | ✅ 30 tests, 0 failures |
| `./gradlew lintDebug` | ✅ 0 errors, 4 accepted warnings |
| `adb install -r app-debug.apk` | ✅ streamed install success (over previous data) |
| Manual smoke (§3) | ✅ 13/13 flows, no crash |

## 6. Known limitations

- Instrumented Espresso tests are configured but not written; UI evidence is the
  documented manual pass with screenshots.
- Offline behaviour is verified only to the extent of the offline banner + cached
  tiles; no airplane-mode soak test.
- GeoJSON import/export was code-reviewed and unit-tested at the parser level;
  the SAF round-trip on-device was exercised for export-menu wiring only.
