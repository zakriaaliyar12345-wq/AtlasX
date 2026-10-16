# Atlas X — Architecture

## 1. Overview

Atlas X is a single-process, activity-based Android application written in Java.
It follows a simple layered architecture with a hard boundary between UI code and
pure, unit-testable domain logic:

```
┌────────────────────────────────────────────────────────────┐
│  UI layer (app/src/main/java/com/atlasx/gis/ui)            │
│  6 Activities + 2 RecyclerView adapters + BottomNav        │
│  Material widgets, osmdroid MapView, dialogs               │
├────────────────────────────────────────────────────────────┤
│  Application & entry (com.atlasx.gis)                      │
│  AtlasXApp (Application) · MainActivity (dashboard)        │
├────────────────────────────────────────────────────────────┤
│  Data layer (data/)                                        │
│  AtlasDatabase  — SQLiteOpenHelper, schema v3, migrations  │
│  SettingsRepository — SharedPreferences facade             │
│  model/SavedPoint, model/Project — plain immutable POJOs   │
├────────────────────────────────────────────────────────────┤
│  Domain layer (geo/)  — pure Java, zero Android imports     │
│  GeoMath · GeoFormat · GeoJson                             │
├────────────────────────────────────────────────────────────┤
│  Platform utilities (util/)                                │
│  AppExecutors · LocaleHelper · LanguageSwitcher            │
│  Notifier · GeoJsonTasks · Ui                              │
└────────────────────────────────────────────────────────────┘
```

The `geo/` package deliberately has **no Android dependencies**, which is what makes
the 29 domain unit tests runnable on a plain JVM in milliseconds.

## 2. Package responsibilities

### `com.atlasx.gis` (root)
- **`AtlasXApp`** — `Application` subclass. Applies the persisted locale in
  `attachBaseContext` and primes the osmdroid configuration (user-agent, sensor
  permissions, tile cache location) in `onCreate`.
- **`MainActivity`** — the dashboard. Computes live statistics from the database
  (`countPoints`, `countProjects`, `databaseSizeBytes`), renders recent points,
  exposes quick actions, and hosts the in-app language switch.

### `data/`
- **`AtlasDatabase`** (singleton via `getInstance(Context)`) — the only class that
  touches SQLite. Extends `SQLiteOpenHelper`; `DATABASE_VERSION = 3`. The file path is
  resolved once from `context.getDatabasePath(...)` so that helper and callers always
  agree on the same file.
  - Tables: `points` (id, name, lat, lon, description, project_id, created_at) and
    `projects` (id, name, created_at). `project_id` is a soft foreign key (kept even if
    a project row is deleted, points become unassigned).
  - Migration 1→2 adds description/project columns; migration 2→3 creates the
    `projects` table (`CREATE TABLE IF NOT EXISTS`, so it is idempotent on databases
    that already had it).
  - Queries: `getPoints(query, projectId, limit)` for search/filter, aggregate
    `countPoints/countProjects`, `databaseSizeBytes` for diagnostics.
- **`SettingsRepository`** — thin, typed wrapper over one `SharedPreferences` file
  (`atlasx_settings`): language tag, DMS vs decimal coordinates, metric vs imperial
  units, tile source, onboarding flag, export counter, last map position.
  Falls back to the raw context when `getApplicationContext()` is null so it is safe
  to construct during `Application.attachBaseContext`.
- **`model/`** — `SavedPoint` and `Project`: immutable value objects built from
  `Cursor` rows.

### `geo/` (pure domain)
- **`GeoMath`** — haversine distance, polyline length, spherical polygon area
  (Chamberlain–Duquette), perimeter. All in metres.
- **`GeoFormat`** — locale-aware number formatting (Persian digits for `fa`),
  decimal ↔ DMS conversion, coordinate parsing with a single missing-symbol fix
  (`٫` → `.`), lat/lon validation.
- **`GeoJson`** — reader/writer for the GeoJSON `FeatureCollection` profile used by
  the app (Point features, `name`/`description`/`project` properties); structured
  error reporting via `ErrorKind` (malformed / no points / io).

### `ui/`
- **`MapActivity`** — the map workspace. Owns the osmdroid `MapView`, marker overlay
  (`Marker` + `InfoWindow`), map-events receiver (tap/long-press), measure overlays
  (`Polyline` / `Polygon`), the coordinate readout (`MapListener` for scroll/zoom),
  my-location overlay with runtime permission flow (`REQUEST_LOCATION_PERMISSION`),
  a connectivity-change `BroadcastReceiver`, tile-source switching (OSM standard,
  CARTO Positron, CARTO Voyager) and pick-a-location mode (transparent UI, returns
  the chosen coordinate through `setResult`).
- **`PointsActivity`** — search + `RecyclerView` list of points; menu actions for
  import/export/share; launches the editor (new/edit) and the map in pick mode.
- **`PointEditorActivity`** — validated create/edit form; optional project spinner;
  can pre-fill latitude/longitude from an intent extra or from the map pick result.
- **`ProjectsActivity`** — CRUD list of projects with point counts.
- **`AnalysisActivity`** — DMS converter, two-point distance, path/area/perimeter
  calculators built on `GeoMath`/`GeoFormat`.
