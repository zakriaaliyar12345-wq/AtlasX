# Atlas X — User Guide

Atlas X keeps your geographic points, projects and measurements on your device.
It works in **English** and **Persian (فارسی)** — switch any time from the
dashboard's language button or the Settings screen. Persian mode is fully
right-to-left and shows Persian digits.

---

## 1. Dashboard

The home screen answers "what is in my workspace?" at a glance:

- **Workspace summary** — e.g. `3 projects · 48 points`, using correct singular
  and plural wording in the active language.
- **Statistics cards** — saved points, projects, local database size.
- **Quick actions** — open the map, saved points, projects, analysis, import a
  GeoJSON file, export all points.
- **Recent points** — the latest saved points; tap one to open it for editing.
- **Language button** (top bar) — toggles between English and Persian; the whole
  app refreshes immediately.

## 2. Map

The map is the main exploration tool (OpenStreetMap and CARTO styles).

| Gesture / control | Action |
|---|---|
| Drag, pinch | Pan and zoom |
| Tap | Shows the exact coordinate of the tapped location at the bottom |
| **Long-press** | Opens the point form with that location already filled in |
| Layers button | Switch tile style: OSM Standard, CARTO Positron (light), CARTO Voyager (colourful). Also toggles the saved-point markers |
| Ruler button | Measurement mode — tap the map to drop vertices; distance, and (from three vertices) area and perimeter update live. Tap the ruler again or **Clear** to finish |
| Location button | Flies to your GPS position (asks for location permission the first time) |
| Add button (FAB) | Creates a point at the current map centre |

Notes:
- When there is no internet connection a banner tells you that only cached tiles
  are shown; everything else keeps working.
- The map remembers where you left off (position and zoom) and restores it when
  you come back.
- Pick mode (opened from the point form via **Pick on map**) hides the other
  controls; tap once to choose the location and return to the form.

## 3. Points

- **Add** — green ＋ button: name (required), latitude, longitude (decimal like
  `35.7`, or DMS like `35°42'10.5"N` if you use the DMS format in Settings),
  optional description and project.
- **Search** — type in the search box to filter by name or description; the count
  line shows e.g. `Showing 2 of 48 points`.
- **Edit / delete** — icons on each row.
- **Import GeoJSON** — menu → picks a `.geojson`/`.json` file; Point features are
  added to your list. A notification reports how many points were imported.
- **Export GeoJSON** — menu → saves all points as a GeoJSON FeatureCollection.
- **Share points** — menu → sends the points as readable text to any app.

## 4. Projects

Projects group points without moving them:

- Create a project with the ＋ button.
- Rename (pencil) or delete (trash) from the row. Deleting a project keeps its
  points — they simply become unassigned.
- Assign a project to a point from the point form's project dropdown.

## 5. Analysis

Three tools in one screen:

1. **Coordinate converter** — paste a decimal (`-35.5123`) or DMS
  (`35°30'44.3"S`) value; get the other format instantly.
2. **Distance** — enter two coordinates → great-circle distance (metric or
  imperial, per Settings).
3. **Area / perimeter** — paste a list of vertices (one `lat,lon` pair per line)
  → polygon area, perimeter and vertex count. The same engine powers the map's
  measure mode.

## 6. Settings

| Setting | Options |
|---|---|
| Language | English / فارسی |
| Coordinate format | Decimal (35.7412) / DMS (35°44'28.3"N) — affects the whole app |
| Units | Metric (km, km²) / Imperial (mi, mi²) |
| Tile source | OSM Standard / CARTO Positron / CARTO Voyager |
| Map cache size | How much space downloaded tiles use; can be cleared |
| Reset saved map view | Forgets the remembered position/zoom |
| Diagnostics | Point/project counts and database size |
| About | Version and privacy note |

## 7. Data & privacy

- Everything is stored **on this device**: points and projects in the app's
  SQLite database, preferences in SharedPreferences, tiles in the app cache.
- The only internet traffic is downloading map tiles.
- GeoJSON import/export uses the system document picker; Atlas X never reads
  other files.
- There are no accounts, no ads and no analytics.

## 8. Troubleshooting

| Situation | What to do |
|---|---|
| Map stays blank | Check the offline banner; switch tile source in the layers dialog or Settings; clear the tile cache in Settings |
| Location button does nothing | Grant the location permission in system settings (the app asks politely the first time) |
| Import says "no points" | The file must be a GeoJSON `FeatureCollection` with `Point` features |
| Language does not change | Use the dashboard language button; every screen follows the saved choice after the app refreshes |
