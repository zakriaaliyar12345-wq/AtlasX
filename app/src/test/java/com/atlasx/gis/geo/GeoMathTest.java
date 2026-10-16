package com.atlasx.gis.geo;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for the pure geographic calculations (course unit 10.4 style). */
public class GeoMathTest {

    private static final double EARTH_DEGREE_METERS =
            GeoMath.EARTH_RADIUS_METERS * Math.PI / 180.0;

    // ------------------------------------------------------------ validation

    @Test
    public void latitudeValidation_acceptsRangeAndRejectsRest() {
        assertTrue(GeoMath.isValidLatitude(35.6892));
        assertTrue(GeoMath.isValidLatitude(-90.0));
        assertTrue(GeoMath.isValidLatitude(90.0));
        assertFalse(GeoMath.isValidLatitude(90.1));
        assertFalse(GeoMath.isValidLatitude(-90.1));
        assertFalse(GeoMath.isValidLatitude(Double.NaN));
        assertFalse(GeoMath.isValidLatitude(Double.POSITIVE_INFINITY));
    }

    @Test
    public void longitudeValidation_acceptsRangeAndRejectsRest() {
        assertTrue(GeoMath.isValidLongitude(51.389));
        assertTrue(GeoMath.isValidLongitude(180.0));
        assertFalse(GeoMath.isValidLongitude(180.1));
        assertFalse(GeoMath.isValidLongitude(Double.NaN));
    }

    @Test
    public void coordinateValidation_requiresBothParts() {
        assertTrue(GeoMath.isValidCoordinate(35.6892, 51.389));
        assertFalse(GeoMath.isValidCoordinate(95.0, 51.389));
        assertFalse(GeoMath.isValidCoordinate(35.6892, 200.0));
        assertFalse(GeoMath.isValidCoordinate(Double.NaN, 51.389));
    }

    // -------------------------------------------------------------- distance

    @Test
    public void distance_oneDegreeOfLongitudeAtEquatorMatchesRadius() {
        double distance = GeoMath.distanceMeters(0.0, 0.0, 0.0, 1.0);
        assertEquals(EARTH_DEGREE_METERS, distance, 1.0);
    }

    @Test
    public void distance_isZeroForSamePointAndSymmetric() {
        assertEquals(0.0, GeoMath.distanceMeters(35.6892, 51.389, 35.6892, 51.389), 1e-6);
        double ab = GeoMath.distanceMeters(35.6892, 51.389, 35.75, 51.4);
        double ba = GeoMath.distanceMeters(35.75, 51.4, 35.6892, 51.389);
        assertEquals(ab, ba, 1e-6);
    }

    @Test
    public void distance_tehranToIsfahanIsRoughlyFourHundredKilometres() {
        double distance = GeoMath.distanceMeters(35.6892, 51.389, 32.6546, 51.668);
        assertTrue("expected ~340 km, got " + distance, distance > 300_000
                && distance < 380_000);
    }

    // --------------------------------------------------------------- bearing

    @Test
    public void bearing_dueNorthIsZeroAndDueEastIsNinety() {
        assertEquals(0.0, GeoMath.bearingDegrees(0.0, 0.0, 1.0, 0.0), 1e-6);
        assertEquals(90.0, GeoMath.bearingDegrees(0.0, 0.0, 0.0, 1.0), 1e-6);
        assertEquals(180.0, GeoMath.bearingDegrees(1.0, 0.0, 0.0, 0.0), 1e-6);
    }

    // -------------------------------------------------------------- polygons

    @Test
    public void area_ofOneDegreeSquareAtEquatorIsAboutTwelveThousandKm2() {
        List<double[]> square = new ArrayList<>();
        square.add(new double[]{0.0, 0.0});
        square.add(new double[]{0.0, 1.0});
        square.add(new double[]{1.0, 1.0});
        square.add(new double[]{1.0, 0.0});

        double area = GeoMath.polygonAreaSquareMeters(square);
        assertTrue("expected ~1.24e10 m2, got " + area,
                area > 1.20e10 && area < 1.28e10);
    }

    @Test
    public void area_ofFewerThanThreeVerticesIsZero() {
        List<double[]> line = new ArrayList<>();
        line.add(new double[]{0.0, 0.0});
        line.add(new double[]{1.0, 1.0});
        assertEquals(0.0, GeoMath.polygonAreaSquareMeters(line), 0.0);
        assertEquals(0.0, GeoMath.polygonAreaSquareMeters(null), 0.0);
    }

    @Test
    public void perimeterClosesTheRingWhilePathDoesNot() {
        List<double[]> square = new ArrayList<>();
        square.add(new double[]{0.0, 0.0});
        square.add(new double[]{0.0, 1.0});
        square.add(new double[]{1.0, 1.0});
        square.add(new double[]{1.0, 0.0});

        double path = GeoMath.pathLengthMeters(square);
        double perimeter = GeoMath.polygonPerimeterMeters(square);
        // One extra closing edge of one degree.
        assertEquals(perimeter - path, EARTH_DEGREE_METERS, 1000.0);
    }

    @Test
    public void pathLength_ofTwoPointsEqualsDistance() {
        List<double[]> segment = new ArrayList<>();
        segment.add(new double[]{35.6892, 51.389});
        segment.add(new double[]{35.75, 51.4});
        assertEquals(GeoMath.distanceMeters(35.6892, 51.389, 35.75, 51.4),
                GeoMath.pathLengthMeters(segment), 1e-6);
        assertEquals(0.0, GeoMath.pathLengthMeters(null), 0.0);
    }

    // --------------------------------------------------------------- parsing

    @Test
    public void parseNumber_acceptsAsciiPersianAndArabicDigits() {
        assertEquals(35.5, GeoMath.parseNumber("35.5"), 1e-9);
        assertEquals(35.5, GeoMath.parseNumber("\u06F3\u06F5\u066B\u06F5"), 1e-9);
        assertEquals(35.5, GeoMath.parseNumber("\u0663\u0665.\u0665"), 1e-9);
        assertEquals(-4.25, GeoMath.parseNumber(" -4.25 "), 1e-9);
        assertEquals(3.5, GeoMath.parseNumber("3,5"), 1e-9);
        assertEquals(3.75, GeoMath.parseNumber("3,75"), 1e-9);
    }

    @Test
    public void parseNumber_returnsNaNForUnusableInput() {
        assertTrue(Double.isNaN(GeoMath.parseNumber(null)));
        assertTrue(Double.isNaN(GeoMath.parseNumber("")));
        assertTrue(Double.isNaN(GeoMath.parseNumber("   ")));
        assertTrue(Double.isNaN(GeoMath.parseNumber("abc")));
        assertTrue(Double.isNaN(GeoMath.parseNumber("35.68.1")));
    }

    @Test
    public void normalizeDigits_convertsBothDigitScripts() {
        assertEquals("123", GeoMath.normalizeDigits("\u06F1\u06F2\u06F3"));
        assertEquals("123", GeoMath.normalizeDigits("\u0661\u0662\u0663"));
        assertEquals("a1b", GeoMath.normalizeDigits("a\u0661b"));
        assertEquals(null, GeoMath.normalizeDigits(null));
    }
}
