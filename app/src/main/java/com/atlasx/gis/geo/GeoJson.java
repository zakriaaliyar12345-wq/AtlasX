package com.atlasx.gis.geo;

import com.atlasx.gis.data.model.SavedPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * GeoJSON (RFC 7946) import and export for point features.
 *
 * <p>Parsing is deliberately strict about the document structure and lenient
 * about individual features: a malformed file is rejected as a whole, while
 * individual features with impossible coordinates are counted as skipped so a
 * partly damaged file can still be imported safely.</p>
 */
public final class GeoJson {

    /** Guard against a hostile or accidental huge file exhausting memory. */
    public static final int MAX_FEATURES = 50_000;

    /** Why an import could not be used. */
    public enum ErrorKind {
        /** No problem at all. */
        NONE,
        /** The document is not a usable GeoJSON file. */
        MALFORMED,
        /** The document parsed but contained no point feature. */
        NO_POINTS,
        /** I/O or unexpected failure; the message carries the detail. */
        OTHER
    }

    private GeoJson() {
    }

    /** Outcome of a parse attempt. */
    public static final class ImportResult {
        public final List<SavedPoint> points;
        public final int skipped;
        public final String error;
        public final ErrorKind kind;

        ImportResult(List<SavedPoint> points, int skipped, ErrorKind kind, String error) {
            this.points = points;
            this.skipped = skipped;
            this.kind = kind;
            this.error = error;
        }

        public boolean isSuccess() {
            return kind == ErrorKind.NONE;
        }
    }

    private static ImportResult failure(ErrorKind kind, String message) {
        return new ImportResult(new ArrayList<>(), 0, kind, message);
    }

    private static ImportResult success(List<SavedPoint> points, int skipped) {
        return new ImportResult(points, skipped, ErrorKind.NONE, null);
    }

    /** Parses GeoJSON text into saved points (not yet written to the database). */
    public static ImportResult parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return failure(ErrorKind.MALFORMED, "empty file");
        }

        final JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (JSONException e) {
            return failure(ErrorKind.MALFORMED, "not valid JSON: " + e.getMessage());
        }

        String type = root.optString("type", "");
        JSONArray features;
        switch (type) {
            case "FeatureCollection":
                features = root.optJSONArray("features");
                if (features == null) {
                    return failure(ErrorKind.MALFORMED, "missing \"features\" array");
                }
                break;
            case "Feature":
                features = new JSONArray();
                features.put(root);
                break;
            case "Point":
                features = new JSONArray();
                try {
                    JSONObject wrapper = new JSONObject();
                    wrapper.put("type", "Feature");
                    wrapper.put("geometry", root);
                    features.put(wrapper);
                } catch (JSONException e) {
                    return failure(ErrorKind.MALFORMED, e.getMessage());
                }
                break;
            default:
                return failure(ErrorKind.MALFORMED,
                        "unsupported GeoJSON type: " + (type.isEmpty() ? "unknown" : type));
        }

        if (features.length() > MAX_FEATURES) {
            return failure(ErrorKind.MALFORMED,
                    "more than " + MAX_FEATURES + " features");
        }

        List<SavedPoint> points = new ArrayList<>();
        int skipped = 0;
        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.optJSONObject(i);
            SavedPoint point = feature == null ? null : readFeature(feature);
            if (point == null) {
                skipped++;
            } else {
                points.add(point);
            }
        }

        if (points.isEmpty()) {
            return new ImportResult(points, skipped, ErrorKind.NO_POINTS,
                    "no valid point feature found");
        }
        return success(points, skipped);
    }

    private static SavedPoint readFeature(JSONObject feature) {
        JSONObject geometry = feature.optJSONObject("geometry");
        if (geometry == null) {
            return null;
        }
        if (!"Point".equals(geometry.optString("type"))) {
            return null; // Atlas X stores points; other geometries are skipped.
        }
        JSONArray coordinates = geometry.optJSONArray("coordinates");
        if (coordinates == null || coordinates.length() < 2) {
            return null;
        }

        // GeoJSON order is longitude, latitude.
        double lon = coordinates.optDouble(0, Double.NaN);
        double lat = coordinates.optDouble(1, Double.NaN);
        if (!GeoMath.isValidCoordinate(lat, lon)) {
            return null;
        }

        JSONObject properties = feature.optJSONObject("properties");
        String name = properties == null ? "" : properties.optString("name", "").trim();
        String description = properties == null
                ? "" : properties.optString("description", "").trim();

        SavedPoint point = new SavedPoint();
        point.setName(name);
        point.setLatitude(lat);
        point.setLongitude(lon);
        point.setDescription(description);
        return point;
    }

    /**
     * Builds a GeoJSON FeatureCollection string from points.
     *
     * @throws JSONException only for structurally impossible output; callers
     *                       surface the message to the user.
     */
    public static String write(List<SavedPoint> points, String collectionName)
            throws JSONException {
        JSONArray features = new JSONArray();
        if (points != null) {
            for (SavedPoint point : points) {
                features.put(writeFeature(point));
            }
        }
        JSONObject collection = new JSONObject();
        collection.put("type", "FeatureCollection");
        if (collectionName != null && !collectionName.isEmpty()) {
            collection.put("name", collectionName);
        }
        collection.put("features", features);
        return collection.toString(2);
    }

    private static JSONObject writeFeature(SavedPoint point) throws JSONException {
        JSONArray coordinates = new JSONArray();
        coordinates.put(point.getLongitude()); // longitude first
        coordinates.put(point.getLatitude());  // latitude second

        JSONObject geometry = new JSONObject();
        geometry.put("type", "Point");
        geometry.put("coordinates", coordinates);

        JSONObject properties = new JSONObject();
        properties.put("name", point.getName() == null ? "" : point.getName());
        properties.put("description", point.getDescription());

        JSONObject feature = new JSONObject();
        feature.put("type", "Feature");
        feature.put("geometry", geometry);
        feature.put("properties", properties);
        return feature;
    }
}
