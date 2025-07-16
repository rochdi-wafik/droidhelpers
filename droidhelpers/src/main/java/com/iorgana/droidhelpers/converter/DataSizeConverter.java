package com.iorgana.droidhelpers.converter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataSizeConverter {
    private static final long KILOBYTE = 1024;
    private static final long MEGABYTE = KILOBYTE * 1024;
    private static final long GIGABYTE = MEGABYTE * 1024;

    private static double LAST_MAX_UPLOAD = 0;
    private static double LAST_MAX_DOWNLOAD = 0;

    private static double LAST_MAX_TOTAL = 0; // upload + download

    private static long L_LAST_MAX_UPLOAD = 0;
    private static long L_LAST_MAX_DOWNLOAD = 0;

    private static long L_LAST_MAX_TOTAL = 0; // upload + download
    /*#########################[Calc Usage]###############################*/

    /**
     * Get Top Upload Speed
     * -----------------------------------------------------------------------
     * If current speed > last speed : update last speed
     * @return double
     */
    public static double getTopUploadSpeed(Double currentBytes){
        if(currentBytes!=null && currentBytes > LAST_MAX_UPLOAD){
            LAST_MAX_UPLOAD = currentBytes;
        }
        return LAST_MAX_UPLOAD;
    }

    /**
     * Get Top Upload Speed
     * -----------------------------------------------------------------------
     * If current speed > last speed : update last speed
     * @return long
     */
    public static long getTopUploadSpeed(Long currentBytes){
        if(currentBytes!=null && currentBytes > L_LAST_MAX_UPLOAD){
            L_LAST_MAX_UPLOAD = currentBytes;
        }
        return L_LAST_MAX_UPLOAD;
    }

    /**
     * Get Top Download Speed (Human Readable)
     * -----------------------------------------------------------------------
     * @return double
     */
    public static double getTopDownloadSpeed(Double currentBytes){
        if(currentBytes!=null && currentBytes > LAST_MAX_DOWNLOAD){
            LAST_MAX_DOWNLOAD = currentBytes;
        }
        return LAST_MAX_DOWNLOAD;
    }

    /**
     * Get Top Download Speed (Human Readable)
     * -----------------------------------------------------------------------
     * @return long
     */
    public static long getTopDownloadSpeed(Long currentBytes){
        if(currentBytes!=null && currentBytes > L_LAST_MAX_DOWNLOAD){
            L_LAST_MAX_DOWNLOAD = currentBytes;
        }
        return L_LAST_MAX_DOWNLOAD;
    }

    /**
     * Get Top Total Speed (Upload + Download)
     * -----------------------------------------------------------------------
     */
    public static double getTopTotalSpeed(long currentByteUpload, long currentByteDownload){
        long currentBytes = Math.max(currentByteUpload, currentByteDownload);

        if(currentBytes > LAST_MAX_TOTAL){
            LAST_MAX_TOTAL = currentBytes;
        }
        return LAST_MAX_TOTAL;
    }

    /**
     * Bytes To Human Readable
     * -----------------------------------------------------------------------
     * Evaluate Bytes To String Size Like: MB, GB
     * @param bytes double
     */
    public static String byteToString(Double bytes) {
        String sizeResult;
//        if(bytes==null) return "0b";

        if (bytes < KILOBYTE) {
            sizeResult =  bytes + " B";
        } else if (bytes < MEGABYTE) {
            double kbValue = bytes / KILOBYTE;
            sizeResult = String.format(Locale.ENGLISH, "%.1f KB", kbValue);
        } else if (bytes < GIGABYTE) {
            double mbValue = bytes / MEGABYTE;
            sizeResult = String.format(Locale.ENGLISH, "%.1f MB", mbValue);
        } else {
            double gbValue = bytes / GIGABYTE;
            sizeResult = String.format(Locale.ENGLISH, "%.1f GB", gbValue);
        }
        return sizeResult;
    }

    /**
     * Bytes To Human Readable
     * -----------------------------------------------------------------------
     * Evaluate Bytes To String Size Like: MB, GB
     * @param bytes long
     */
    public static String byteToString(Long bytes) {
        String sizeResult;
//        if(bytes==null) return "0b";

        if (bytes < KILOBYTE) {
            sizeResult =  bytes + " B";
        } else if (bytes < MEGABYTE) {
            double kbValue = (double) bytes / KILOBYTE;
            sizeResult = String.format(Locale.ENGLISH, "%.1f KB", kbValue);
        } else if (bytes < GIGABYTE) {
            double mbValue = (double) bytes / MEGABYTE;
            sizeResult = String.format(Locale.ENGLISH, "%.1f MB", mbValue);
        } else {
            double gbValue = (double) bytes / GIGABYTE;
            sizeResult = String.format(Locale.ENGLISH, "%.1f GB", gbValue);
        }
        return sizeResult;
    }

    /*########################### [ Data Formatting ] ###########################*/

    /**
     * Short Double
     * ----------------------------------------------------------------
     * - Cut long double to short double
     * - Example: cut (15.333367) To (15.3)
     * @param value double
     * @param numToKeep Number of decimal to keep after (.)
     * Example: shortDouble(15.333367, 1) => 15.3
     */
    public static double shortDouble(double value, int numToKeep) {
        double scalingFactor = Math.pow(10, numToKeep);
        return Math.floor(value * scalingFactor) / scalingFactor;
    }

    /**
     * Short Double
     * ----------------------------------------------------------------
     * - Cut long double to short double & Set Locale
     * - Example: cut (15.333367) To (15.3)
     * @param value double
     * @param numToKeep Number of decimal to keep after (.)
     * @param locale the output locale: ex ENGLISH, JAPAN, ROOT, etc
     * Example: shortDouble(15.333367, 1,LOCALE.ROOT) => ۱۵.۳
     */
    public static String shortDouble(double value, int numToKeep,  Locale locale) {
        double scaledValue = Math.pow(10, numToKeep);
        int intValue = (int) (value * scaledValue);
        double formattedValue = intValue / scaledValue;
        Locale mLocale = (locale!=null) ? locale : Locale.ENGLISH;
        return String.format(mLocale, "%." + numToKeep + "f", formattedValue, Locale.ENGLISH);
    }



    /**
     * Remove Chars After Dot (.)
     * ------------------------------------------------------------------------
     * @param input example: "6.33378GB"
     * @param CharsToKeep number of chars to keep after (.)
     * Example: removeCharsAfterDot("6.33378GB",1) => "6.3";
     */
    public static String removeCharsAfterDot(String input, int CharsToKeep) {
        Pattern pattern = Pattern.compile("(.*\\.)(.{0," + CharsToKeep + "}).*");
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            return matcher.group(1) + matcher.group(2);
        }

        return input;
    }




    /*########################### [ KB,MB,GB,Long ] ###########################*/
    // Double can store big data than long

    public static double bytesToGigabytes(Double bytes) {
        return (bytes / (1024 * 1024 * 1024));
    }
    public static double bytesToGigabytes(Long bytes) {
        // TODO: 10/6/2024 check if contains (xxx.0) remove (.0)
        return ((double) bytes / (1024 * 1024 * 1024));
    }
    public static double bytesToMegabytes(Double bytes) {
        return (bytes / (1024 * 1024));
    }
    public static double bytesToMegabytes(Long bytes) {
        // TODO: 10/6/2024 check if contains (xxx.0) remove (.0)
        return ((double) bytes / (1024 * 1024));
    }

    public static double bytesToKelobytes(Double bytes) {
        return  (bytes / 1024);
    }



    public static double gigaBytesToBytes(Double megabytes) {
        return (megabytes * 1024 * 1024 * 1024);
    }
    public static double megabytesToBytes(Double megabytes) {
        return (megabytes * 1024 * 1024);
    }

    public static double kelobytesToBytes(Double megabytes) {
        return  (megabytes * 1024);
    }


}