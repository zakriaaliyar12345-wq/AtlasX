package com.atlasx.gis.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atlasx.gis.R;
import com.atlasx.gis.data.AtlasDatabase;
import com.atlasx.gis.data.model.Project;
import com.atlasx.gis.ui.adapter.ProjectAdapter;
import com.atlasx.gis.util.LocaleHelper;
import com.atlasx.gis.util.Ui;

/**
 * Project management: create, rename and delete the projects that group a
 * field survey's points. Deleting a project keeps its points and clears their
 * project reference.
 */
public class ProjectsActivity extends AppCompatActivity implements ProjectAdapter.Listener {

    private ProjectAdapter adapter;
    private View emptyState;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.updateBaseContext(base));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        emptyState = findViewById(R.id.emptyState);
        RecyclerView list = findViewById(R.id.projectsList);
        adapter = new ProjectAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.addProjectButton).setOnClickListener(v -> showProjectDialog(null));

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        BottomNav.install(this, findViewById(android.R.id.content), R.id.nav_dashboard);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        AtlasDatabase db = AtlasDatabase.getInstance(this);
        adapter.submit(db.getProjects());
        emptyState.setVisibility(db.countProjects() == 0 ? View.VISIBLE : View.GONE);
    }

    /** Shows the create dialog, or the rename dialog when {@code project} is set. */
    private void showProjectDialog(final Project project) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint(R.string.project_name_hint);
        if (project != null) {
            input.setText(project.getName());
            input.setSelection(input.getText().length());
        }

        LinearLayout wrapper = new LinearLayout(this);
        int padding = Ui.dp(this, 22);
        wrapper.setPadding(padding, Ui.dp(this, 8), padding, 0);
        wrapper.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        final AtlasDatabase db = AtlasDatabase.getInstance(this);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(project == null
                        ? R.string.project_dialog_title
                        : R.string.rename_project)
                .setView(wrapper)
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.save, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Ui.toast(this, R.string.project_name_required);
                        return;
                    }
                    boolean duplicate = false;
                    for (Project existing : db.getProjects()) {
                        if (existing.getName().equalsIgnoreCase(name)
                                && (project == null || existing.getId() != project.getId())) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (duplicate) {
                        Ui.toast(this, R.string.project_name_exists);
                        return;
                    }

                    if (project == null) {
                        db.insertProject(name);
                        Ui.toast(this, R.string.project_saved);
                    } else {
                        db.renameProject(project.getId(), name);
                        Ui.toast(this, R.string.project_renamed);
                    }
                    reload();
                })
                .show();
    }

    @Override
    public void onRename(Project project) {
        showProjectDialog(project);
    }

    @Override
    public void onDelete(Project project) {
        Ui.confirm(
                this,
                getString(R.string.delete_project_title),
                getString(R.string.delete_project_confirm, project.getName()),
                getString(R.string.delete),
                () -> {
                    AtlasDatabase.getInstance(this).deleteProject(project.getId());
                    Ui.toast(this, R.string.project_deleted);
                    reload();
                });
    }
}
