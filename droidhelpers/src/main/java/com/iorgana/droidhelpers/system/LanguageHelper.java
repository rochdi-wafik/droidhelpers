package com.iorgana.droidhelpers.system;


import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.util.Locale;

public class LanguageHelper {

    /**
     * Get Android System Language
     * @return lang_code
     */
    public static String getSystemLanguage(){
        return Resources.getSystem().getConfiguration().locale.getLanguage();
    }

    /**
     * Get Application Language
     * @return lang_code
     */
    public static String getAppLanguage(){
        return Locale.getDefault().getLanguage();
    }

    /**
     * Update App Language
     * @param context:
     * @param lang_code: ar en es etc
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
