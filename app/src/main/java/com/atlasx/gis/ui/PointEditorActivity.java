package com.atlasx.gis.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.atlasx.gis.R;
import com.atlasx.gis.data.AtlasDatabase;
import com.atlasx.gis.data.model.Project;
import com.atlasx.gis.data.model.SavedPoint;
import com.atlasx.gis.geo.GeoMath;
import com.atlasx.gis.util.LocaleHelper;
import com.atlasx.gis.util.Ui;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Create or edit a single geographic point.
 *
 * <p>Demonstrates a form built from XML resources, input validation with field
 * level errors, activity results (the coordinates can be picked on the map)
 * and saving instance state across configuration changes.</p>
 */
public class PointEditorActivity extends AppCompatActivity {

    private static final String STATE_NAME = "state_name";
    private static final String STATE_LATITUDE = "state_latitude";
    private static final String STATE_LONGITUDE = "state_longitude";
    private static final String STATE_DESCRIPTION = "state_description";

    /** Optional prefill coordinates (for example from a map long press). */
    public static final String EXTRA_LATITUDE = "com.atlasx.gis.extra.LATITUDE";
    public static final String EXTRA_LONGITUDE = "com.atlasx.gis.extra.LONGITUDE";

    private TextInputLayout nameLayout;
    private TextInputLayout latitudeLayout;
    private TextInputLayout longitudeLayout;
    private EditText nameInput;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private EditText descriptionInput;
    private Spinner projectSpinner;
    private TextView pickHint;
    private View deleteButton;

    private final List<Project> projects = new ArrayList<>();
    private long pointId = 0;          // 0 == creating a new point

