# Atlas X — Course Coverage Matrix

This document maps every unit of the Android course
(*C:\Users\Zakria\Desktop\Adroid Programming\*, 32 PowerPoint units) to the
Atlas X implementation.

**Legend**

| Symbol | Meaning |
|---|---|
| ✅ | Fully covered by the application |
| 🟡 | Partially covered / deliberately adapted (reason given) |
| ❌ | Not covered (reason given) |

---

## Module 01 — Introduction

| # | Unit | Status | Where |
|---|------|--------|-------|
| 01.0 | Introduction to Android | ✅ | Complete project: manifest, resources, emulator deployment, API 24–37 support |
| 01.1 | Your first Android app | ✅ | `AtlasXApp` + `MainActivity` (launcher), built and installed on the API 37 emulator |
| 01.2 | Layouts and resources for the UI | ✅ | 10 layouts (`activity_*.xml`, `include_bottom_nav.xml`, `item_*.xml`), `values/colors.xml`, `dimens.xml`, `themes.xml`, `values-night` palette, English + Persian resource sets (176 strings + 6 plurals each) |
| 01.3 | Text and scrolling views | ✅ | `TextView`/`EditText` (TextInputLayout) throughout; `ScrollView` dashboard; `RecyclerView` point/project lists; hint/error text handling |
| 01.4 | Resources to help you learn | ✅ | Android Studio + official docs used during development; decisions recorded in `docs/TECHNICAL_DECISIONS.md` |

## Module 02 — Activities, intents, lifecycle

| # | Unit | Status | Where |
|---|------|--------|-------|
| 02.1 | Activities and Intents | ✅ | 7 activities (`MainActivity`, `MapActivity`, `PointsActivity`, `PointEditorActivity`, `ProjectsActivity`, `AnalysisActivity`, `SettingsActivity`); explicit intents with extras (`EXTRA_POINT_ID`, `EXTRA_POINT_NAME`, `EXTRA_LATITUDE`/`EXTRA_LONGITUDE`, `EXTRA_PICK_MODE`) |
| 02.2 | Activity lifecycle and state | ✅ | `onResume` reloads data and the marker set; map camera saved in `onPause` and restored (with sanity check) in `onCreate`; language switch recreates the task; database singleton survives configuration |
| 02.3 | Implicit Intents | ✅ | Storage Access Framework (`ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT`) for GeoJSON, `ACTION_SEND` for sharing points, `Intent` result contract from map pick mode |

## Module 03 — Debugging and testing

| # | Unit | Status | Where |
|---|------|--------|-------|
| 03.1 | The Android Studio debugger | 🟡 | Debugging was done with Logcat (`android.util.Log` in `GeoJsonTasks`) and the IDE debugger during development; it is a tool, not an app artifact |
| 03.2 | App testing | ✅ | 30 JUnit unit tests (`geo/GeoMathTest`, `GeoFormatTest`, `GeoJsonTest`), `lintDebug` (0 errors), documented manual smoke-test pass on the emulator (`docs/TEST_REPORT.md`); Espresso wired via `androidTestImplementation` |
| 03.3 | The Android Support Library | ✅ | Modern successor in use: AndroidX (`appcompat`, `recyclerview`, `constraintlayout`, `activity-ktx`) + Material Components |

## Module 04 — Controls, menus, navigation, lists

| # | Unit | Status | Where |
|---|------|--------|-------|
| 04.1 | Buttons and clickable images | ✅ | Material buttons, FAB (add point), icon buttons with `contentDescription`, card-style quick actions on the dashboard |
| 04.2 | Input controls | ✅ | Form fields with `inputType` (`numberDecimal|numberSigned`, `textCapSentences`), TextInputLayout validation, search field, project `Spinner` |
| 04.3 | Menus and pickers | ✅ | Toolbar menus (`menu_map.xml`: layers/measure/location; dashboard & points menus); MaterialAlertDialog single-choice pickers for language, tile source, coordinate format, units |
| 04.4 | User navigation | ✅ | `BottomNavigationView` on all top-level screens (`BottomNav` helper, `FLAG_ACTIVITY_REORDER_TO_FRONT`), toolbar up-navigation via `parentActivityName`, activity-result flow for map picking |
| 04.5 | RecyclerView | ✅ | `PointAdapter` + `PointViewHolder`, `ProjectAdapter` + `ProjectViewHolder`, `item_point.xml` / `item_project.xml`, edit & delete item actions |

## Module 05 — Visual design

