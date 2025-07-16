package com.iorgana.droidhelpers.service;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

public class ServiceHelper {

    /**
     * Check IF A Service Is Running
     * -------------------------------------------------------------------------------
     * This service return true only if the service is running (startService())
     * IF The service is bound, but not started yet, it will return false
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
