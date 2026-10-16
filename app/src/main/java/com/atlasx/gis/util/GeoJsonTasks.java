package com.atlasx.gis.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.atlasx.gis.R;
import com.atlasx.gis.data.AtlasDatabase;
import com.atlasx.gis.data.SettingsRepository;
import com.atlasx.gis.data.model.SavedPoint;
import com.atlasx.gis.geo.GeoJson;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * GeoJSON import and export using the Storage Access Framework.
 *
 * <p>File reading, JSON parsing and writing all run on a background executor
 * (course unit 07.1 covers the deprecated AsyncTask; this is the supported
 * replacement). Results are reported with a toast and, when the permission is
 * available, a notification.</p>
 *
 * <p>The class registers its own activity result launchers, so it must be
 * created in the host activity's {@code onCreate()}.</p>
 */
public class GeoJsonTasks {

    /** Callback used by the host screen to refresh its data. */
    public interface Callback {
        void onGeoJsonFinished();
    }

    private static final String MIME_GEOJSON = "application/geo+json";
    private static final String MIME_ANY = "*/*";
    private static final String EXPORT_FILE_NAME = "atlasx-points.geojson";

    private final ComponentActivity activity;
    private final Callback callback;
    private final ActivityResultLauncher<Intent> importLauncher;
    private final ActivityResultLauncher<Intent> exportLauncher;

    private GeoJsonTasks(ComponentActivity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;

        importLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK
                            || result.getData() == null
                            || result.getData().getData() == null) {
                        return;
                    }
                    importFrom(result.getData().getData());
                });

        exportLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK
                            || result.getData() == null
                            || result.getData().getData() == null) {
                        return;
                    }
                    exportTo(result.getData().getData());
                });
    }

    /** Creates the helper for an activity; call from {@code onCreate()}. */
    public static GeoJsonTasks install(ComponentActivity activity, Callback callback) {
        return new GeoJsonTasks(activity, callback);
    }

    /** Opens the system document picker so the user can choose a file. */
    public void launchImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MIME_ANY);
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{MIME_GEOJSON, "application/json", "text/plain", MIME_ANY});
        importLauncher.launch(intent);
    }

    /**
     * Opens the save dialog for an export. When there is nothing to export the
     * user is told instead of being shown a pointless file picker.
     */
    public void launchExport() {
        if (AtlasDatabase.getInstance(activity).countPoints() == 0) {
            Ui.toast(activity, R.string.geojson_export_empty);
            return;
        }
        Notifier.requestIfNeeded(activity);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(MIME_GEOJSON);
        intent.putExtra(Intent.EXTRA_TITLE, EXPORT_FILE_NAME);
        exportLauncher.launch(intent);
    }

    // --------------------------------------------------------------- import

    private void importFrom(Uri uri) {
        Ui.toast(activity, R.string.loading);
        AppExecutors.runOnBackground(() -> {
            String error = null;
            int imported = 0;
            GeoJson.ErrorKind kind = GeoJson.ErrorKind.NONE;
            try {
                String text = readText(uri);
                GeoJson.ImportResult parsed = GeoJson.parse(text);
                if (!parsed.isSuccess()) {
                    kind = parsed.kind;
                    error = parsed.error;
                } else {
                    AtlasDatabase db = AtlasDatabase.getInstance(activity);
                    for (SavedPoint point : parsed.points) {
                        if (point.getName() == null || point.getName().isEmpty()) {
                            point.setName(activity.getString(R.string.imported_point_default));
                        }
                        db.insertPoint(point);
                        imported++;
                    }
                }
            } catch (SecurityException e) {
                kind = GeoJson.ErrorKind.OTHER;
                error = e.getMessage();
            } catch (OutOfMemoryError e) {
                kind = GeoJson.ErrorKind.OTHER;
                error = "file too large";
            } catch (RuntimeException e) {
                android.util.Log.e("AtlasX", "GeoJSON import failed", e);
                kind = GeoJson.ErrorKind.OTHER;
                error = e.getMessage();
            } catch (Exception e) {
                android.util.Log.e("AtlasX", "GeoJSON import failed", e);
                kind = GeoJson.ErrorKind.OTHER;
                error = e.getMessage();
            }

            final String finalError = error;
            final int finalImported = imported;
            final GeoJson.ErrorKind finalKind = kind;
            AppExecutors.runOnMain(() -> {
                if (finalKind == GeoJson.ErrorKind.MALFORMED) {
                    Ui.toast(activity, R.string.geojson_import_invalid);
                } else if (finalKind == GeoJson.ErrorKind.NO_POINTS) {
                    Ui.toast(activity, R.string.geojson_import_empty);
                } else if (finalKind != GeoJson.ErrorKind.NONE) {
                    Ui.toast(activity, activity.getString(
                            R.string.geojson_error,
                            finalError == null ? "" : finalError));
                } else {
                    Ui.toast(activity,
                            activity.getResources().getQuantityString(R.plurals.geojson_import_success,
                                    finalImported, finalImported));
                    Notifier.notifyFileOperation(
                            activity,
                            activity.getString(R.string.notif_import_title),
                            activity.getResources().getQuantityString(R.plurals.notif_import_text,
                                    finalImported, finalImported));
                }
                if (callback != null) {
                    callback.onGeoJsonFinished();
                }
            });
        });
    }

    // --------------------------------------------------------------- export

    private void exportTo(Uri uri) {
        Ui.toast(activity, R.string.loading);
        AppExecutors.runOnBackground(() -> {
            String error = null;
            int written = 0;
            try {
                List<SavedPoint> points =
                        AtlasDatabase.getInstance(activity).getPoints();
                String json = GeoJson.write(points, "Atlas X points");
                try (OutputStream output =
                             activity.getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) {
                        throw new IllegalStateException("cannot open output stream");
                    }
                    output.write(json.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                }
                written = points.size();
                new SettingsRepository(activity).increaseExportCount();
            } catch (Exception e) {
                android.util.Log.e("AtlasX", "GeoJSON export failed", e);
                error = e.getMessage();
            }

            final String finalError = error;
            final int finalWritten = written;
            AppExecutors.runOnMain(() -> {
                if (finalError != null) {
                    Ui.toast(activity,
                            activity.getString(R.string.geojson_error, finalError));
                } else {
                    Ui.toast(activity, R.string.geojson_export_success);
                    Notifier.notifyFileOperation(
                            activity,
                            activity.getString(R.string.notif_export_title),
                            activity.getResources().getQuantityString(R.plurals.notif_export_text,
                                    finalWritten, finalWritten));
                }
                if (callback != null) {
                    callback.onGeoJsonFinished();
                }
            });
        });
    }

    private String readText(Uri uri) throws Exception {
        try (InputStream input = activity.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (input == null) {
                throw new IllegalStateException("cannot open input stream");
            }
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
