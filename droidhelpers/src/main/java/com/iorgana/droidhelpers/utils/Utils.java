package com.iorgana.droidhelpers.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * General Utils
 */
public class Utils {

    /**
     * **************************************************************
     *  Is Debugging Mode
     * **************************************************************
     */
    public static boolean isDebuggingMode(Context anyContext){
        return (anyContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
