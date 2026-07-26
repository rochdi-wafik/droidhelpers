package com.iorgana.droidhelpers.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * ************************************************************************
 * Utils
 * ************************************************************************
 * General utility methods used across the library.
 */
public class Utils {

    /**
     * ************************************************************************
     * isDebuggingMode()
     * ************************************************************************
     * - Check if the application is running in debug mode.
     * ------------------------------------------------------------------------
     * @param anyContext Any valid context.
     * @return true if the app is debuggable, false otherwise.
     */
    public static boolean isDebuggingMode(Context anyContext){
        return (anyContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}