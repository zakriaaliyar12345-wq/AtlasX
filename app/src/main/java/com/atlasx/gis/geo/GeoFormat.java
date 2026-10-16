package com.atlasx.gis.geo;

import java.util.Locale;

/**
 * Formats geographic values for display.
 *
 * <p>Values are returned without any label so the caller can combine them with
 * a translated string resource; numbers themselves follow the active locale so
 * a Persian user sees Persian digits.</p>
 */
public final class GeoFormat {

    private GeoFormat() {
    }

    /** Formats a length using the requested unit system. */
    public static String formatLength(double meters, boolean metric, Locale locale) {
        if (metric) {
            if (Math.abs(meters) >= 1000.0) {
                return String.format(locale, "%.3f km", meters / 1000.0);
            }
            return String.format(locale, "%.1f m", meters);
        }
        double miles = meters / 1609.344;
        if (Math.abs(miles) >= 0.1) {
            return String.format(locale, "%.3f mi", miles);
        }
        return String.format(locale, "%.1f ft", meters * 3.280839895);
    }

    /** Formats an area using the requested unit system. */
    public static String formatArea(double squareMeters, boolean metric, Locale locale) {
        if (metric) {
            if (Math.abs(squareMeters) >= 1_000_000.0) {
                return String.format(locale, "%.3f km²", squareMeters / 1_000_000.0);
            }
            if (Math.abs(squareMeters) >= 10_000.0) {
                return String.format(locale, "%.3f ha", squareMeters / 10_000.0);
            }
            return String.format(locale, "%.1f m²", squareMeters);
        }
        double squareFeet = squareMeters * 10.763910417;
        double squareMiles = squareMeters / 2_589_988.110336;
        if (Math.abs(squareMiles) >= 0.1) {
            return String.format(locale, "%.3f mi²", squareMiles);
        }
        return String.format(locale, "%.1f ft²", squareFeet);
    }

    /** Formats a single number with the given precision for the locale. */
    public static String formatNumber(double value, int decimals, Locale locale) {
        return String.format(locale, "%." + decimals + "f", value);
    }

    /**
     * Formats a latitude or longitude either as decimal degrees or as degrees,
     * minutes and seconds (for example {@code 35°41'21.12" N}).
     */
    public static String formatCoordinate(double value, boolean latitude,
                                          boolean dms, Locale locale) {
        if (dms) {
            return toDms(value, latitude, locale);
        }
        return formatNumber(value, 6, locale);
    }

    /** Converts a signed decimal degree value to {@code 35°41'21.12" N}. */
    public static String toDms(double value, boolean latitude, Locale locale) {
        String direction;
        double absolute;
        if (latitude) {
            direction = value >= 0 ? "N" : "S";
            absolute = Math.abs(value);
        } else {
            direction = value >= 0 ? "E" : "W";
            absolute = Math.abs(value);
        }
        int degrees = (int) absolute;
        double remaining = (absolute - degrees) * 60.0;
        int minutes = (int) remaining;
        double seconds = (remaining - minutes) * 60.0;
        return String.format(locale, "%d°%d'%.2f\" %s",
                degrees, minutes, seconds, direction);
    }

    /**
     * Parses either a decimal value or a DMS value such as
     * {@code 35°41'21.12" N} / {@code 35 41 21.12 N}.
     *
     * @return the signed decimal degrees, or {@link Double#NaN} when the input
     * cannot be understood.
     */
    public static double parseCoordinate(String raw, boolean latitude) {
        if (raw == null) {
            return Double.NaN;
        }
        String text = GeoMath.normalizeDigits(raw).trim();
        if (text.isEmpty()) {
            return Double.NaN;
        }

        // Direction letters only count when they stand alone (so that 1e-5,
        // or a letter inside a word, is not mistaken for a direction).
        boolean negativeDirection = latitude
                ? hasDirection(text, "Ss")
                : hasDirection(text, "Ww");
        boolean positiveDirection = latitude
                ? hasDirection(text, "Nn")
                : hasDirection(text, "Ee");

        String cleaned = text
                .replaceAll("(?<![A-Za-z])[NSEWnsew](?![A-Za-z])", " ")
                .replaceAll("[\\p{L}\\p{M}&&[^A-Za-z]]", " ")
                .replace("\u200c", " ")
                .replace("\u066C", "")
                .replace("\u066B", ".")
                .replace('\u00B0', ' ')
                .replace('\'', ' ')
                .replace('\u2032', ' ')
                .replace('\"', ' ')
                .replace('\u2033', ' ')
                .trim()
                .replaceAll("\\s+", " ");

        if (cleaned.isEmpty()) {
            return Double.NaN;
        }

        String[] parts = cleaned.split(" ");
        double degrees;
        double minutes = 0.0;
        double seconds = 0.0;
        try {
            degrees = Double.parseDouble(parts[0]);
            if (parts.length > 1) {
                minutes = Double.parseDouble(parts[1]);
            }
            if (parts.length > 2) {
                seconds = Double.parseDouble(parts[2]);
            }
        } catch (NumberFormatException e) {
            return Double.NaN;
        }

        if (minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) {
            return Double.NaN;
        }

        double decimal = Math.abs(degrees) + minutes / 60.0 + seconds / 3600.0;
        if ((degrees < 0 || negativeDirection) && !positiveDirection) {
            decimal = -decimal;
        }

        return latitude
                ? (GeoMath.isValidLatitude(decimal) ? decimal : Double.NaN)
                : (GeoMath.isValidLongitude(decimal) ? decimal : Double.NaN);
    }

    private static boolean hasDirection(String text, String letters) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (letters.indexOf(c) < 0) {
                continue;
            }
            boolean before = i > 0 && Character.isLetter(text.charAt(i - 1));
            boolean after = i < text.length() - 1
                    && Character.isLetter(text.charAt(i + 1));
            if (!before && !after) {
                return true;
            }
        }
        return false;
    }
}
