package com.iorgana.droidhelpers.service;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * ************************************************************************
 * ServiceHelper
 * ************************************************************************
 * Helper methods for Android service operations.
 */
public class ServiceHelper {

    /**
     * ************************************************************************
     * isServiceRunning()
     * ************************************************************************
     * - Check if a specific service is currently running.
     * - Returns true only if the service was started (startService()).
     * - If the service is bound but not started, it returns false.
     * ------------------------------------------------------------------------
     * @param context      Any valid context.
     * @param serviceClass The class of the service to check.
     * @return true if the service is running, false otherwise.
     */
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground || service.started) {
                    isRunning = true;
                    break;
                }
            }
        }
        return isRunning;
    }

}