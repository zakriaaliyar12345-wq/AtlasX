package com.atlasx.gis.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atlasx.gis.R;
import com.atlasx.gis.data.SettingsRepository;
import com.atlasx.gis.data.model.Project;
import com.atlasx.gis.data.model.SavedPoint;
import com.atlasx.gis.geo.GeoFormat;
import com.atlasx.gis.util.LocaleHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for saved geographic points.
 *
 * <p>Only the data list is held here; filtering, loading and persistence stay
 * in {@code PointsActivity}, and edit/delete events are reported back through
 * {@link Listener}.</p>
 */
public class PointAdapter extends RecyclerView.Adapter<PointAdapter.PointViewHolder> {

    /** UI callbacks for a single row. */
    public interface Listener {
        void onEdit(SavedPoint point);

        void onDelete(SavedPoint point);
    }

    private final List<SavedPoint> points = new ArrayList<>();
    private final List<Project> projects = new ArrayList<>();
    private final Listener listener;
    private final Locale locale;
    private final boolean dms;

    public PointAdapter(Context context, Listener listener) {
        this.listener = listener;
        this.locale = LocaleHelper.current(context);
        this.dms = new SettingsRepository(context).isDmsFormat();
    }

    public void submit(List<SavedPoint> newPoints, List<Project> newProjects) {
        points.clear();
        if (newPoints != null) {
            points.addAll(newPoints);
        }
        projects.clear();
        if (newProjects != null) {
            projects.addAll(newProjects);
        }
        notifyDataSetChanged();
    }

    public SavedPoint itemAt(int position) {
        return points.get(position);
    }

    @Override
    public int getItemCount() {
        return points.size();
    }

    @NonNull
    @Override
    public PointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_point, parent, false);
        return new PointViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PointViewHolder holder, int position) {
        SavedPoint point = points.get(position);

        holder.name.setText(point.getName());
        holder.coordinates.setText(holder.itemView.getContext().getString(
                R.string.coordinate_pair_format,
                GeoFormat.formatCoordinate(point.getLatitude(), true, dms, locale),
                GeoFormat.formatCoordinate(point.getLongitude(), false, dms, locale)));

        String description = point.getDescription();
        if (description == null || description.isEmpty()) {
            holder.description.setVisibility(View.GONE);
        } else {
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(description);
        }

        String projectLabel = projectLabel(point.getProjectId());
        if (projectLabel == null) {
            holder.project.setVisibility(View.GONE);
        } else {
            holder.project.setVisibility(View.VISIBLE);
            holder.project.setText(projectLabel);
        }

        holder.edit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(point);
        });
        holder.delete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(point);
        });
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(point);
        });
    }

    private String projectLabel(long projectId) {
        if (projectId <= 0) {
            return null;
        }
        for (Project project : projects) {
            if (project.getId() == projectId) {
                return project.getName();
            }
        }
        return null;
    }

    static class PointViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView coordinates;
        final TextView description;
        final TextView project;
        final ImageButton edit;
        final ImageButton delete;

        PointViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.pointName);
            coordinates = itemView.findViewById(R.id.pointCoordinates);
            description = itemView.findViewById(R.id.pointDescription);
            project = itemView.findViewById(R.id.pointProject);
            edit = itemView.findViewById(R.id.pointEdit);
            delete = itemView.findViewById(R.id.pointDelete);
        }
    }
}
