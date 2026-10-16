package com.atlasx.gis.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.atlasx.gis.util.LocaleHelper;

/**
 * Typed access to the persisted user preferences.
 *
 * <p>SharedPreferences is the storage used for settings (course unit 09.1 and
 * 09.2): language, map tile style, coordinate format, measurement units and a
 * few counters that the dashboard reports.</p>
 */
public class SettingsRepository {

    public static final String PREFS_NAME = "atlasx_settings";

    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_TILE_SOURCE = "tile_source";
    public static final String KEY_COORD_FORMAT = "coordinate_format";
    public static final String KEY_UNITS = "units";
    public static final String KEY_ONBOARDING_DONE = "onboarding_done";
    public static final String KEY_EXPORT_COUNT = "export_count";
    public static final String KEY_MAP_LAT = "map_lat";
    public static final String KEY_MAP_LON = "map_lon";
    public static final String KEY_MAP_ZOOM = "map_zoom";

    /** Tile styles offered in the map layer dialog. */
    public static final String TILE_OSM = "osm";
    public static final String TILE_POSITRON = "positron";
    public static final String TILE_VOYAGER = "voyager";

    public static final String FORMAT_DECIMAL = "decimal";
    public static final String FORMAT_DMS = "dms";

    public static final String UNITS_METRIC = "metric";
    public static final String UNITS_IMPERIAL = "imperial";

    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        Context app = context.getApplicationContext();
        // getApplicationContext() is still null while the Application object
        // is being attached, so fall back to the raw context in that window.
        if (app == null) {
            app = context;
        }
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private String getString(String key, String fallback) {
        return prefs.getString(key, fallback);
    }

    public String getLanguage() {
        return getString(KEY_LANGUAGE, LocaleHelper.LANG_ENGLISH);
    }

    public void setLanguage(String tag) {
        prefs.edit().putString(KEY_LANGUAGE, tag).apply();
    }

    public String getTileSource() {
        return getString(KEY_TILE_SOURCE, TILE_OSM);
    }

    public void setTileSource(String value) {
        prefs.edit().putString(KEY_TILE_SOURCE, value).apply();
    }

    public String getCoordinateFormat() {
        return getString(KEY_COORD_FORMAT, FORMAT_DECIMAL);
    }

    public void setCoordinateFormat(String value) {
        prefs.edit().putString(KEY_COORD_FORMAT, value).apply();
    }

    public boolean isDmsFormat() {
        return FORMAT_DMS.equals(getCoordinateFormat());
    }

    public String getUnits() {
        return getString(KEY_UNITS, UNITS_METRIC);
    }

    public void setUnits(String value) {
        prefs.edit().putString(KEY_UNITS, value).apply();
    }

    public boolean isMetric() {
        return !UNITS_IMPERIAL.equals(getUnits());
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
    }

    public int getExportCount() {
        return prefs.getInt(KEY_EXPORT_COUNT, 0);
    }

    public void increaseExportCount() {
        prefs.edit().putInt(KEY_EXPORT_COUNT, getExportCount() + 1).apply();
    }

    public void saveMapPosition(double lat, double lon, double zoom) {
        prefs.edit()
                .putFloat(KEY_MAP_LAT, (float) lat)
                .putFloat(KEY_MAP_LON, (float) lon)
                .putFloat(KEY_MAP_ZOOM, (float) zoom)
                .apply();
    }

    public float[] loadMapPosition() {
        return new float[]{
                prefs.getFloat(KEY_MAP_LAT, 0f),
                prefs.getFloat(KEY_MAP_LON, 0f),
                prefs.getFloat(KEY_MAP_ZOOM, 2f)
        };
    }

    /** Clears stored map preferences (used by the restore/reset action). */
    public void clearMapPosition() {
        prefs.edit()
                .remove(KEY_MAP_LAT)
                .remove(KEY_MAP_LON)
                .remove(KEY_MAP_ZOOM)
                .apply();
    }
}
