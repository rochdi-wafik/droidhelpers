package com.iorgana.droidhelpers.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class ConnectivityUtils {


    /**
     * ******************************************************************
     * Is Network Connected
     * ******************************************************************
     * - Is device connected to network regardless of whether that connection
     *   actually has internet access or not.
     * - Checks on (CELLULAR - WIFI - VPN - ETHERNET)
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
     * ******************************************************************
     * Is Device Has Internet Connection
     * ******************************************************************
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