    private final ActivityResultLauncher<Intent> mapPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                            return;
                        }
                        double lat = result.getData()
                                .getDoubleExtra(MapActivity.EXTRA_PICK_LAT, Double.NaN);
                        double lon = result.getData()
                                .getDoubleExtra(MapActivity.EXTRA_PICK_LON, Double.NaN);
                        if (GeoMath.isValidCoordinate(lat, lon)) {
                            latitudeInput.setText(formatValue(lat));
                            longitudeInput.setText(formatValue(lon));
                            pickHint.setVisibility(View.GONE);
                            latitudeLayout.setError(null);
                            longitudeLayout.setError(null);
                        }
                    });

    public static Intent newPoint(Context context) {
        return new Intent(context, PointEditorActivity.class);
    }

    /** Creates a new point, pre-filled with coordinates chosen on the map. */
    public static Intent newPoint(Context context, double latitude, double longitude) {
        Intent intent = new Intent(context, PointEditorActivity.class);
        intent.putExtra(EXTRA_LATITUDE, latitude);
        intent.putExtra(EXTRA_LONGITUDE, longitude);
        return intent;
    }

    public static Intent editPoint(Context context, long id) {
        Intent intent = new Intent(context, PointEditorActivity.class);
        intent.putExtra(PointsActivity.EXTRA_POINT_ID, id);
        return intent;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.updateBaseContext(base));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_editor);

        nameLayout = findViewById(R.id.nameLayout);
        latitudeLayout = findViewById(R.id.latitudeLayout);
        longitudeLayout = findViewById(R.id.longitudeLayout);
        nameInput = findViewById(R.id.nameInput);
        latitudeInput = findViewById(R.id.latitudeInput);
        longitudeInput = findViewById(R.id.longitudeInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        projectSpinner = findViewById(R.id.projectSpinner);
        pickHint = findViewById(R.id.pickHint);
        deleteButton = findViewById(R.id.deleteButton);

        pointId = getIntent().getLongExtra(PointsActivity.EXTRA_POINT_ID, 0);

        installToolbar();
        installProjectSpinner();
        installActions();

        if (pointId > 0) {
            loadPoint();
        }
        if (savedInstanceState != null) {
            restoreForm(savedInstanceState);
        } else if (pointId == 0) {
            double latitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, Double.NaN);
            double longitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, Double.NaN);
            if (GeoMath.isValidCoordinate(latitude, longitude)) {
                latitudeInput.setText(formatValue(latitude));
                longitudeInput.setText(formatValue(longitude));
            }
        }
    }

    private void installToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(pointId > 0
                ? R.string.point_form_title_edit
                : R.string.point_form_title_new);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void installProjectSpinner() {
        projects.clear();
        projects.addAll(AtlasDatabase.getInstance(this).getProjects());

        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.no_project_label));
        for (Project project : projects) {
            labels.add(project.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        projectSpinner.setAdapter(adapter);
    }

    private void installActions() {
        findViewById(R.id.saveButton).setOnClickListener(v -> save());
        findViewById(R.id.pickOnMapButton).setOnClickListener(v -> {
            pickHint.setVisibility(View.VISIBLE);
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra(MapActivity.EXTRA_PICK_MODE, true);
            mapPicker.launch(intent);
        });
        deleteButton.setOnClickListener(v -> {
            Ui.confirm(
                    this,
                    getString(R.string.delete_point_title),
                    getString(R.string.delete_point_confirm, nameInput.getText()),
                    getString(R.string.delete),
                    () -> {
                        AtlasDatabase.getInstance(this).deletePoint(pointId);
                        Ui.toast(this, R.string.point_deleted);
                        setResult(RESULT_OK);
                        finish();
                    });
        });
    }

    private void loadPoint() {
        SavedPoint point = AtlasDatabase.getInstance(this).getPoint(pointId);
        if (point == null) {
            // The record disappeared between screens: fall back to creation.
            pointId = 0;
            return;
        }
        nameInput.setText(point.getName());
        latitudeInput.setText(formatValue(point.getLatitude()));
        longitudeInput.setText(formatValue(point.getLongitude()));
        descriptionInput.setText(point.getDescription());
        deleteButton.setVisibility(View.VISIBLE);

        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getId() == point.getProjectId()) {
                projectSpinner.setSelection(i + 1);
                break;
            }
        }
    }

    /** Technical input keeps Latin digits so the numeric keyboard stays usable. */
    private String formatValue(double value) {
        return String.format(java.util.Locale.US, "%.6f", value);
    }

    private void restoreForm(Bundle state) {
        String name = state.getString(STATE_NAME);
        String latitude = state.getString(STATE_LATITUDE);
        String longitude = state.getString(STATE_LONGITUDE);
        String description = state.getString(STATE_DESCRIPTION);
        if (name != null) nameInput.setText(name);
        if (latitude != null) latitudeInput.setText(latitude);
        if (longitude != null) longitudeInput.setText(longitude);
        if (description != null) descriptionInput.setText(description);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_NAME, textOf(nameInput));
        outState.putString(STATE_LATITUDE, textOf(latitudeInput));
        outState.putString(STATE_LONGITUDE, textOf(longitudeInput));
        outState.putString(STATE_DESCRIPTION, textOf(descriptionInput));
    }

    private static String textOf(EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    /** Validates the form and stores the point. */
    private void save() {
        String name = textOf(nameInput).trim();
        double latitude = GeoMath.parseNumber(textOf(latitudeInput));
        double longitude = GeoMath.parseNumber(textOf(longitudeInput));

        boolean valid = true;

        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.err_name_required));
            valid = false;
        } else {
            nameLayout.setError(null);
            nameLayout.setErrorEnabled(false);
        }

        if (Double.isNaN(latitude)) {
            latitudeLayout.setError(getString(R.string.err_coordinate_number));
            valid = false;
        } else if (!GeoMath.isValidLatitude(latitude)) {
            latitudeLayout.setError(getString(R.string.err_latitude_range));
            valid = false;
        } else {
            latitudeLayout.setError(null);
            latitudeLayout.setErrorEnabled(false);
        }

        if (Double.isNaN(longitude)) {
            longitudeLayout.setError(getString(R.string.err_coordinate_number));
            valid = false;
        } else if (!GeoMath.isValidLongitude(longitude)) {
            longitudeLayout.setError(getString(R.string.err_longitude_range));
            valid = false;
        } else {
            longitudeLayout.setError(null);
            longitudeLayout.setErrorEnabled(false);
        }

        if (!valid) {
            return;
        }

        SavedPoint point = pointId > 0
                ? AtlasDatabase.getInstance(this).getPoint(pointId)
                : new SavedPoint();
        if (point == null) {
            point = new SavedPoint();
            pointId = 0;
        }
        point.setName(name);
        point.setLatitude(latitude);
        point.setLongitude(longitude);
        point.setDescription(textOf(descriptionInput).trim());
        point.setProjectId(selectedProjectId());

        AtlasDatabase db = AtlasDatabase.getInstance(this);
        if (pointId > 0 && db.updatePoint(point)) {
            Ui.toast(this, R.string.point_updated);
        } else {
            db.insertPoint(point);
            Ui.toast(this, R.string.point_saved);
        }
        setResult(RESULT_OK);
        finish();
    }

    private long selectedProjectId() {
        int position = projectSpinner.getSelectedItemPosition();
        if (position <= 0 || position > projects.size()) {
            return 0;
        }
        return projects.get(position - 1).getId();
    }
}
