package com.iorgana.droidhelpers.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

public class NetworkHelper {
    /**
     * Is Device Connected to a network (CELLULAR or WIFI)
     */
    public static boolean isConnected(Context context){
        boolean isConnected = false;
        ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Api Level 23+
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
        if(capabilities!=null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                isConnected = true;
            }
        }

        return isConnected;
    }
}
