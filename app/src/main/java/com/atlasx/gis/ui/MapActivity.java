package com.atlasx.gis.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.graphics.drawable.Drawable;

import com.atlasx.gis.R;
import com.atlasx.gis.data.AtlasDatabase;
import com.atlasx.gis.data.SettingsRepository;
import com.atlasx.gis.data.model.SavedPoint;
import com.atlasx.gis.geo.GeoFormat;
import com.atlasx.gis.geo.GeoMath;
import com.atlasx.gis.util.LocaleHelper;
import com.atlasx.gis.util.Ui;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The map workspace: offline-capable OpenStreetMap/CARTO tiles with saved
 * point markers, long-press point creation, a distance/area measurement tool,
 * tile style selection, a coordinate readout, optional "my location" after a
 * runtime permission request and a coordinate pick mode used by the point
 * editor (activity results).
 */
public class MapActivity extends AppCompatActivity {

    /** Input extras: the editor asks the map to return a coordinate. */
    public static final String EXTRA_PICK_MODE = "com.atlasx.gis.extra.PICK_MODE";
    public static final String EXTRA_PICK_LAT = "com.atlasx.gis.extra.PICK_LAT";
    public static final String EXTRA_PICK_LON = "com.atlasx.gis.extra.PICK_LON";

    private static final int REQUEST_LOCATION_PERMISSION = 41;
    private static final GeoPoint DEFAULT_CENTER = new GeoPoint(35.6892, 51.3890);

    /** CARTO light grey basemap (English labels). */
    private static final OnlineTileSourceBase CARTO_POSITRON =
            new OnlineTileSourceBase("CARTO-Positron", 0, 19, 256, ".png",
                    new String[]{"https://basemaps.cartocdn.com/light_all/"}) {
                @Override
                public String getTileURLString(long mapTileIndex) {
                    return getBaseUrl()
                            + MapTileIndex.getZoom(mapTileIndex) + "/"
                            + MapTileIndex.getX(mapTileIndex) + "/"
                            + MapTileIndex.getY(mapTileIndex) + ".png";
                }
            };

    /** CARTO colourful basemap (English labels). */
    private static final OnlineTileSourceBase CARTO_VOYAGER =
            new OnlineTileSourceBase("CARTO-Voyager", 0, 19, 256, ".png",
                    new String[]{"https://basemaps.cartocdn.com/rastertiles/voyager/"}) {
                @Override
                public String getTileURLString(long mapTileIndex) {
                    return getBaseUrl()
                            + MapTileIndex.getZoom(mapTileIndex) + "/"
                            + MapTileIndex.getX(mapTileIndex) + "/"
                            + MapTileIndex.getY(mapTileIndex) + ".png";
                }
            };

    private MapView map;
    private SettingsRepository settings;

    private View pickHint;
    private View offlineBanner;
    private View measureBar;
    private android.widget.TextView measureText;
    private android.widget.TextView coordinateReadout;
    private View addPointButton;

    private MapEventsOverlay mapEvents;
    private Polyline measureLine;
    private Polygon measurePolygon;
    private MyLocationNewOverlay locationOverlay;

    private final List<Marker> pointMarkers = new ArrayList<>();
    private final Map<Marker, Long> markerPointIds = new HashMap<>();
    private final List<double[]> measureVertices = new ArrayList<>();

