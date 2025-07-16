package com.iorgana.droidhelpers.network;
import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

public class WifiHelper {

    /**
     * Is Wifi Enabled
     * ------------------------------------------------------------------------
     */
    public static boolean isWifiEnabled(Application application){
        Context applicationContext = application.getApplicationContext();
        WifiManager wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    /**
     * Set Wifi Enabled
     * ------------------------------------------------------------------------
     */
    public static void setWifiEnabled(Application application, boolean setEnabled){
        Context applicationContext = application.getApplicationContext();
        WifiManager wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(setEnabled);
    }

    /**
     * Is Wifi Hotspot Enabled
     * ------------------------------------------------------------------------
     * return True if enabled
     * return false if disabled
     * return null if something went wrong
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
     * Get Wifi IP Address
     * -------------------------------------------------------------------
     */
    public static String getWifiIpAddress(Context context){
        // todo not implemented yet
        return null;
    }
}