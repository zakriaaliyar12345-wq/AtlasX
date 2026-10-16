# Atlas X — Technical Decisions

Architecture Decision Records for the choices that shaped the codebase.
Each entry: context → decision → consequences.

---

## ADR-1 · Activities + shared bottom-nav instead of Fragments

**Context.** The course teaches activities, intents and lifecycle; the app has
five top-level destinations and three form/detail screens.
**Decision.** One activity per screen. Top-level screens include
`include_bottom_nav.xml` and share a `BottomNav` helper that launches destinations
with `FLAG_ACTIVITY_REORDER_TO_FRONT`, preserving each screen's state.
**Consequences.** Simple, course-aligned, easy to deep-link later. No
`FragmentContainer`/backstack plumbing. Sub-screen results flow through
`startActivityForResult`-style contracts (used once, for map picking).

## ADR-2 · SQLiteOpenHelper instead of Room

**Context.** Unit 10 of the course covers raw SQLite; Room (10.1) would hide the
cursors, `ContentValues` and migrations the assignment is about.
**Decision.** A single `AtlasDatabase extends SQLiteOpenHelper` with explicit
SQL, `Cursor` mapping and hand-written migrations (`DATABASE_VERSION = 3`).
**Consequences.** All SQL is visible and auditable; there is no annotation
processor or extra dependency. The cost is more boilerplate and no compile-time
SQL verification — mitigated by keeping every query inside one class and covering
formats/parsers with JVM tests.

## ADR-3 · AppExecutors instead of AsyncTask

**Context.** Unit 07.1 teaches `AsyncTask`, which is deprecated and has known
lifecycle bugs (it holds an implicit activity reference, breaks on rotation).
**Decision.** `util/AppExecutors` — a small thread pool + main-thread `Handler`.
`GeoJsonTasks` runs import/export on the pool and posts results back explicitly.
**Consequences.** Same teaching shape (background → main) with a supported API;
no leaked activities because the callback is created per operation and cleared.

## ADR-4 · osmdroid as the map engine

**Context.** Needs an embeddable, offline-capable map with custom tile sources,
overlays and no API key.
**Decision.** `org.osmdroid:osmdroid-android:6.1.20` with OSM Standard plus two
CARTO styles as `OnlineTileSourceBase` subclasses; markers via `Marker`,
measurement via `Polyline`/`Polygon`, gestures via `MapEventsReceiver`.
**Consequences.** No Google Play services requirement (works on any API 24+
device incl. the test emulator), tile cache is a plain directory the user can
clear from Settings. Cost: slightly more manual overlay code than the Google SDK.

## ADR-5 · Bilingual resources with LocaleHelper on every activity

**Context.** Persian support must cover the whole UI, including RTL and Persian
digits, and must survive activity recreation.
**Decision.**
`values/strings.xml` (en) + `values-fa/strings.xml` in exact key parity;
`LocaleHelper.updateBaseContext` applied in **every** activity's
`attachBaseContext` and in `AtlasXApp`; `android:localeConfig` declared for the
system per-app language page; formatting numbers through
`GeoFormat` + the active `Locale`.
**Consequences.** A single missed activity would silently ignore the language
setting — this was a real bug during development and the reason the rule is now
mechanical. Key parity is checked in review/build.

## ADR-6 · Plurals and format strings, no concatenation

**Context.** English "1 points" is incorrect; lint flags `setText` concatenation.
**Decision.** All countable strings use `<plurals>` resolved with
`Resources.getQuantityString`; composite strings (summary joins, coordinate
pairs, converter lines) use `%1$s`-style format strings.
**Consequences.** Correct grammar in both languages (verified on-device), lint
`SetTextI18n`/`PluralsCandidate` clean, translators see full sentences.

## ADR-7 · API-37 note: getQuantityString moved off Context

