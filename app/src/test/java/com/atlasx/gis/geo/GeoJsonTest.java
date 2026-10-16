package com.atlasx.gis.geo;

import com.atlasx.gis.data.model.SavedPoint;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for the GeoJSON reader and writer (RFC 7946 point features). */
public class GeoJsonTest {

    private static SavedPoint point(String name, double latitude, double longitude) {
        SavedPoint point = new SavedPoint();
        point.setName(name);
        point.setLatitude(latitude);
        point.setLongitude(longitude);
        point.setDescription("from test");
        return point;
    }

    @Test
    public void roundTrip_preservesNameCoordinatesAndOrder() throws Exception {
        SavedPoint original = point("Tehran office", 35.6892, 51.389);

        String json = GeoJson.write(Collections.singletonList(original), "Atlas X points");
        GeoJson.ImportResult result = GeoJson.parse(json);

        assertTrue(result.isSuccess());
        assertEquals(1, result.points.size());
        SavedPoint parsed = result.points.get(0);
        assertEquals("Tehran office", parsed.getName());
        assertEquals(35.6892, parsed.getLatitude(), 1e-9);
        assertEquals(51.389, parsed.getLongitude(), 1e-9);
        assertEquals("from test", parsed.getDescription());
    }

    @Test
    public void write_putsLongitudeBeforeLatitude() throws Exception {
        String json = GeoJson.write(Collections.singletonList(point("P", 35.0, 51.0)), null);
        org.json.JSONArray coordinates = new JSONObject(json)
                .getJSONArray("features")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates");
        assertEquals(51.0, coordinates.getDouble(0), 0.0);
        assertEquals(35.0, coordinates.getDouble(1), 0.0);
    }

    @Test
    public void parse_rejectsBrokenJson() {
        GeoJson.ImportResult broken = GeoJson.parse("{not json");
        assertFalse(broken.isSuccess());
        assertEquals(GeoJson.ErrorKind.MALFORMED, broken.kind);

        GeoJson.ImportResult empty = GeoJson.parse("   ");
        assertFalse(empty.isSuccess());
        assertEquals(GeoJson.ErrorKind.MALFORMED, empty.kind);

        GeoJson.ImportResult missing = GeoJson.parse("{\"type\":\"FeatureCollection\"}");
        assertFalse(missing.isSuccess());
        assertEquals(GeoJson.ErrorKind.MALFORMED, missing.kind);
    }

    @Test
    public void parse_reportsDocumentWithoutPoints() {
        String lines = "{\"type\":\"FeatureCollection\",\"features\":[{"
                + "\"type\":\"Feature\","
                + "\"geometry\":{\"type\":\"LineString\","
                + "\"coordinates\":[[51.0,35.0],[51.1,35.1]]},"
                + "\"properties\":{}}]}";

        GeoJson.ImportResult result = GeoJson.parse(lines);
        assertFalse(result.isSuccess());
        assertEquals(GeoJson.ErrorKind.NO_POINTS, result.kind);
    }

    @Test
    public void parse_skipsDamagedFeaturesButImportsTheRest() {
        String mixed = "{\"type\":\"FeatureCollection\",\"features\":["
                + "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\","
                + "\"coordinates\":[51.389,35.6892]},"
                + "\"properties\":{\"name\":\"good\"}},"
                + "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\","
                + "\"coordinates\":[51.0,95.0]},"
                + "\"properties\":{\"name\":\"impossible\"}},"
                + "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\","
                + "\"coordinates\":[51.668,32.6546]},"
                + "\"properties\":{\"name\":\"second good\"}}]}";

        GeoJson.ImportResult result = GeoJson.parse(mixed);
        assertTrue(result.isSuccess());
        assertEquals(2, result.points.size());
        assertEquals(1, result.skipped);
        assertEquals("good", result.points.get(0).getName());
    }

    @Test
    public void parse_acceptsSinglePointGeometryDocument() {
        String single = "{\"type\":\"Point\",\"coordinates\":[51.389,35.6892]}";
        GeoJson.ImportResult result = GeoJson.parse(single);
        assertTrue(result.isSuccess());
        assertEquals(1, result.points.size());
        assertEquals(35.6892, result.points.get(0).getLatitude(), 1e-9);
    }

    @Test
    public void parse_rejectsNullAndUnsupportedTypes() {
        assertEquals(GeoJson.ErrorKind.MALFORMED, GeoJson.parse(null).kind);
        assertEquals(GeoJson.ErrorKind.MALFORMED,
                GeoJson.parse("{\"type\":\"Polygon\",\"coordinates\":[]}").kind);
    }
}
