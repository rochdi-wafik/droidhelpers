package com.iorgana.droidhelpers.converters;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ************************************************************************
 * DataSizeConverter
 * ************************************************************************
 * - Utility methods for converting and formatting data sizes.
 * - Provides methods for tracking maximum speeds and converting between
 *   bytes, kilobytes, megabytes, and gigabytes.
 */
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
     * ************************************************************************
     * resetMaxSpeeds()
     * ************************************************************************
     * - Reset all tracked maximum speed values for this instance.
     * - Call this when starting a new session or screen.
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
     * ************************************************************************
     * getTopUploadSpeed() (Double)
     * ************************************************************************
     * - Update and return the maximum upload speed recorded (double).
     * ------------------------------------------------------------------------
     * @param currentBytes The current upload speed in bytes.
     * @return The maximum upload speed recorded so far.
     */
    public double getTopUploadSpeed(Double currentBytes) {
        if (currentBytes != null && currentBytes > lastMaxUpload) {
            lastMaxUpload = currentBytes;
        }
        return lastMaxUpload;
    }

    /**
     * ************************************************************************
     * getTopUploadSpeed() (Long)
     * ************************************************************************
     * - Update and return the maximum upload speed recorded (long).
     * ------------------------------------------------------------------------
     * @param currentBytes The current upload speed in bytes.
     * @return The maximum upload speed recorded so far.
     */
    public long getTopUploadSpeed(Long currentBytes) {
        if (currentBytes != null && currentBytes > lLastMaxUpload) {
            lLastMaxUpload = currentBytes;
        }
        return lLastMaxUpload;
    }

    /**
     * ************************************************************************
     * getTopDownloadSpeed() (Double)
     * ************************************************************************
     * - Update and return the maximum download speed recorded (double).
     * ------------------------------------------------------------------------
     * @param currentBytes The current download speed in bytes.
     * @return The maximum download speed recorded so far.
     */
    public double getTopDownloadSpeed(Double currentBytes) {
        if (currentBytes != null && currentBytes > lastMaxDownload) {
            lastMaxDownload = currentBytes;
        }
        return lastMaxDownload;
    }

    /**
     * ************************************************************************
     * getTopDownloadSpeed() (Long)
     * ************************************************************************
     * - Update and return the maximum download speed recorded (long).
     * ------------------------------------------------------------------------
     * @param currentBytes The current download speed in bytes.
     * @return The maximum download speed recorded so far.
     */
    public long getTopDownloadSpeed(Long currentBytes) {
        if (currentBytes != null && currentBytes > lLastMaxDownload) {
            lLastMaxDownload = currentBytes;
        }
        return lLastMaxDownload;
    }

    /**
     * ************************************************************************
     * getTopTotalSpeed()
     * ************************************************************************
     * - Update and return the maximum total speed (upload + download).
     * ------------------------------------------------------------------------
     * @param currentByteUpload   Current upload speed in bytes.
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
     * ************************************************************************
     * byteToString() (Double)
     * ************************************************************************
     * - Convert bytes to a human-readable string size (B, KB, MB, GB).
     * ------------------------------------------------------------------------
     * @param bytes The size in bytes (double).
     * @return Formatted size string (e.g., "1.5 MB").
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
     * ************************************************************************
     * byteToString() (Long)
     * ************************************************************************
     * - Convert bytes to a human-readable string size (B, KB, MB, GB).
     * ------------------------------------------------------------------------
     * @param bytes The size in bytes (long).
     * @return Formatted size string (e.g., "1.5 MB").
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

    /*==============================[ Data Formatting ]==============================*/

    /**
     * ************************************************************************
     * shortDouble()
     * ************************************************************************
     * - Truncate a double to a specified number of decimal places.
     * ------------------------------------------------------------------------
     * @param value     The double value to format.
     * @param numToKeep Number of decimal places to keep.
     * @return The truncated double value.
     */
    public static double shortDouble(double value, int numToKeep) {
        double scalingFactor = Math.pow(10, numToKeep);
        return Math.floor(value * scalingFactor) / scalingFactor;
    }

    /**
     * ************************************************************************
     * shortDouble() with Locale
     * ************************************************************************
     * - Truncate a double and format as a string with the given locale.
     * ------------------------------------------------------------------------
     * @param value     The double value to format.
     * @param numToKeep Number of decimal places to keep.
     * @param locale    The output locale (e.g., Locale.ENGLISH). Defaults to
     *                  ENGLISH if null.
     * @return Formatted string of the truncated double.
     */
    public static String shortDouble(double value, int numToKeep, Locale locale) {
        double scalingFactor = Math.pow(10, numToKeep);
        double truncatedValue = Math.floor(value * scalingFactor) / scalingFactor;
        Locale mLocale = (locale != null) ? locale : Locale.ENGLISH;
        return String.format(mLocale, "%." + numToKeep + "f", truncatedValue);
    }

    /**
     * ************************************************************************
     * removeCharsAfterDot()
     * ************************************************************************
     * - Truncate a string representation after the decimal point.
     * ------------------------------------------------------------------------
     * @param input       The input string (e.g., "6.33378GB").
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

    /*==============================[ Unit Conversions ]==============================*/

    /**
     * ************************************************************************
     * bytesToGigabytes() (Double)
     * ************************************************************************
     * - Convert bytes to gigabytes (double).
     * ------------------------------------------------------------------------
     * @param bytes The size in bytes.
     * @return The value in gigabytes.
     */
    public static double bytesToGigabytes(Double bytes) {
        return (bytes / GIGABYTE);
    }

    /**
     * ************************************************************************
     * bytesToGigabytes() (Formatted Long)
     * ************************************************************************
     * - Convert bytes to gigabytes and format as a string.
     * - Removes ".0" for whole numbers.
     * ------------------------------------------------------------------------
     * @param bytes The size in bytes.
     * @return Formatted gigabyte string.
     */
    public static String bytesToGigabytes(Long bytes) {
        double gb = (double) bytes / GIGABYTE;
        if (gb == (long) gb) {
            return String.valueOf((long) gb);
        }
        return String.format(Locale.ENGLISH, "%.1f", gb);
    }

    /**
     * ************************************************************************
     * bytesToMegabytes() (Double)
     * ************************************************************************
     * - Convert bytes to megabytes (double).
     * ------------------------------------------------------------------------
     * @param bytes The size in bytes.
     * @return The value in megabytes.
     */
    public static double bytesToMegabytes(Double bytes) {
        return (bytes / MEGABYTE);
    }

    /**
     * ************************************************************************
     * bytesToMegabytes() (Formatted Long)
     * ************************************************************************
     * - Convert bytes to megabytes and format as a string.
     * - Removes ".0" for whole numbers.
     * ------------------------------------------------------------------------
     * @param bytes The size in bytes.
     * @return Formatted megabyte string.
     */
    public static String bytesToMegabytes(Long bytes) {
        double mb = (double) bytes / MEGABYTE;
        if (mb == (long) mb) {
            return String.valueOf((long) mb);
        }
        return String.format(Locale.ENGLISH, "%.1f", mb);
    }

    /**
     * ************************************************************************
     * bytesToKilobytes() (Double)
     * ************************************************************************
     * - Convert bytes to kilobytes (double).
     * ------------------------------------------------------------------------
     * @param bytes The size in bytes.
     * @return The value in kilobytes.
     */
    public static double bytesToKilobytes(Double bytes) {
        return (bytes / KILOBYTE);
    }

    /**
     * ************************************************************************
     * gigabytesToBytes()
     * ************************************************************************
     * - Convert gigabytes to bytes (double).
     * ------------------------------------------------------------------------
     * @param gigabytes The value in gigabytes.
     * @return The value in bytes.
     */
    public static double gigabytesToBytes(Double gigabytes) {
        return (gigabytes * GIGABYTE);
    }

    /**
     * ************************************************************************
     * megabytesToBytes()
     * ************************************************************************
     * - Convert megabytes to bytes (double).
     * ------------------------------------------------------------------------
     * @param megabytes The value in megabytes.
     * @return The value in bytes.
     */
    public static double megabytesToBytes(Double megabytes) {
        return (megabytes * MEGABYTE);
    }

    /**
     * ************************************************************************
     * kilobytesToBytes()
     * ************************************************************************
     * - Convert kilobytes to bytes (double).
     * ------------------------------------------------------------------------
     * @param kilobytes The value in kilobytes.
     * @return The value in bytes.
     */
    public static double kilobytesToBytes(Double kilobytes) {
        return (kilobytes * KILOBYTE);
    }
}