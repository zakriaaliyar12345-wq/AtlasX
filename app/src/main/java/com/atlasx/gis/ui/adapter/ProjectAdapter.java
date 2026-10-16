package com.atlasx.gis.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atlasx.gis.R;
import com.atlasx.gis.data.model.Project;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** RecyclerView adapter for projects: name, creation date and point count. */
public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    /** UI callbacks for a single project row. */
    public interface Listener {
        void onRename(Project project);

        void onDelete(Project project);
    }

    private final List<Project> projects = new ArrayList<>();
    private final Listener listener;

    public ProjectAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Project> newProjects) {
        projects.clear();
        if (newProjects != null) {
            projects.addAll(newProjects);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        holder.name.setText(project.getName());

        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                DateFormat.SHORT);
        holder.meta.setText(holder.itemView.getContext().getString(
                R.string.project_meta_format,
                holder.itemView.getContext().getResources().getQuantityString(
                        R.plurals.points_count, project.getPointCount(),
                        project.getPointCount()),
                format.format(new Date(project.getCreatedAt()))));

        holder.edit.setOnClickListener(v -> {
            if (listener != null) listener.onRename(project);
        });
        holder.delete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(project);
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRename(project);
        });
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView meta;
        final ImageButton edit;
        final ImageButton delete;

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.projectName);
            meta = itemView.findViewById(R.id.projectMeta);
            edit = itemView.findViewById(R.id.projectEdit);
            delete = itemView.findViewById(R.id.projectDelete);
        }
    }
}
