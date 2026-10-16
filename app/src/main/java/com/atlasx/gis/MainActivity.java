package com.atlasx.gis;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.atlasx.gis.data.AtlasDatabase;
import com.atlasx.gis.data.SettingsRepository;
import com.atlasx.gis.data.model.SavedPoint;
import com.atlasx.gis.geo.GeoFormat;
import com.atlasx.gis.ui.AnalysisActivity;
import com.atlasx.gis.ui.BottomNav;
import com.atlasx.gis.ui.MapActivity;
import com.atlasx.gis.ui.PointsActivity;
import com.atlasx.gis.ui.ProjectsActivity;
import com.atlasx.gis.ui.SettingsActivity;
import com.atlasx.gis.util.GeoJsonTasks;
import com.atlasx.gis.util.LanguageSwitcher;
import com.atlasx.gis.util.LocaleHelper;
import com.atlasx.gis.util.Ui;

import java.util.List;
import java.util.Locale;

/**
 * Dashboard: the launcher screen of Atlas X.
 *
 * <p>Shows statistics that are computed from the local database, the quick
 * actions that open the other screens, and the most recent points. All numbers
 * are refreshed in {@link #onResume()} so returning from another screen always
 * shows current data.</p>
 */
public class MainActivity extends AppCompatActivity {

    private TextView dashboardSummary;
    private TextView statPoints;
    private TextView statProjects;
    private TextView statStorage;
    private TextView recentEmpty;
    private LinearLayout recentContainer;
    private SettingsRepository settings;
    private GeoJsonTasks geoJson;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.updateBaseContext(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = new SettingsRepository(this);
        dashboardSummary = findViewById(R.id.dashboardSummary);
        statPoints = findViewById(R.id.statPointsValue);
        statProjects = findViewById(R.id.statProjectsValue);
        statStorage = findViewById(R.id.statStorageValue);
        recentEmpty = findViewById(R.id.recentEmpty);
        recentContainer = findViewById(R.id.recentContainer);

        installToolbar();
        installQuickActions();
        geoJson = GeoJsonTasks.install(this, () -> {
            refreshStatistics();
            refreshRecentPoints();
        });
        BottomNav.install(this, findViewById(android.R.id.content), R.id.nav_dashboard);

        if (!settings.isOnboardingDone()) {
            showOnboarding();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatistics();
        refreshRecentPoints();
    }

    private void installToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_language) {
                LanguageSwitcher.showChooser(this);
                return true;
            }
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new android.content.Intent(this, SettingsActivity.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            }
            return false;
        });
    }

    private void installQuickActions() {
        findViewById(R.id.actionOpenMap).setOnClickListener(v ->
                open(MapActivity.class));
        findViewById(R.id.actionOpenPoints).setOnClickListener(v ->
                open(PointsActivity.class));
        findViewById(R.id.actionOpenProjects).setOnClickListener(v ->
                open(ProjectsActivity.class));
        findViewById(R.id.actionOpenAnalysis).setOnClickListener(v ->
                open(AnalysisActivity.class));
        findViewById(R.id.actionImportGeoJson).setOnClickListener(v ->
                geoJson.launchImport());
        findViewById(R.id.actionExportGeoJson).setOnClickListener(v ->
                geoJson.launchExport());
        findViewById(R.id.actionOpenSettings).setOnClickListener(v ->
                open(SettingsActivity.class));
    }

    private void open(Class<?> screen) {
        startActivity(new android.content.Intent(this, screen)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    private void refreshStatistics() {
        AtlasDatabase db = AtlasDatabase.getInstance(this);
        int points = db.countPoints();
        int projects = db.countProjects();

        Locale locale = LocaleHelper.current(this);
        statPoints.setText(String.format(locale, "%d", points));
        statProjects.setText(String.format(locale, "%d", projects));
        statStorage.setText(Ui.formatBytes(db.databaseSizeBytes()));
        dashboardSummary.setText(
                getString(R.string.dashboard_summary_format,
                        getResources().getQuantityString(
                                R.plurals.projects_count, projects, projects),
                        getResources().getQuantityString(
                                R.plurals.points_count, points, points)));
    }

    private void refreshRecentPoints() {
        if (recentContainer == null) {
            return;
        }
        recentContainer.removeAllViews();
        List<SavedPoint> recent =
                AtlasDatabase.getInstance(this).getPoints(null, null, 3);
        recentEmpty.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);

        Locale locale = LocaleHelper.current(this);
        for (SavedPoint point : recent) {
            TextView row = new TextView(this);
            row.setText(getString(R.string.point_item_format,
                    point.getName(),
                    point.getLatitude(),
                    point.getLongitude()));
            row.setTextColor(getColor(R.color.atlas_text_primary));
            row.setTextSize(14);
            row.setPadding(Ui.dp(this, 16), Ui.dp(this, 14),
                    Ui.dp(this, 16), Ui.dp(this, 14));
            row.setBackgroundResource(R.drawable.bg_card);
            row.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pin, 0, 0, 0);
            int gap = Ui.dp(this, 12);
            row.setCompoundDrawablePadding(gap);
            row.setOnClickListener(v -> {
                android.content.Intent intent =
                        new android.content.Intent(this, PointsActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                intent.putExtra(PointsActivity.EXTRA_SEARCH, point.getName());
                startActivity(intent);
            });
            Ui.addWithBottomMargin(recentContainer, row, 8);
        }
    }

    private void showOnboarding() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.onboarding_title)
                .setMessage(R.string.onboarding_message)
                .setPositiveButton(R.string.onboarding_action, (d, w) -> {
                    settings.setOnboardingDone();
                    d.dismiss();
                })
                .setCancelable(false)
                .show();
    }
}