**Context.** Compiling against SDK 37, `Context.getQuantityString(...)` no longer
exists — only `Resources.getQuantityString(...)`.
**Decision.** All call sites go through `getResources().getQuantityString(...)`.
**Consequences.** Compiles on SDK 37 and behaves identically on older devices.

## ADR-8 · Notifications only for file operations

**Context.** Import/export can take a moment; nothing else runs unsupervised.
**Decision.** `Notifier` posts a completion notification for GeoJSON import and
export, with the `POST_NOTIFICATIONS` runtime permission requested on API 33+.
**Consequences.** Demonstrates the notification stack without inventing fake
background work; no ongoing/forground-service UX to maintain.

## ADR-9 · Services, alarms and JobScheduler out of scope

**Context.** Course units 07.4 / 08.2 / 08.3.
**Decision.** Not implemented. Every operation in Atlas X is user-initiated and
short-lived; there is nothing to schedule, wake up for, or survive the UI.
**Consequences.** Smaller process footprint, no battery-policy surface. Documented
as an explicit scope decision rather than an omission (see COURSE_COVERAGE.md).

## ADR-10 · Dependency pins and version catalog

**Context.** Lint suggests newer versions; the environment's repositories decide
what actually resolves.
**Decision.**
- All dependencies live in `gradle/libs.versions.toml`.
- `recyclerview` pinned to **1.4.0** (lint-approved current version; an earlier
  attempt at a nonexistent 1.5.0 was rejected by the resolver and rolled back to
  the version Material itself depends on before being raised to 1.4.0).
- AGP stays at **9.3.3**: 9.4.1 requires Gradle ≥ 9.6.0 while the wrapper is
  9.5.0 — upgrading either side alone breaks the build, so the pair is pinned
  together (the one remaining `AndroidGradlePluginVersion` lint warning is the
  accepted cost).
- `org.json:json` is a **test-only** dependency (20260814) so the GeoJSON unit
  tests can run on the JVM.
**Consequences.** Reproducible builds; every pin has a written reason.

## ADR-11 · Full-reload RecyclerView adapters

**Context.** Lint prefers granular `notifyItemChanged`/DiffUtil.
**Decision.** `PointAdapter`/`ProjectAdapter` expose `submit(List)` which
replaces the backing list and calls `notifyDataSetChanged`.
**Consequences.** Correct for the app's access pattern (reload after every DB
mutation, lists of tens–hundreds of rows). The two remaining lint warnings are
accepted; DiffUtil would need id-based callbacks with no user-visible gain.

## ADR-12 · Hardening pass: prune, don't hoard

**Context.** The baseline project carried many unused strings, colours, styles and
drawables (131 lint warnings).
**Decision.** Removed 97 unused resources (both locale files kept in parity),
replaced hardcoded layout text with `tools:text`, removed redundant root
backgrounds (the theme already paints them), deleted dead `SDK_INT` branches and
the stale `build.gradle.kts`.
**Consequences.** APK slightly smaller, resource files readable, lint reduced to
4 accepted warnings — and the build itself proves nothing needed was deleted.

---

## Decision log during development (short form)

| When | Change | Why |
|---|---|---|
| DB migration | Added v3 migration creating `projects` | v1→2 omitted the table → runtime crash on upgrade |
| `SettingsRepository` | Context fallback in constructor | `getApplicationContext()` is null in `attachBaseContext` |
| Map restore | Ignore saved zoom ≤ 3 | First-run placeholder zooms (2.0) were restored as real state |
| Locale | `attachBaseContext` in **all** activities | Only the dashboard had been localised initially |
| Plurals | `<plurals>` for points/projects/vertices/notifs | Grammar + lint |
| SDK 37 | `getResources().getQuantityString` | Method removed from `Context` in API 37 |
| Layouts | `tools:ignore="MergeRootFrame"` on map root | The FrameLayout genuinely overlays toolbar/HUD on the map; `<merge>` would break `layout_gravity` overlays |
| Build | Removed stale `app/build.gradle.kts` | Duplicate/old script produced a phantom lint finding |
