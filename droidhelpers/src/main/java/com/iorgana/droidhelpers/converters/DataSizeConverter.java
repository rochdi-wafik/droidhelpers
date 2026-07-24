package com.iorgana.droidhelpers.converters;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataSizeConverter {
    private static final long KILOBYTE = 1024;
    private static final long MEGABYTE = KILOBYTE * 1024;
    private static final long GIGABYTE = MEGABYTE * 1024;

    // Instance fields for isolated tracking (prevents cross-screen corruption)
    private double lastMaxUpload = 0;
    private double lastMaxDownload = 0;
    private double lastMaxTotal = 0;
    private long lLastMaxUpload = 0;
    private long lLastMaxDownload = 0;
    private long lLastMaxTotal = 0;

    /**
     *--------------------------------------------------------------
     * Reset Max Speeds
     * --------------------------------------------------------------
     * Resets all tracked maximum speed values for this instance.
     * Call this when a new session or screen starts to prevent
     * carrying over old max values.
     */
    public void resetMaxSpeeds() {
        this.lastMaxUpload = 0;
        this.lastMaxDownload = 0;
        this.lastMaxTotal = 0;
        this.lLastMaxUpload = 0;
        this.lLastMaxDownload = 0;
        this.lLastMaxTotal = 0;
    }

    /**
     *--------------------------------------------------------------
     * Get Top Upload Speed
     * --------------------------------------------------------------
     * Updates and returns the maximum upload speed recorded for this instance.
     * @param currentBytes The current upload speed in bytes.
     * @return The maximum upload speed recorded so far (double).
     */
    public double getTopUploadSpeed(Double currentBytes) {
        if (currentBytes != null && currentBytes > lastMaxUpload) {
            lastMaxUpload = currentBytes;
        }
        return lastMaxUpload;
    }

    /**
     *--------------------------------------------------------------
     * Get Top Upload Speed
     * --------------------------------------------------------------
     * Updates and returns the maximum upload speed recorded for this instance.
     * @param currentBytes The current upload speed in bytes.
     * @return The maximum upload speed recorded so far (long).
     */
    public long getTopUploadSpeed(Long currentBytes) {
        if (currentBytes != null && currentBytes > lLastMaxUpload) {
            lLastMaxUpload = currentBytes;
        }
        return lLastMaxUpload;
    }

    /**
     *--------------------------------------------------------------
     * Get Top Download Speed
     * --------------------------------------------------------------
     * Updates and returns the maximum download speed recorded for this instance.
     * @param currentBytes The current download speed in bytes.
     * @return The maximum download speed recorded so far (double).
     */
    public double getTopDownloadSpeed(Double currentBytes) {
        if (currentBytes != null && currentBytes > lastMaxDownload) {
            lastMaxDownload = currentBytes;
        }
        return lastMaxDownload;
    }

    /**
     *--------------------------------------------------------------
     * Get Top Download Speed
     * --------------------------------------------------------------
     * Updates and returns the maximum download speed recorded for this instance.
     * @param currentBytes The current download speed in bytes.
     * @return The maximum download speed recorded so far (long).
     */
    public long getTopDownloadSpeed(Long currentBytes) {
        if (currentBytes != null && currentBytes > lLastMaxDownload) {
            lLastMaxDownload = currentBytes;
        }
        return lLastMaxDownload;
    }

    /**
     *--------------------------------------------------------------
     * Get Top Total Speed
     * --------------------------------------------------------------
     * Updates and returns the maximum total speed (upload + download)
     * recorded for this instance.
     * @param currentByteUpload Current upload speed in bytes.
     * @param currentByteDownload Current download speed in bytes.
     * @return The maximum total speed recorded so far.
     */
    public double getTopTotalSpeed(long currentByteUpload, long currentByteDownload) {
        long currentBytes = currentByteUpload + currentByteDownload; // Fixed: Total is sum, not max
        if (currentBytes > lLastMaxTotal) {
            lLastMaxTotal = currentBytes;
        }
        return lLastMaxTotal;
    }

    /**
     *--------------------------------------------------------------
     * Bytes To Human Readable
     * --------------------------------------------------------------
     * Evaluates bytes to a human-readable string size (e.g., KB, MB, GB).
     * @param bytes The size in bytes (double).
     * @return Formatted size string.
     */
    public static String byteToString(Double bytes) {
        if (bytes == null) return "0 B";

        if (bytes < KILOBYTE) {
            return bytes + " B";
        } else if (bytes < MEGABYTE) {
            return String.format(Locale.ENGLISH, "%.1f KB", bytes / KILOBYTE);
        } else if (bytes < GIGABYTE) {
            return String.format(Locale.ENGLISH, "%.1f MB", bytes / MEGABYTE);
        } else {
            return String.format(Locale.ENGLISH, "%.1f GB", bytes / GIGABYTE);
        }
    }

    /**
     *--------------------------------------------------------------
     * Bytes To Human Readable
     * --------------------------------------------------------------
     * Evaluates bytes to a human-readable string size (e.g., KB, MB, GB).
     * @param bytes The size in bytes (long).
     * @return Formatted size string.
     */
    public static String byteToString(Long bytes) {
        if (bytes == null) return "0 B";

        if (bytes < KILOBYTE) {
            return bytes + " B";
        } else if (bytes < MEGABYTE) {
            return String.format(Locale.ENGLISH, "%.1f KB", (double) bytes / KILOBYTE);
        } else if (bytes < GIGABYTE) {
            return String.format(Locale.ENGLISH, "%.1f MB", (double) bytes / MEGABYTE);
        } else {
            return String.format(Locale.ENGLISH, "%.1f GB", (double) bytes / GIGABYTE);
        }
    }

    /*########################### [ Data Formatting ] ###########################*/

    /**
     *--------------------------------------------------------------
     * Short Double
     * --------------------------------------------------------------
     * Truncates a double to a specified number of decimal places.
     * @param value The double value to format.
     * @param numToKeep Number of decimal places to keep after the dot.
     * @return Truncated double value.
     */
    public static double shortDouble(double value, int numToKeep) {
        double scalingFactor = Math.pow(10, numToKeep);
        return Math.floor(value * scalingFactor) / scalingFactor;
    }

    /**
     *--------------------------------------------------------------
     * Short Double With Locale
     * --------------------------------------------------------------
     * Truncates a double and formats it as a string with the given locale.
     * @param value The double value to format.
     * @param numToKeep Number of decimal places to keep after the dot.
     * @param locale The output locale (e.g., Locale.ENGLISH, Locale.ROOT).
     *               Defaults to Locale.ENGLISH if null.
     * @return Formatted string representation of the truncated double.
     */
    public static String shortDouble(double value, int numToKeep, Locale locale) {
        double scalingFactor = Math.pow(10, numToKeep);
        double truncatedValue = Math.floor(value * scalingFactor) / scalingFactor;
        Locale mLocale = (locale != null) ? locale : Locale.ENGLISH;
        return String.format(mLocale, "%." + numToKeep + "f", truncatedValue);
    }

    /**
     *--------------------------------------------------------------
     * Remove Chars After Dot
     * --------------------------------------------------------------
     * Truncates a string representation of a number after the decimal point.
     * @param input The input string (e.g., "6.33378GB").
     * @param charsToKeep Number of characters to keep after the dot.
     * @return Truncated string (e.g., "6.3GB").
     */
    public static String removeCharsAfterDot(String input, int charsToKeep) {
        Pattern pattern = Pattern.compile("(.*\\.)(.{0," + charsToKeep + "}).*");
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            return matcher.group(1) + matcher.group(2);
        }
        return input;
    }

    /*########################### [ Unit Conversions ] ###########################*/

    /**
     *--------------------------------------------------------------
     * Bytes To Gigabytes
     * --------------------------------------------------------------
     * Converts bytes to gigabytes (double).
     */
    public static double bytesToGigabytes(Double bytes) {
        return (bytes / GIGABYTE);
    }

    /**
     *--------------------------------------------------------------
     * Bytes To Gigabytes (Formatted)
     * --------------------------------------------------------------
     * Converts bytes to gigabytes. Returns a formatted string,
     * automatically removing ".0" for whole numbers to keep output clean.
     */
    public static String bytesToGigabytes(Long bytes) {
        double gb = (double) bytes / GIGABYTE;
        if (gb == (long) gb) {
            return String.valueOf((long) gb);
        }
        return String.format(Locale.ENGLISH, "%.1f", gb);
    }

    /**
     *--------------------------------------------------------------
     * Bytes To Megabytes
     * --------------------------------------------------------------
     * Converts bytes to megabytes (double).
     */
    public static double bytesToMegabytes(Double bytes) {
        return (bytes / MEGABYTE);
    }

    /**
     *--------------------------------------------------------------
     * Bytes To Megabytes (Formatted)
     * --------------------------------------------------------------
     * Converts bytes to megabytes. Returns a formatted string,
     * automatically removing ".0" for whole numbers to keep output clean.
     */
    public static String bytesToMegabytes(Long bytes) {
        double mb = (double) bytes / MEGABYTE;
        if (mb == (long) mb) {
            return String.valueOf((long) mb);
        }
        return String.format(Locale.ENGLISH, "%.1f", mb);
    }

    /**
     *--------------------------------------------------------------
     * Bytes To Kilobytes
     * --------------------------------------------------------------
     * Converts bytes to kilobytes (double).
     */
    public static double bytesToKilobytes(Double bytes) {
        return (bytes / KILOBYTE);
    }

    /**
     *--------------------------------------------------------------
     * Gigabytes To Bytes
     * --------------------------------------------------------------
     * Converts gigabytes to bytes (double).
     */
    public static double gigabytesToBytes(Double gigabytes) {
        return (gigabytes * GIGABYTE);
    }

    /**
     *--------------------------------------------------------------
     * Megabytes To Bytes
     * --------------------------------------------------------------
     * Converts megabytes to bytes (double).
     */
    public static double megabytesToBytes(Double megabytes) {
        return (megabytes * MEGABYTE);
    }

    /**
     *--------------------------------------------------------------
     * Kilobytes To Bytes
     * --------------------------------------------------------------
     * Converts kilobytes to bytes (double).
     */
    public static double kilobytesToBytes(Double kilobytes) {
        return (kilobytes * KILOBYTE);
    }
}