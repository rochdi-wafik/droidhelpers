package com.iorgana.droidhelpers.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * ************************************************************************
 * ConnectivityUtils
 * ************************************************************************
 * Utility methods for checking network connectivity status.
 */
public class ConnectivityUtils {


    /**
     * ************************************************************************
     * isConnected()
     * ************************************************************************
     * - Check if the device is connected to a network.
     * - Checks for CELLULAR, WIFI, VPN, ETHERNET transports.
     * ------------------------------------------------------------------------
     * @param context Any valid context.
     * @return true if connected to any network, false otherwise.
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) return false;

        Network activeNetwork = manager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = manager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            return false;
        }

        // Check for any of these transport types: CELLULAR, WIFI, VPN, ETHERNET, etc.
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }

    /**
     * ************************************************************************
     * hasInternet()
     * ************************************************************************
     * - Check if the device has verified internet connectivity.
     * ------------------------------------------------------------------------
     * @param context Any valid context.
     * @return true if the device has validated internet access, false otherwise.
     */
    public static boolean hasInternet(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        Network activeNetwork = manager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = manager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            return false;
        }

        // NET_CAPABILITY_INTERNET: The network is set up to reach the internet.
        // NET_CAPABILITY_VALIDATED: The system successfully verified actual internet connectivity.
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}