| # | Unit | Status | Where |
|---|------|--------|-------|
| 05.1 | Drawables, styles, and themes | ✅ | Vector shape drawables (`bg_card`, `bg_pill`, `bg_badge_info`, `bg_card_navy`), `Theme.AtlasX` + widget/text styles in `themes.xml`, night-mode palette in `values-night/colors.xml` |
| 05.2 | Material Design | ✅ | Material 3 theme (`Theme.Material3.DayNight.NoActionBar` parent), `MaterialToolbar`, `BottomNavigationView`, `TextInputLayout`, Material dialogs, elevation & active-indicator styling |
| 05.3 | Resources for adaptive layouts | ✅ | Weight-based stat cards, `match_parent`/`wrap_content` discipline, `ScrollView` for overflow, start/end (RTL-safe) spacing, separate locale resource sets + `localeConfig` |

## Module 06 — UI testing

| # | Unit | Status | Where |
|---|------|--------|-------|
| 06.1 | UI testing | 🟡 | Espresso dependencies and runner are configured, but the delivered evidence is a scripted manual UI walkthrough of all 7 screens (both languages) on the emulator — see `docs/TEST_REPORT.md` with screenshots |

## Module 07 — Background work and the internet

| # | Unit | Status | Where |
|---|------|--------|-------|
| 07.1 | AsyncTask and AsyncTaskLoader | 🟡 | **Intentionally not used** — `AsyncTask` is deprecated; the equivalent pattern is `AppExecutors` (thread pool + main handler), used by `GeoJsonTasks` (see TECHNICAL_DECISIONS.md) |
| 07.2 | Internet connection | ✅ | `INTERNET` + `ACCESS_NETWORK_STATE` permissions; osmdroid downloads OSM/CARTO tiles; offline banner when connectivity is lost; tile cache sized in Settings |
| 07.3 | Broadcasts | ✅ | Runtime-registered `BroadcastReceiver` for `ConnectivityManager.CONNECTIVITY_ACTION` in `MapActivity` (shows/hides the offline banner) |
| 07.4 | Services | ❌ | No long-lived background work exists in this app: imports/exports finish in seconds on the UI-owned thread pool. Adding a service would add lifecycle surface without a requirement (TECHNICAL_DECISIONS.md §7) |

## Module 08 — Notifications and scheduled work

| # | Unit | Status | Where |
|---|------|--------|-------|
| 08.1 | Notifications | ✅ | `Notifier.notifyFileOperation` posts import/export completion notifications; `POST_NOTIFICATIONS` runtime permission requested on API 33+ |
| 08.2 | Alarms | ❌ | The app has no time-based triggers; every operation is user-initiated (TECHNICAL_DECISIONS.md §7) |
| 08.3 | Efficient data transfer and JobScheduler | ❌ | Transfers are small (single GeoJSON files) and streamed directly through the SAF `ContentResolver`; there is no periodic or deferred work that would justify JobScheduler (TECHNICAL_DECISIONS.md §7) |

## Module 09 — Data storage

| # | Unit | Status | Where |
|---|------|--------|-------|
| 09.0 | Data Storage | ✅ | Three storage mechanisms demonstrated: SQLite (points/projects), SharedPreferences (settings), document files via SAF (GeoJSON import/export) |
| 09.1 | Shared Preferences | ✅ | `SettingsRepository` — typed facade over `SharedPreferences` (language, coordinate format, units, tile source, map position, onboarding, export count) |
| 09.2 | App settings | ✅ | `SettingsActivity` + `activity_settings.xml`: language, coordinate format, units, tile source, cache size, map-view reset, storage diagnostics, about |

## Module 10 — SQLite

| # | Unit | Status | Where |
|---|------|--------|-------|
| 10.0 | SQLite Primer | ✅ | `AtlasDatabase` (SQLiteOpenHelper, schema v3): `insert/update/delete/query` with `ContentValues` + `Cursor`, parameterised search, transactions for bulk import, explicit migrations 1→2→3 |
| 10.1 | Room, LiveData, and ViewModel | 🟡 | **Deliberately adapted** — plain `SQLiteOpenHelper` is used instead of Room so the SQLite concepts from unit 10.0 stay visible; trade-off documented in TECHNICAL_DECISIONS.md §2 |

---

## Summary

| Status | Units | Share |
|---|---|---|
| ✅ Fully covered | 25 | 78 % |
| 🟡 Adapted / partial | 4 | 13 % |
| ❌ Not covered | 3 | 9 % |
| **Total (unit files)** | **32** | **100 %** |

### Why three units are out of scope

| Unit | Reason |
|---|---|
| 07.4 Services | All work is short-lived and user-visible; a foreground service would be over-engineering and requires ongoing notification UX with no product requirement |
| 08.2 Alarms | No scheduled behaviour exists (no reminders, syncs or timers) |
| 08.3 JobScheduler | No periodic/deferred jobs; file transfers are synchronous from the user's point of view and small |

The two 🟡 adaptations (AsyncTask → `AppExecutors`, Room → `SQLiteOpenHelper`) are
teaching-driven: the modern replacement is implemented **and** the underlying concept
(the old API's job) remains visible in the code, with the rationale written down.
