package com.iorgana.droidhelpers.network;
import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

/**
 * ************************************************************************
 * WifiHelper
 * ************************************************************************
 * Helper methods for WiFi operations.
 */
public class WifiHelper {

    /**
     * ************************************************************************
     * isWifiEnabled()
     * ************************************************************************
     * - Check if WiFi is enabled on the device.
     * ------------------------------------------------------------------------
     * @param application The application instance.
     * @return true if WiFi is enabled, false otherwise.
     */
    public static boolean isWifiEnabled(Application application){
        Context applicationContext = application.getApplicationContext();
        WifiManager wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    /**
     * ************************************************************************
     * setWifiEnabled()
     * ************************************************************************
     * - Enable or disable WiFi on the device.
     * ------------------------------------------------------------------------
     * @param application The application instance.
     * @param setEnabled  true to enable, false to disable.
     */
    public static void setWifiEnabled(Application application, boolean setEnabled){
        Context applicationContext = application.getApplicationContext();
        WifiManager wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(setEnabled);
    }

    /**
     * ************************************************************************
     * isHotspotEnabled()
     * ************************************************************************
     * - Check if the WiFi hotspot is enabled on the device.
     * ------------------------------------------------------------------------
     * @param application The application instance.
     * @return true if enabled, false if disabled, null if an error occurred.
     */
    public static Boolean isHotspotEnabled(Application application){
        Boolean isConnected = null;
        Context applicationContext = application.getApplicationContext();
        WifiManager wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            isConnected = (Boolean) method.invoke(wifiManager);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return isConnected;
    }

    /**
     * ************************************************************************
     * getWifiIpAddress()
     * ************************************************************************
     * - Get the WiFi IP address of the device.
     * ------------------------------------------------------------------------
     * @param context Any valid context.
     * @return The WiFi IP address, or null if not available.
     * @todo Not implemented yet.
     */
    public static String getWifiIpAddress(Context context){
        // todo not implemented yet
        return null;
    }
}