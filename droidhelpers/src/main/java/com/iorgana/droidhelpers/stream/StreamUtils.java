package com.iorgana.droidhelpers.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamUtils {
    /**
     * ----------------------------------------------------------------
     * Get String From Byte Stream
     * ----------------------------------------------------------------
     * Convert Stream response to String
     * @param inputStream byte stream
     * @return string result
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
