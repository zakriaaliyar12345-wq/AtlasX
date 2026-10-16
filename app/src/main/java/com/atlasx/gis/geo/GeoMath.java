package com.atlasx.gis.geo;

import java.util.List;

/**
 * Pure geographic calculations. No Android dependencies, so every method can
 * be verified with plain JUnit tests on the development machine.
 *
 * <p>All angular inputs are decimal degrees on the WGS 84 datum and all
 * outputs are metres, square metres or degrees unless stated otherwise.</p>
 */
public final class GeoMath {

    /** Mean Earth radius in metres (IUGG mean radius, used by haversine). */
    public static final double EARTH_RADIUS_METERS = 6371008.8;

    private GeoMath() {
    }

    // ------------------------------------------------------------ validation

    public static boolean isValidLatitude(double latitude) {
        return isFinite(latitude) && latitude >= -90.0 && latitude <= 90.0;
    }

    public static boolean isValidLongitude(double longitude) {
        return isFinite(longitude) && longitude >= -180.0 && longitude <= 180.0;
    }

    public static boolean isValidCoordinate(double latitude, double longitude) {
        return isValidLatitude(latitude) && isValidLongitude(longitude);
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    // -------------------------------------------------------------- distance

    /**
     * Great-circle (haversine) distance between two coordinates in metres.
     * Accurate to well under 0.5% for the distances a field survey covers.
     */
    public static double distanceMeters(double lat1, double lon1,
                                        double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);

        double sinHalfPhi = Math.sin(dPhi / 2);
        double sinHalfLambda = Math.sin(dLambda / 2);
        double a = sinHalfPhi * sinHalfPhi
                + Math.cos(phi1) * Math.cos(phi2) * sinHalfLambda * sinHalfLambda;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1.0 - a)));
        return EARTH_RADIUS_METERS * c;
    }

    /** Initial bearing from point A to point B, in degrees (0..360). */
    public static double bearingDegrees(double lat1, double lon1,
                                        double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLambda = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLambda);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360.0) % 360.0;
    }

    // -------------------------------------------------------- polygon maths

    /**
     * Polygon area on the sphere using the Chamberlain–Duquette spherical
     * excess formula: {@code |Σ (λ₂−λ₁)(2 + sin φ₁ + sin φ₂)| · R² / 2}.
     *
     * @param vertices ordered vertices as {latitude, longitude}; the ring is
     *                 closed automatically, so the first vertex must not be
     *                 repeated at the end.
     */
    public static double polygonAreaSquareMeters(List<double[]> vertices) {
        if (vertices == null || vertices.size() < 3) {
            return 0.0;
        }
        double total = 0.0;
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            double[] current = vertices.get(i);
            double[] next = vertices.get((i + 1) % n);
            double phi1 = Math.toRadians(current[0]);
            double phi2 = Math.toRadians(next[0]);
            double lambda1 = Math.toRadians(current[1]);
            double lambda2 = Math.toRadians(next[1]);
            total += (lambda2 - lambda1) * (2.0 + Math.sin(phi1) + Math.sin(phi2));
        }
        double area = Math.abs(total) * EARTH_RADIUS_METERS * EARTH_RADIUS_METERS / 2.0;
        return area;
    }

    /** Total length of the ring in metres (the closing edge is included). */
    public static double polygonPerimeterMeters(List<double[]> vertices) {
        if (vertices == null || vertices.size() < 2) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 0; i < vertices.size(); i++) {
            double[] a = vertices.get(i);
            double[] b = vertices.get((i + 1) % vertices.size());
            total += distanceMeters(a[0], a[1], b[0], b[1]);
        }
        return total;
    }

    /** Length of an open polyline in metres (no closing edge). */
    public static double pathLengthMeters(List<double[]> vertices) {
        if (vertices == null || vertices.size() < 2) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 1; i < vertices.size(); i++) {
            double[] a = vertices.get(i - 1);
            double[] b = vertices.get(i);
            total += distanceMeters(a[0], a[1], b[0], b[1]);
        }
        return total;
    }

    // -------------------------------------------------------------- parsing

    /**
     * Parses a user supplied number, accepting Persian (۰-۹), Arabic-Indic
     * (٠-٩) and ASCII digits so typed values work in both languages.
     *
     * @return the parsed value or {@link Double#NaN} when it is not a number.
     */
    public static double parseNumber(String raw) {
        if (raw == null) {
            return Double.NaN;
        }
        String text = normalizeDigits(raw).trim();
        if (text.isEmpty()) {
            return Double.NaN;
        }
        // Tolerate Persian/Arabic thousands separators and Arabic decimal mark.
        text = text.replace("\u066C", "").replace("\u200c", "")
                .replace("\u066B", ".").replace(',', '.');
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** Converts Persian and Arabic-Indic digits to ASCII digits. */
    public static String normalizeDigits(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '\u06F0' && c <= '\u06F9') {
                out.append((char) ('0' + (c - '\u06F0')));      // ۰-۹
            } else if (c >= '\u0660' && c <= '\u0669') {
                out.append((char) ('0' + (c - '\u0660')));      // ٠-٩
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
