package com.atlasx.gis.data.model;

import java.io.Serializable;

/** A named project that groups saved points. */
public class Project implements Serializable {

    private long id;
    private String name;
    private long createdAt;
    private int pointCount;

    public Project() {
    }

    public Project(long id, String name, long createdAt, int pointCount) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.pointCount = pointCount;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getPointCount() {
        return pointCount;
    }

    public void setPointCount(int pointCount) {
        this.pointCount = pointCount;
    }
}
