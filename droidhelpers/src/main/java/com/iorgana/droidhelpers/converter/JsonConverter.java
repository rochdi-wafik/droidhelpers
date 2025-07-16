package com.iorgana.droidhelpers.converter;

import com.orhanobut.logger.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;

public class JsonConverter {
    private static final String TAG = "__JsonHelper";

    /**
     * Convert Keys To Java Format
     * -----------------------------------------------------------------------------
     * - [1] Loop over JSON string,
     * - [2] change key format from thia_case to thisCase,
     * - [3] and return new JSON string
     * @param jsonString JSON string in this_case format
     * @return JSON string in java case format
     *
     * [Warning] This method works until now with json objects {}, not json array []
     */
    public static String convertKeysToJavaCase(String jsonString){
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject convertedObject = new JSONObject();

            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                convertedObject.put(jsonToJavaCase(key), jsonObject.get(key));
            }
            return convertedObject.toString();
        }catch (JSONException e){
            Logger.d(TAG + " convertKeysToJavaCase(): JSONException: "+e.getMessage());
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Convert Keys To Json Format
     * -----------------------------------------------------------------------------
     * - [1] Loop over JSON string,
     * - [2] change key format from thisCase to this_case,
     * - [3] and return new JSON string
     * @param jsonString JSON string in thisCase format
     * @return JSON string in this_case format
     */
    public static String convertKeysToJsonCase(String jsonString){
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject convertedObject = new JSONObject();

            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                convertedObject.put(javaToJsonCase(key), jsonObject.get(key));
            }
            return convertedObject.toString();
        }catch (JSONException e){
            Logger.d(TAG + " convertKeysToJsonCase(): JSONException: "+e.getMessage());
            e.printStackTrace();
            return null;
        }

    }


    /**
     * javaToJsonCase: Convert name format to (this_case)
     * -----------------------------------------------------------------------------
     * Convert variable name from (thisCase) to (this_case)
     * @param variableName example: appName
     * @return json case: example: app_name
     */
    public static String javaToJsonCase(String variableName) {
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
     * jsonToJavaCase: Convert name format to (thisCase)
     * -----------------------------------------------------------------------------
     * Convert variable name from (this_case) to (thisCase)
     * @param variable_name example: app_name
     * @return java case: example: appName
     */
    public static String jsonToJavaCase(String variable_name){
        // Split the key by underscores
        String[] parts = variable_name.split("_");

        // if its already in java case, return it
        if(parts.length==0){
            return variable_name;
        }

        // Convert each part to lowercase and capitalize the first letter
        for (int i = 1; i < parts.length; i++) {
            parts[i] = capitalizeFirstLetter(parts[i]);
        }

        // Join the parts and return the modified key
        return String.join("", parts);
    }

    private static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
