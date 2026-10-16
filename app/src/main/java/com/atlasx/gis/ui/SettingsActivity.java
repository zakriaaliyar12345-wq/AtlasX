package com.atlasx.gis.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.atlasx.gis.R;
import com.atlasx.gis.data.AtlasDatabase;
import com.atlasx.gis.data.SettingsRepository;
import com.atlasx.gis.util.LanguageSwitcher;
import com.atlasx.gis.util.LocaleHelper;
import com.atlasx.gis.util.Ui;

import java.io.File;
import java.util.Locale;

/**
 * Application preferences (course unit 09.x — SharedPreferences based):
 * language, map style, coordinate format, measurement units, cache and
 * storage maintenance, plus about and diagnostics information.
 */
public class SettingsActivity extends AppCompatActivity {

    private SettingsRepository settings;

    private TextView languageValue;
    private TextView tileSourceValue;
    private TextView coordinateFormatValue;
    private TextView unitsValue;
    private TextView cacheValue;
    private TextView diagnosticsValue;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.updateBaseContext(base));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = new SettingsRepository(this);

        languageValue = findViewById(R.id.languageValue);
        tileSourceValue = findViewById(R.id.tileSourceValue);
        coordinateFormatValue = findViewById(R.id.coordinateFormatValue);
        unitsValue = findViewById(R.id.unitsValue);
        cacheValue = findViewById(R.id.cacheValue);
        diagnosticsValue = findViewById(R.id.diagnosticsValue);
        TextView versionValue = findViewById(R.id.versionValue);

        findViewById(R.id.rowLanguage).setOnClickListener(v ->
                LanguageSwitcher.showChooser(this));
        findViewById(R.id.rowTileSource).setOnClickListener(v -> chooseTileSource());
        findViewById(R.id.rowCoordinateFormat).setOnClickListener(v -> chooseCoordinateFormat());
        findViewById(R.id.rowUnits).setOnClickListener(v -> chooseUnits());
        findViewById(R.id.rowClearCache).setOnClickListener(v -> confirmClearCache());
        findViewById(R.id.rowResetMap).setOnClickListener(v -> resetMapView());
        findViewById(R.id.rowPrivacy).setOnClickListener(v -> showMessage(
                getString(R.string.about_privacy_title),
                getString(R.string.about_privacy_message)));
        findViewById(R.id.rowAttribution).setOnClickListener(v -> showMessage(
                getString(R.string.about_attribution_title),
                getString(R.string.about_attribution_message)));
        findViewById(R.id.rowOpenSource).setOnClickListener(v -> showMessage(
                getString(R.string.about_open_source_title),
                getString(R.string.about_open_source_message)));

        versionValue.setText(getString(R.string.about_version, appVersion()));

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        BottomNav.install(this, findViewById(android.R.id.content), R.id.nav_settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        languageValue.setText(LocaleHelper.LANG_PERSIAN.equals(settings.getLanguage())
                ? R.string.language_persian : R.string.language_english);

        String tile = settings.getTileSource();
        tileSourceValue.setText(tile.equals(SettingsRepository.TILE_POSITRON)
                ? R.string.tile_positron
                : tile.equals(SettingsRepository.TILE_VOYAGER)
                ? R.string.tile_voyager : R.string.tile_standard);

        coordinateFormatValue.setText(settings.isDmsFormat()
                ? R.string.coord_dms : R.string.coord_decimal);
        unitsValue.setText(settings.isMetric() ? R.string.unit_metric : R.string.unit_imperial);

        cacheValue.setText(getString(R.string.cache_size_format,
                Ui.formatBytes(directorySize(new File(getCacheDir(), "osmdroid/tiles")))));

        AtlasDatabase db = AtlasDatabase.getInstance(this);
        int pointCount = db.countPoints();
        int projectCount = db.countProjects();
        diagnosticsValue.setText(getString(R.string.diagnostics_format,
                getResources().getQuantityString(
                        R.plurals.points_count, pointCount, pointCount),
                getResources().getQuantityString(
                        R.plurals.projects_count, projectCount, projectCount)));
    }

    private void chooseTileSource() {
        final String[] values = {
                SettingsRepository.TILE_OSM,
                SettingsRepository.TILE_POSITRON,
                SettingsRepository.TILE_VOYAGER
        };
        int checked = settings.getTileSource().equals(SettingsRepository.TILE_POSITRON) ? 1
                : settings.getTileSource().equals(SettingsRepository.TILE_VOYAGER) ? 2 : 0;
        new AlertDialog.Builder(this)
                .setTitle(R.string.tile_source_title)
                .setSingleChoiceItems(new String[]{
                        getString(R.string.tile_standard),
                        getString(R.string.tile_positron),
                        getString(R.string.tile_voyager)
                }, checked, (dialog, which) -> {
                    settings.setTileSource(values[which]);
                    refresh();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void chooseCoordinateFormat() {
        int checked = settings.isDmsFormat() ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle(R.string.coordinate_format_title)
                .setSingleChoiceItems(new String[]{
                        getString(R.string.coord_decimal),
                        getString(R.string.coord_dms)
                }, checked, (dialog, which) -> {
                    settings.setCoordinateFormat(
                            which == 1 ? SettingsRepository.FORMAT_DMS
                                    : SettingsRepository.FORMAT_DECIMAL);
                    refresh();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void chooseUnits() {
        int checked = settings.isMetric() ? 0 : 1;
        new AlertDialog.Builder(this)
                .setTitle(R.string.units_title)
                .setSingleChoiceItems(new String[]{
                        getString(R.string.unit_metric),
                        getString(R.string.unit_imperial)
                }, checked, (dialog, which) -> {
                    settings.setUnits(which == 1
                            ? SettingsRepository.UNITS_IMPERIAL
                            : SettingsRepository.UNITS_METRIC);
                    refresh();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmClearCache() {
        Ui.confirm(this,
                getString(R.string.cache_title),
                getString(R.string.cache_clear_confirm),
                getString(R.string.clear_cache),
                () -> {
                    deleteRecursively(new File(getCacheDir(), "osmdroid/tiles"));
                    refresh();
                    Ui.toast(this, R.string.cache_cleared);
                });
    }

    private void resetMapView() {
        settings.clearMapPosition();
        Ui.toast(this, R.string.reset_map_done);
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private String appVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName != null ? info.versionName : "?";
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    private static long directorySize(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        if (file.isDirectory()) {
            long total = 0L;
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    total += directorySize(child);
                }
            }
            return total;
        }
        return file.length();
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