    private boolean pickMode;
    private boolean measuring;

    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateOnlineState();
        }
    };

    private final MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
        @Override
        public boolean singleTapConfirmedHelper(GeoPoint p) {
            if (pickMode) {
                finishPick(p);
                return true;
            }
            if (measuring) {
                addMeasurePoint(p);
                return true;
            }
            return false;
        }

        @Override
        public boolean longPressHelper(GeoPoint p) {
            if (pickMode) {
                finishPick(p);
                return true;
            }
            openEditor(p.getLatitude(), p.getLongitude());
            return true;
        }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.updateBaseContext(base));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        settings = new SettingsRepository(this);
        pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);

        map = findViewById(R.id.mapView);
        pickHint = findViewById(R.id.pickHint);
        offlineBanner = findViewById(R.id.offlineBanner);
        measureBar = findViewById(R.id.measureBar);
        measureText = findViewById(R.id.measureText);
        coordinateReadout = findViewById(R.id.coordinateReadout);
        addPointButton = findViewById(R.id.addPointButton);

        setupMap();
        setupToolbar();
        setupActions();

        applyTileSource(settings.getTileSource());
        restorePosition();

        BottomNav.install(this, findViewById(android.R.id.content), R.id.nav_map);
    }

    private void setupMap() {
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(
                CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);

        mapEvents = new MapEventsOverlay(this, mapEventsReceiver);
        map.getOverlays().add(mapEvents);

        map.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateReadout();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                updateReadout();
                return false;
            }
        });

        measureLine = new Polyline(map);
        measureLine.setColor(ContextCompat.getColor(this, R.color.atlas_teal));
        measureLine.setWidth(5f);

        measurePolygon = new Polygon(map);
        measurePolygon.setFillColor(Color.argb(48, 23, 130, 123));
        measurePolygon.setStrokeColor(ContextCompat.getColor(this, R.color.atlas_teal));
        measurePolygon.setStrokeWidth(3f);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (pickMode) {
            toolbar.setTitle(R.string.map_pick_title);
            toolbar.setNavigationIcon(R.drawable.ic_back);
            toolbar.setNavigationOnClickListener(v -> finish());
            pickHint.setVisibility(View.VISIBLE);
            addPointButton.setVisibility(View.GONE);
            View bottomNav = findViewById(R.id.bottomNavigation);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
        } else {
            toolbar.inflateMenu(R.menu.menu_map);
            toolbar.setOnMenuItemClickListener(this::onMenuItem);
        }
    }

    private void setupActions() {
        addPointButton.setOnClickListener(v -> {
            IGeoPoint center = map.getMapCenter();
            openEditor(center.getLatitude(), center.getLongitude());
        });
        findViewById(R.id.measureClear).setOnClickListener(v -> {
            measureVertices.clear();
            updateMeasureUi();
            refreshOverlays();
        });
    }

    private boolean onMenuItem(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_layers) {
            showLayersDialog();
            return true;
        }
        if (id == R.id.action_measure) {
            setMeasuring(!measuring);
            item.setChecked(measuring);
            return true;
        }
        if (id == R.id.action_locate) {
            locate();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------ lifecycle

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        registerReceiver(networkReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        updateOnlineState();
        loadPointMarkers();
        refreshOverlays();
        updateReadout();
    }

    @Override
    protected void onPause() {
        savePosition();
        try {
            unregisterReceiver(networkReceiver);
        } catch (IllegalArgumentException ignored) {
            // Never registered (for example when the activity is finishing early).
        }
        map.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        map.onDetach();
        super.onDestroy();
    }

    // --------------------------------------------------------------- pick mode

    private void finishPick(GeoPoint point) {
        Intent data = new Intent();
        data.putExtra(EXTRA_PICK_LAT, point.getLatitude());
        data.putExtra(EXTRA_PICK_LON, point.getLongitude());
        setResult(RESULT_OK, data);
        finish();
    }

    private void openEditor(double latitude, double longitude) {
        startActivity(PointEditorActivity.newPoint(this, latitude, longitude));
    }

    // ----------------------------------------------------------------- markers

    private void loadPointMarkers() {
        pointMarkers.clear();
        markerPointIds.clear();
        for (SavedPoint point : AtlasDatabase.getInstance(this).getPoints()) {
            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(point.getLatitude(), point.getLongitude()));
            marker.setTitle(point.getName());
            marker.setSnippet(formatPointSnippet(point));
            Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_pin);
            if (icon != null) {
                Drawable tinted = DrawableCompat.wrap(icon.mutate());
                DrawableCompat.setTint(tinted,
                        ContextCompat.getColor(this, R.color.atlas_teal));
                marker.setIcon(tinted);
            }
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setOnMarkerClickListener((m, mapView) -> {
                Long pointId = markerPointIds.get(m);
                if (pointId != null) {
                    startActivity(PointEditorActivity.editPoint(MapActivity.this, pointId));
                }
                return true;
            });
            pointMarkers.add(marker);
            markerPointIds.put(marker, point.getId());
        }
    }

    private String formatPointSnippet(SavedPoint point) {
        Locale locale = LocaleHelper.current(this);
        boolean dms = settings.isDmsFormat();
        return GeoFormat.formatCoordinate(point.getLatitude(), true, dms, locale)
                + ", "
                + GeoFormat.formatCoordinate(point.getLongitude(), false, dms, locale);
    }

    /** Rebuilds the overlay stack for the current screen mode. */
    private void refreshOverlays() {
        List<Overlay> overlays = map.getOverlays();
        overlays.clear();
        overlays.add(mapEvents);
        if (locationOverlay != null) {
            overlays.add(locationOverlay);
        }
        if (!pickMode && !measuring) {
            overlays.addAll(pointMarkers);
        }
        if (measuring) {
            if (measureVertices.size() >= 3) {
                measurePolygon.setPoints(toGeoPoints());
                overlays.add(measurePolygon);
            }
            if (!measureVertices.isEmpty()) {
                measureLine.setPoints(toGeoPoints());
                overlays.add(measureLine);
            }
        }
        map.invalidate();
    }

    private List<GeoPoint> toGeoPoints() {
        List<GeoPoint> points = new ArrayList<>();
        for (double[] vertex : measureVertices) {
            points.add(new GeoPoint(vertex[0], vertex[1]));
        }
        return points;
    }

    // --------------------------------------------------------------- measure

    private void setMeasuring(boolean on) {
        measuring = on;
        measureVertices.clear();
        measureBar.setVisibility(on ? View.VISIBLE : View.GONE);
        updateMeasureUi();
        refreshOverlays();
    }

    private void addMeasurePoint(GeoPoint point) {
        measureVertices.add(new double[]{point.getLatitude(), point.getLongitude()});
        updateMeasureUi();
        refreshOverlays();
    }

    private void updateMeasureUi() {
        int count = measureVertices.size();
        if (count == 0) {
            measureText.setText(R.string.map_measure_start);
            return;
        }
        StringBuilder builder = new StringBuilder(getResources().getQuantityString(
                R.plurals.area_vertex_count, count, count));
        if (count >= 2) {
            double path = GeoMath.pathLengthMeters(measureVertices);
            builder.append('\n').append(getString(R.string.distance_result_km,
                    GeoFormat.formatLength(path, settings.isMetric(),
                            LocaleHelper.current(this))));
        }
        if (count >= 3) {
            double area = GeoMath.polygonAreaSquareMeters(measureVertices);
            builder.append('\n').append(getString(R.string.area_result,
                    GeoFormat.formatArea(area, settings.isMetric(),
                            LocaleHelper.current(this))));
        }
        measureText.setText(builder.toString());
    }

    // --------------------------------------------------------------- layers

    private void showLayersDialog() {
        final String[] values = {
                SettingsRepository.TILE_OSM,
                SettingsRepository.TILE_POSITRON,
                SettingsRepository.TILE_VOYAGER
        };
        String current = settings.getTileSource();
        int checked = current.equals(SettingsRepository.TILE_POSITRON) ? 1
                : current.equals(SettingsRepository.TILE_VOYAGER) ? 2 : 0;

        new AlertDialog.Builder(this)
                .setTitle(R.string.cd_layers)
                .setMessage(R.string.tile_labels_note)
                .setSingleChoiceItems(new String[]{
                        getString(R.string.tile_standard),
                        getString(R.string.tile_positron),
                        getString(R.string.tile_voyager)
                }, checked, (dialog, which) -> {
                    settings.setTileSource(values[which]);
                    applyTileSource(values[which]);
                    Ui.toast(this, R.string.map_style_changed);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyTileSource(String key) {
        ITileSource source;
        if (SettingsRepository.TILE_POSITRON.equals(key)) {
            source = CARTO_POSITRON;
        } else if (SettingsRepository.TILE_VOYAGER.equals(key)) {
            source = CARTO_VOYAGER;
        } else {
            source = TileSourceFactory.DEFAULT_TILE_SOURCE;
        }
        map.setTileSource(source);
        map.invalidate();
    }

    // ------------------------------------------------------------- location

    private void locate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }
        if (locationOverlay == null) {
            locationOverlay = new MyLocationNewOverlay(
                    new GpsMyLocationProvider(this), map);
            locationOverlay.enableMyLocation();
        }
        refreshOverlays();
        GeoPoint here = locationOverlay.getMyLocation();
        if (here != null) {
            map.getController().animateTo(here);
            locationOverlay.enableFollowLocation();
        } else {
            Ui.toast(this, R.string.map_location_unavailable);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locate();
            } else {
                Ui.toast(this, R.string.map_location_permission_denied);
            }
        }
    }

    // ---------------------------------------------------------- connectivity

    private void updateOnlineState() {
        offlineBanner.setVisibility(isOnline() ? View.GONE : View.VISIBLE);
    }

    private boolean isOnline() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return true;
        }
        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(manager.getActiveNetwork());
        return capabilities != null
                && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    // ------------------------------------------------------------ readout

    private void updateReadout() {
        IGeoPoint center = map.getMapCenter();
        Locale locale = LocaleHelper.current(this);
        boolean dms = settings.isDmsFormat();
        coordinateReadout.setText(getString(R.string.coordinate_pair_format,
                GeoFormat.formatCoordinate(center.getLatitude(), true, dms, locale),
                GeoFormat.formatCoordinate(center.getLongitude(), false, dms, locale)));
    }

    // ------------------------------------------------------------ position

    private void savePosition() {
        IGeoPoint center = map.getMapCenter();
        settings.saveMapPosition(center.getLatitude(), center.getLongitude(),
                map.getZoomLevelDouble());
    }

    private void restorePosition() {
        float[] position = settings.loadMapPosition();
        map.getController().setCenter(
                position[0] == 0f && position[1] == 0f
                        ? DEFAULT_CENTER
                        : new GeoPoint(position[0], position[1]));
        map.getController().setZoom(position[2] > 3f ? (double) position[2] : 15.0);
    }
}