- **`SettingsActivity`** — preference-style screen writing through
  `SettingsRepository`; cache sizing and diagnostics read-only values.
- **`adapter/PointAdapter`, `adapter/ProjectAdapter`** — `ListAdapter`-style
  adapters with an explicit `submit(list)` full-reload model.
- **`BottomNav`** — one helper that wires the shared
  `include_bottom_nav.xml` menu to activity launches using
  `FLAG_ACTIVITY_REORDER_TO_FRONT`, so back behaviour stays natural and the five
  top-level screens keep their state.

### `util/`
- **`AppExecutors`** — fixed background pool + main-thread `Handler`. Replaces
  `AsyncTask` (deprecated, removed from the course's recommended practice).
- **`LocaleHelper`** — the single place that applies a language tag to a `Context`
  (`Configuration.setLocale` + `LocaleList` + `createConfigurationContext`). Every
  activity overrides `attachBaseContext` with it — missing even one activity causes
  that screen to ignore the language setting, which is exactly the bug this design
  prevents.
- **`LanguageSwitcher`** — writes the new language to preferences and restarts the
  task so the change is immediate and process-wide.
- **`Notifier`** — wraps `NotificationManager` (with `POST_NOTIFICATIONS` runtime
  permission on API 33+) for the two long-running file operations.
- **`GeoJsonTasks`** — runs GeoJSON import/export on `AppExecutors`, streams via the
  Storage Access Framework (`ContentResolver`), posts success/failure toasts and the
  progress notification, and reports back through a small callback interface.
- **`Ui`** — toast, byte-size formatting, dialog helpers.

## 3. Navigation model

Activities, not fragments. The five top-level screens each inflate
`include_bottom_nav.xml`; `BottomNav.install(activity, selectedId)` handles item
selection with re-order-to-front launches. Sub-screens (point editor, map) are opened
normally and finish() back. Result passing is used exactly once:
`MapActivity` pick mode returns `EXTRA_PICK_LAT` / `EXTRA_PICK_LON`.

```
MainActivity ⇄ MapActivity ⇄ PointEditorActivity
     ⇅              (pick mode returns a coordinate)
PointsActivity → PointEditorActivity
ProjectsActivity          AnalysisActivity          SettingsActivity
```

## 4. Data flow (example: import a GeoJSON file)

```
PointsActivity
  └─ SAF ACTION_OPEN_DOCUMENT → Uri
       └─ GeoJsonTasks.importFrom(uri, callback)
            ├─ background: ContentResolver.openInputStream
            │    └─ GeoJson.read(...) → List<SavedPoint>
            │         └─ AtlasDatabase.insertPoint(...)  (transaction per point)
            ├─ main: toast + Notifier notification
            └─ callback → PointsActivity.reload()  (adapter.submit + counts)
```

## 5. Threading model

- **UI thread** — all view mutation, database *reads* on small tables (single-digit ms).
- **Background pool** (`AppExecutors`, 2–4 threads) — GeoJSON parse/serialise, file
  I/O, tile-cache directory sizing.
- **Main handler** — results posted back with `AppExecutors.runOnMain`.
- osmdroid loads tiles on its own thread pool; the app never blocks on network.

## 6. Localisation

- Two resource sets: `values/strings.xml` (en, the default) and `values-fa/strings.xml`.
  Keys are kept in exact parity (verified by script and by build).
- `android:localeConfig="@xml/locales_config"` + `android:supportsRtl="true"` declare
  per-app language support; the in-app switch additionally works fully offline.
- Numbers: formatting goes through `GeoFormat` with the active `Locale`, so Persian
  users see Persian digits (۳۵٫۵) while technical input fields stay Latin.
- Layouts use `start/end` (not `left/right`) margins and paddings throughout.
- Countable strings use `<plurals>` (points, projects, vertices, import/export
  messages) resolved through `Resources.getQuantityString`.

## 7. Map stack (osmdroid)

- `MapView` with online tile sources; tiles cached under `getCacheDir()/osmdroid/tiles`.
- Overlays: `Marker` (saved points, draggable info window), `Polyline`/`Polygon`
  (measurement), `MapEventsReceiver` (tap, long-press), `MyLocationNewOverlay`
  (GPS, runtime permission).
- Map camera (lat, lon, zoom) is persisted in `SettingsRepository` and restored on
  resume (with a sanity check that treats near-zero zooms as "unset").
- Offline state is observed with a `ConnectivityManager` callback +
  `CONNECTIVITY_ACTION` receiver; a banner informs the user when tiles cannot load.

## 8. Persistence contracts

| Store | Owner | Contents |
|---|---|---|
| SQLite `atlasx_gis.db` | `AtlasDatabase` | points, projects |
| SharedPreferences `atlasx_settings` | `SettingsRepository` | language, DMS flag, units, tile source, map position, onboarding, export count |
| osmdroid tile cache | osmdroid | downloaded tiles (`cache/…/osmdroid/tiles`) |

## 9. Extensibility notes

- Adding a language = adding `values-<tag>/strings.xml` + a `locales_config` entry.
- Adding a tile source = new `OnlineTileSourceBase` subclass + one string pair.
- The `geo/` layer could be reused unchanged in instrumented tests or a future
  content provider.
