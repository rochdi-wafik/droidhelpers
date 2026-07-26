package com.iorgana.droidhelpers.converters;

import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Iterator;

/**
 * ************************************************************************
 * JsonConverter
 * ************************************************************************
 * - Utility methods for converting JSON key naming conventions between
 *   camelCase (Java) and snake_case (JSON).
 */
public class JsonConverter {
    private static final String TAG = "__JsonHelper";

    /**
     * ************************************************************************
     * convertKeysToJavaCase()
     * ************************************************************************
     * - Recursively convert all JSON keys from snake_case to camelCase.
     * - Supports nested objects, nested arrays, and root-level arrays.
     * ------------------------------------------------------------------------
     * @param jsonString JSON string in snake_case format.
     * @return JSON string in camelCase format, or null if parsing fails.
     */
    public static String convertKeysToJavaCase(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return jsonString;
        }
        try {
            Object parsed = new JSONTokener(jsonString).nextValue();
            Object converted = convertKeys(parsed, true);
            return converted.toString();
        } catch (JSONException e) {
            Logger.d(TAG + " convertKeysToJavaCase(): JSONException: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ************************************************************************
     * convertKeysToJsonCase()
     * ************************************************************************
     * - Recursively convert all JSON keys from camelCase to snake_case.
     * - Supports nested objects, nested arrays, and root-level arrays.
     * ------------------------------------------------------------------------
     * @param jsonString JSON string in camelCase format.
     * @return JSON string in snake_case format, or null if parsing fails.
     */
    public static String convertKeysToJsonCase(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return jsonString;
        }
        try {
            Object parsed = new JSONTokener(jsonString).nextValue();
            Object converted = convertKeys(parsed, false);
            return converted.toString();
        } catch (JSONException e) {
            Logger.d(TAG + " convertKeysToJsonCase(): JSONException: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ************************************************************************
     * convertKeys() (Recursive Helper)
     * ************************************************************************
     * - Recursively process JSON values and convert keys.
     * ------------------------------------------------------------------------
     * @param value      The current JSON value (Object, Array, or primitive).
     * @param toJavaCase true to convert to camelCase, false for snake_case.
     * @return The converted JSON value.
     * @throws JSONException if JSON processing fails.
     */
    private static Object convertKeys(Object value, boolean toJavaCase) throws JSONException {
        if (value instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) value;
            JSONObject newObj = new JSONObject();
            Iterator<String> keys = jsonObj.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                String newKey = toJavaCase ? jsonToJavaCase(key) : javaToJsonCase(key);
                newObj.put(newKey, convertKeys(jsonObj.get(key), toJavaCase));
            }
            return newObj;

        } else if (value instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) value;
            JSONArray newArray = new JSONArray();

            for (int i = 0; i < jsonArray.length(); i++) {
                newArray.put(convertKeys(jsonArray.get(i), toJavaCase));
            }
            return newArray;

        } else {
            // Primitive value (String, Number, Boolean, null), return as-is
            return value;
        }
    }

    /**
     * ************************************************************************
     * javaToJsonCase()
     * ************************************************************************
     * - Convert a variable name from camelCase to snake_case.
     * ------------------------------------------------------------------------
     * @param variableName Example: "appName".
     * @return snake_case string, e.g., "app_name".
     */
    public static String javaToJsonCase(String variableName) {
        if (variableName == null) {
            return null;
        }
        StringBuilder jsonCase = new StringBuilder();
        for (int i = 0; i < variableName.length(); i++) {
            char c = variableName.charAt(i);
            if (Character.isUpperCase(c)) {
                jsonCase.append("_").append(Character.toLowerCase(c));
            } else {
                jsonCase.append(c);
            }
        }
        return jsonCase.toString();
    }

    /**
     * ************************************************************************
     * jsonToJavaCase()
     * ************************************************************************
     * - Convert a variable name from snake_case to camelCase.
     * - Bypasses conversion if no underscores are present.
     * ------------------------------------------------------------------------
     * @param variableName Example: "app_name".
     * @return camelCase string, e.g., "appName".
     */
    public static String jsonToJavaCase(String variableName) {
        // Fixed: Replaced unreachable parts.length == 0 check with a proper contains check
        if (variableName == null || !variableName.contains("_")) {
            return variableName;
        }

        String[] parts = variableName.split("_");
        StringBuilder javaCase = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            javaCase.append(capitalizeFirstLetter(parts[i].toLowerCase()));
        }

        return javaCase.toString();
    }

    /**
     * ************************************************************************
     * capitalizeFirstLetter() (Private Helper)
     * ************************************************************************
     * - Capitalize the first character of a string.
     * ------------------------------------------------------------------------
     * @param str The input string.
     * @return String with the first letter capitalized.
     */
    private static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}