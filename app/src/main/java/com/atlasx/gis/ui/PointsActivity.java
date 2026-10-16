package com.atlasx.gis.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atlasx.gis.R;
import com.atlasx.gis.data.AtlasDatabase;
import com.atlasx.gis.data.model.Project;
import com.atlasx.gis.data.model.SavedPoint;
import com.atlasx.gis.ui.adapter.PointAdapter;
import com.atlasx.gis.util.GeoJsonTasks;
import com.atlasx.gis.util.LocaleHelper;
import com.atlasx.gis.util.Ui;

import java.util.List;
import java.util.Locale;

/**
 * Lists the saved geographic points with live search, and hosts the GeoJSON
 * import / export and share actions.
 */
public class PointsActivity extends AppCompatActivity implements PointAdapter.Listener {

    /** Optional search text supplied by the dashboard. */
    public static final String EXTRA_SEARCH = "extra_search";
    public static final String EXTRA_POINT_ID = "extra_point_id";

    private static final int MAX_SHARE_LINES = 500;

    private EditText searchInput;
    private TextView pointsCount;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private RecyclerView list;
    private PointAdapter adapter;
    private GeoJsonTasks geoJson;

    /** Used by other screens to open the import picker directly. */
    public static void startImport(Context context) {
        Intent intent = new Intent(context, PointsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(EXTRA_ACTION, ACTION_IMPORT);
        context.startActivity(intent);
    }

    /** Used by other screens to open the export flow directly. */
    public static void startExport(Context context) {
        Intent intent = new Intent(context, PointsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(EXTRA_ACTION, ACTION_EXPORT);
        context.startActivity(intent);
    }

    private static final String EXTRA_ACTION = "extra_action";
    private static final String ACTION_IMPORT = "import";
    private static final String ACTION_EXPORT = "export";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.updateBaseContext(base));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points);

        searchInput = findViewById(R.id.searchInput);
        pointsCount = findViewById(R.id.pointsCount);
        emptyState = findViewById(R.id.emptyState);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptyMessage = findViewById(R.id.emptyMessage);
        list = findViewById(R.id.pointsList);

        adapter = new PointAdapter(this, this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        geoJson = GeoJsonTasks.install(this, this::reload);

        installToolbar();
        installSearch();
        installFab();
        BottomNav.install(this, findViewById(android.R.id.content), R.id.nav_points);

        applyIncomingIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    /**
     * The screen is reused with FLAG_ACTIVITY_REORDER_TO_FRONT, so a request
     * sent to an already running instance arrives here instead of onCreate().
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyIncomingIntent();
    }

    private void applyIncomingIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        String search = intent.getStringExtra(EXTRA_SEARCH);
        if (search != null && !search.isEmpty()) {
            searchInput.setText(search);
        }
        String action = intent.getStringExtra(EXTRA_ACTION);
        if (ACTION_IMPORT.equals(action)) {
            geoJson.launchImport();
        } else if (ACTION_EXPORT.equals(action)) {
            geoJson.launchExport();
        }
        // Consume the action so a configuration change does not repeat it.
        intent.removeExtra(EXTRA_ACTION);
        intent.removeExtra(EXTRA_SEARCH);
    }

    private void installToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_import) {
                geoJson.launchImport();
                return true;
            }
            if (id == R.id.action_export) {
                geoJson.launchExport();
                return true;
            }
            if (id == R.id.action_share) {
                sharePoints();
                return true;
            }
            return false;
        });
    }

    private void installSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                reload();
            }
        });
    }

    private void installFab() {
        findViewById(R.id.addPointButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, PointEditorActivity.class);
            editorLauncher.launch(intent);
        });
    }

    private final androidx.activity.result.ActivityResultLauncher<Intent> editorLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> reload());

    private void reload() {
        String query = searchInput.getText().toString();
        AtlasDatabase db = AtlasDatabase.getInstance(this);
        List<SavedPoint> points = db.getPoints(query, null, 0);
        List<Project> projects = db.getProjects();
        adapter.submit(points, projects);

        int total = db.countPoints();
        pointsCount.setText(query.trim().isEmpty()
                ? getResources().getQuantityString(R.plurals.points_count, points.size(), points.size())
                : getString(R.string.points_count_filtered_format, points.size(),
                        getResources().getQuantityString(R.plurals.points_count, total, total)));

        if (points.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            if (total == 0) {
                emptyTitle.setText(R.string.points_empty_title);
                emptyMessage.setText(R.string.points_empty_message);
            } else {
                emptyTitle.setText(R.string.search_empty_title);
                emptyMessage.setText(R.string.search_empty_message);
            }
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    // ---------------------------------------------------- PointAdapter.Listener

    @Override
    public void onEdit(SavedPoint point) {
        Intent intent = new Intent(this, PointEditorActivity.class);
        intent.putExtra(EXTRA_POINT_ID, point.getId());
        editorLauncher.launch(intent);
    }

    @Override
    public void onDelete(SavedPoint point) {
        Ui.confirm(
                this,
                getString(R.string.delete_point_title),
                getString(R.string.delete_point_confirm, point.getName()),
                getString(R.string.delete),
                () -> {
                    AtlasDatabase.getInstance(this).deletePoint(point.getId());
                    Ui.toast(this, R.string.point_deleted);
                    reload();
                });
    }

    // ------------------------------------------------------------------ share

    /** Implicit intent that shares the current points as plain text. */
    private void sharePoints() {
        List<SavedPoint> points =
                AtlasDatabase.getInstance(this).getPoints(null, null, MAX_SHARE_LINES);
        if (points.isEmpty()) {
            Ui.toast(this, R.string.points_empty_title);
            return;
        }
        StringBuilder body = new StringBuilder();
        for (SavedPoint point : points) {
            body.append(getString(R.string.share_line_format,
                    point.getName(), point.getLatitude(), point.getLongitude()))
                    .append('\n');
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
        intent.putExtra(Intent.EXTRA_TEXT, body.toString().trim());
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }
}
