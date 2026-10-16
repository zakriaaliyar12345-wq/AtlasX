package com.atlasx.gis.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.IdRes;

import com.atlasx.gis.MainActivity;
import com.atlasx.gis.R;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Installs the persistent bottom navigation on a screen.
 *
 * <p>Destinations are reused with {@link Intent#FLAG_ACTIVITY_REORDER_TO_FRONT}
 * so each screen keeps a single instance and the back stack stays shallow;
 * every screen reloads its data in {@code onResume} to stay correct when it is
 * brought back to the front.</p>
 */
public final class BottomNav {

    private BottomNav() {
    }

    /** Wires the shared navigation bar and marks {@code current} as selected. */
    public static void install(Activity activity, View root, @IdRes int current) {
        BottomNavigationView nav = root.findViewById(R.id.bottomNavigation);
        if (nav == null) {
            return;
        }
        // Select first (no listener attached yet, so nothing is launched).
        nav.setSelectedItemId(current);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == current) {
                return true;
            }
            Class<?> target;
            if (id == R.id.nav_dashboard) {
                target = MainActivity.class;
            } else if (id == R.id.nav_map) {
                target = MapActivity.class;
            } else if (id == R.id.nav_points) {
                target = PointsActivity.class;
            } else if (id == R.id.nav_analysis) {
                target = AnalysisActivity.class;
            } else if (id == R.id.nav_settings) {
                target = SettingsActivity.class;
            } else {
                return false;
            }
            Intent intent = new Intent(activity, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
            return true;
        });
    }

    /** Returns the id used for the given screen, for tests and bookkeeping. */
    public static @IdRes int idFor(Class<?> screen) {
        if (screen == MainActivity.class) return R.id.nav_dashboard;
        if (screen == MapActivity.class) return R.id.nav_map;
        if (screen == PointsActivity.class) return R.id.nav_points;
        if (screen == AnalysisActivity.class) return R.id.nav_analysis;
        if (screen == SettingsActivity.class) return R.id.nav_settings;
        return R.id.nav_dashboard;
    }


}
