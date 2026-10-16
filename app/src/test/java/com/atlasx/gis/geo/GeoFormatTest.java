package com.atlasx.gis.geo;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Unit tests for coordinate parsing and display formatting. */
public class GeoFormatTest {

    private static final Locale US = Locale.US;

    // -------------------------------------------------------- decimal parsing

    @Test
    public void parseCoordinate_readsPlainDecimalValues() {
        assertEquals(35.6892, GeoFormat.parseCoordinate("35.6892", true), 1e-9);
        assertEquals(51.389, GeoFormat.parseCoordinate("51.389", false), 1e-9);
        assertEquals(-4.5, GeoFormat.parseCoordinate("-4.5", true), 1e-9);
        assertEquals(35.5, GeoFormat.parseCoordinate("\u06F3\u06F5\u066B\u06F5", true), 1e-9);
    }

    // ------------------------------------------------------------- DMS parsing

    @Test
    public void parseCoordinate_readsDmsWithDirectionLetter() {
        double expected = 35.0 + 41.0 / 60.0 + 21.12 / 3600.0;
        assertEquals(expected,
                GeoFormat.parseCoordinate("35\u00B041'21.12\" N", true), 1e-6);
        assertEquals(-expected,
                GeoFormat.parseCoordinate("35\u00B041'21.12\" S", true), 1e-6);
        assertEquals(51.0 + 23.0 / 60.0 + 20.4 / 3600.0,
                GeoFormat.parseCoordinate("51\u00B023'20.4\" E", false), 1e-6);
    }

    @Test
    public void parseCoordinate_readsSpaceSeparatedDms() {
        double expected = 35.0 + 41.0 / 60.0 + 21.12 / 3600.0;
        assertEquals(expected,
                GeoFormat.parseCoordinate("35 41 21.12 N", true), 1e-6);
    }

    @Test
    public void parseCoordinate_rejectsUnusableInput() {
        assertTrue(Double.isNaN(GeoFormat.parseCoordinate(null, true)));
        assertTrue(Double.isNaN(GeoFormat.parseCoordinate("", true)));
        assertTrue(Double.isNaN(GeoFormat.parseCoordinate("abc", true)));
        assertTrue(Double.isNaN(GeoFormat.parseCoordinate("95", true)));     // lat range
        assertTrue(Double.isNaN(GeoFormat.parseCoordinate("181", false)));   // lon range
        assertTrue(Double.isNaN(GeoFormat.parseCoordinate("35 61 0 N", true))); // minutes
        assertTrue(Double.isNaN(GeoFormat.parseCoordinate("35 0 61 N", true))); // seconds
    }

    // ------------------------------------------------------------ formatting

    @Test
    public void toDms_formatsLatitudeWithHemisphere() {
        assertEquals("35\u00B041'21.12\" N",
                GeoFormat.toDms(35.6892, true, US));
        assertEquals("51\u00B023'20.40\" E",
                GeoFormat.toDms(51.389, false, US));
    }

    @Test
    public void formatCoordinate_switchesBetweenDecimalAndDms() {
        assertEquals("35.689200",
                GeoFormat.formatCoordinate(35.6892, true, false, US));
        assertEquals("35\u00B041'21.12\" N",
                GeoFormat.formatCoordinate(35.6892, true, true, US));
    }

    @Test
    public void formatLength_usesRequestedUnitSystem() {
        assertEquals("1.500 km", GeoFormat.formatLength(1500.0, true, US));
        assertEquals("500.0 m", GeoFormat.formatLength(500.0, true, US));
        assertEquals("1.000 mi", GeoFormat.formatLength(1609.344, false, US));
        assertEquals("1.6 ft", GeoFormat.formatLength(0.5, false, US));
    }

    @Test
    public void formatArea_picksSensibleScales() {
        assertEquals("500.0 m\u00B2", GeoFormat.formatArea(500.0, true, US));
        assertEquals("2.500 ha", GeoFormat.formatArea(25_000.0, true, US));
        assertEquals("2.500 km\u00B2", GeoFormat.formatArea(2_500_000.0, true, US));
    }
}
