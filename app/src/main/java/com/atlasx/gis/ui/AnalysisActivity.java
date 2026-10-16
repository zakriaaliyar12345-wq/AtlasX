package com.atlasx.gis.ui;

import android.content.Context;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.atlasx.gis.R;
import com.atlasx.gis.data.SettingsRepository;
import com.atlasx.gis.geo.GeoFormat;
import com.atlasx.gis.geo.GeoMath;
import com.atlasx.gis.util.LocaleHelper;
import com.atlasx.gis.util.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Three offline survey tools: point-to-point geodesic distance, polygon area
 * and perimeter from a vertex list, and decimal/DMS coordinate conversion.
 * No network access is required — everything is computed locally.
 */
public class AnalysisActivity extends AppCompatActivity {

    private EditText latitudeA;
    private EditText longitudeA;
    private EditText latitudeB;
    private EditText longitudeB;
    private TextView distanceResult;

    private final List<double[]> vertices = new ArrayList<>();
    private EditText vertexLatitude;
    private EditText vertexLongitude;
    private TextView verticesCount;
    private TextView verticesText;
    private TextView areaResult;
    private TextView perimeterResult;

    private EditText converterInput;
    private TextView converterResult;

    private SettingsRepository settings;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.updateBaseContext(base));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        settings = new SettingsRepository(this);

        latitudeA = findViewById(R.id.latitudeA);
        longitudeA = findViewById(R.id.longitudeA);
        latitudeB = findViewById(R.id.latitudeB);
        longitudeB = findViewById(R.id.longitudeB);
        distanceResult = findViewById(R.id.distanceResult);

        vertexLatitude = findViewById(R.id.vertexLatitude);
        vertexLongitude = findViewById(R.id.vertexLongitude);
        verticesCount = findViewById(R.id.verticesCount);
        verticesText = findViewById(R.id.verticesText);
        areaResult = findViewById(R.id.areaResult);
        perimeterResult = findViewById(R.id.perimeterResult);

        converterInput = findViewById(R.id.converterInput);
        converterResult = findViewById(R.id.converterResult);
        RadioGroup converterAxis = findViewById(R.id.converterAxis);

        findViewById(R.id.calculateDistance).setOnClickListener(v -> calculateDistance());
        findViewById(R.id.addVertex).setOnClickListener(v -> addVertex());
        findViewById(R.id.clearVertices).setOnClickListener(v -> clearVertices());
        findViewById(R.id.convertButton).setOnClickListener(v -> convert(converterAxis));

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        BottomNav.install(this, findViewById(android.R.id.content), R.id.nav_analysis);
        updateVerticesUi();
    }

    // ---------------------------------------------------------------- distance

    private void calculateDistance() {
        double latA = GeoMath.parseNumber(latitudeA.getText().toString());
        double lonA = GeoMath.parseNumber(longitudeA.getText().toString());
        double latB = GeoMath.parseNumber(latitudeB.getText().toString());
        double lonB = GeoMath.parseNumber(longitudeB.getText().toString());

        if (!GeoMath.isValidCoordinate(latA, lonA) || !GeoMath.isValidCoordinate(latB, lonB)) {
            Ui.toast(this, R.string.err_coordinates_invalid);
            return;
        }

        double meters = GeoMath.distanceMeters(latA, lonA, latB, lonB);
        boolean metric = settings.isMetric();
        distanceResult.setText(getString(R.string.distance_result_km,
                GeoFormat.formatLength(meters, metric, LocaleHelper.current(this))));
    }

    // -------------------------------------------------------------------- area

    private void addVertex() {
        double lat = GeoMath.parseNumber(vertexLatitude.getText().toString());
        double lon = GeoMath.parseNumber(vertexLongitude.getText().toString());
        if (!GeoMath.isValidCoordinate(lat, lon)) {
            Ui.toast(this, R.string.err_coordinates_invalid);
            return;
        }
        vertices.add(new double[]{lat, lon});
        vertexLatitude.setText("");
        vertexLongitude.setText("");
        vertexLatitude.requestFocus();
        updateVerticesUi();
        computeArea();
    }

    private void clearVertices() {
        vertices.clear();
        updateVerticesUi();
        computeArea();
    }

    private void updateVerticesUi() {
        verticesCount.setText(getResources().getQuantityString(
                R.plurals.area_vertex_count, vertices.size(), vertices.size()));
        if (vertices.isEmpty()) {
            verticesText.setVisibility(android.view.View.GONE);
            return;
        }
        verticesText.setVisibility(android.view.View.VISIBLE);
        Locale locale = LocaleHelper.current(this);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vertices.size(); i++) {
            double[] v = vertices.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(GeoFormat.formatCoordinate(v[0], true, settings.isDmsFormat(), locale))
                    .append(", ")
                    .append(GeoFormat.formatCoordinate(v[1], false, settings.isDmsFormat(), locale))
                    .append('\n');
        }
        verticesText.setText(builder.toString().trim());
    }

    private void computeArea() {
        if (vertices.size() < 3) {
            areaResult.setText(R.string.area_placeholder);
            perimeterResult.setText(R.string.area_needs_three);
            return;
        }
        double area = GeoMath.polygonAreaSquareMeters(vertices);
        double perimeter = GeoMath.polygonPerimeterMeters(vertices);
        boolean metric = settings.isMetric();
        Locale locale = LocaleHelper.current(this);
        areaResult.setText(getString(R.string.area_result,
                GeoFormat.formatArea(area, metric, locale)));
        perimeterResult.setText(getString(R.string.perimeter_result,
                GeoFormat.formatLength(perimeter, metric, locale)));
    }

    // --------------------------------------------------------------- converter

    private void convert(RadioGroup axisGroup) {
        boolean isLatitude = axisGroup.getCheckedRadioButtonId() == R.id.axisLatitude;
        String raw = converterInput.getText().toString().trim();
        if (raw.isEmpty()) {
            Ui.toast(this, R.string.err_input_required);
            return;
        }

        double value = GeoFormat.parseCoordinate(raw, isLatitude);
        if (Double.isNaN(value)) {
            converterResult.setText(R.string.err_dms_format);
            return;
        }

        Locale locale = LocaleHelper.current(this);
        String decimal = GeoFormat.formatNumber(value, 6, locale);
        String dms = GeoFormat.toDms(value, isLatitude, locale);
        converterResult.setText(getString(R.string.converter_result_lines,
                getString(R.string.converter_result, decimal),
                getString(R.string.converter_result, dms)));
    }
}
