package com.atlasx.gis.data.model;

import java.io.Serializable;

/**
 * A saved geographic point (WGS 84 decimal degrees) plus its optional project.
 *
 * <p>Implements {@link Serializable} so an edit screen can hand a point to the
 * editor activity through an intent extra.</p>
 */
public class SavedPoint implements Serializable {

    private long id;
    private String name;
    private double latitude;
    private double longitude;
    private String description;
    private long projectId;      // 0 == no project
    private long createdAt;
    private long updatedAt;

    public SavedPoint() {
    }

    public SavedPoint(long id, String name, double latitude, double longitude,
                      String description, long projectId, long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description == null ? "" : description;
        this.projectId = projectId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
