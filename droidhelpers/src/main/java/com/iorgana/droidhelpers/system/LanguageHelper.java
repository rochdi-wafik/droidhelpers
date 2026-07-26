package com.iorgana.droidhelpers.system;


import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.Locale;

/**
 * ************************************************************************
 * LanguageHelper
 * ************************************************************************
 * Helper methods for language and locale operations.
 */
public class LanguageHelper {

    /**
     * ************************************************************************
     * getSystemLanguage()
     * ************************************************************************
     * - Get the Android system language code.
     * ------------------------------------------------------------------------
     * @return The system language code (e.g., "en", "ar").
     */
    public static String getSystemLanguage(){
        return Resources.getSystem().getConfiguration().locale.getLanguage();
    }

    /**
     * ************************************************************************
     * getAppLanguage()
     * ************************************************************************
     * - Get the current application language code.
     * ------------------------------------------------------------------------
     * @return The app language code (e.g., "en", "ar").
     */
    public static String getAppLanguage(){
        return Locale.getDefault().getLanguage();
    }

    /**
     * ************************************************************************
     * updateAppLanguage()
     * ************************************************************************
     * - Update the application language at runtime.
     * ------------------------------------------------------------------------
     * @param context   Any valid context.
     * @param lang_code The language code (e.g., "en", "ar", "es").
     */
    public static void updateAppLanguage(Context context, String lang_code) {
        Locale myLocal = new Locale(lang_code);
        Locale.setDefault(myLocal);
        Resources res = context.getResources();
        DisplayMetrics metrics = res.getDisplayMetrics();
        Configuration configuration = res.getConfiguration();
        configuration.locale = myLocal;
        configuration.setLayoutDirection(myLocal);
        res.updateConfiguration(configuration,metrics);
    }
}