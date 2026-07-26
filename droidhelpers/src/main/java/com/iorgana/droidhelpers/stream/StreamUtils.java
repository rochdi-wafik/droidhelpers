package com.iorgana.droidhelpers.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * ************************************************************************
 * StreamUtils
 * ************************************************************************
 * Utility methods for stream operations.
 */
public class StreamUtils {
    /**
     * ************************************************************************
     * streamToString()
     * ************************************************************************
     * - Convert an InputStream to a String.
     * ------------------------------------------------------------------------
     * @param inputStream The byte stream to convert.
     * @return The resulting string, or null if an error occurs.
     */
    public static String streamToString(InputStream inputStream) {
        // [-] Convert Stream to String (Json)
        String result=null;
        try {
            // Get response stream
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            // Extract string data
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            result = stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Return response
        return result;
    }
}