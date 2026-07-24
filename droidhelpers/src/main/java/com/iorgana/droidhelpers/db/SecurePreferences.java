package com.iorgana.droidhelpers.db;


import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;

import com.frybits.harmony.secure.EncryptedHarmony;
import com.iorgana.droidhelpers.crypto.CryptoUtil;
import com.iorgana.droidhelpers.utils.Utils;
import com.orhanobut.logger.Logger;

import java.util.HashMap;
import java.util.Map;

public class SecurePreferences {
    private static final String TAG = "__SecurePreferences";

    // Initialize the map to hold multiple SharedPreferences instances keyed by name
    private static final Map<String, SharedPreferences> preferencesMap = new HashMap<>();

    /**
     * ************************************************************************
     *   Get SharedPreferences
     * ************************************************************************
     * - This SharedPreferences are Encrypted and Multi-Process Safe
     * - We can use this method to access SharedPreferences from anywhere.
     * --------------------------------------------------------------------------------
     * - If EncryptedSharedPreferences fails to initialize:
     *  -- Debugging Mode: Throw an exception to crash the app and catch the error.
     *  -- Production Mode: Fallback to non-encrypted SharedPreferences to avoid crashing the app.
     * --------------------------------------------------------------------------------
     * @param context Any context (application context is used for prefs).
     * @param name    The name of the SharedPreferences file.
     * @return        The EncryptedSharedPreferences instance.
     * @apiNote Warning:  EncryptedSharedPreferences.PrefKeyEncryptionScheme deprecated
     */

    public static synchronized SharedPreferences getSharedPreferences(Context context, String name){
        // Check pref name
        if (name == null || name.trim().isEmpty()) {
            if(Utils.isDebuggingMode(context)){
                throw new IllegalArgumentException("Preference name cannot be null or empty");
            }
            else{
                Logger.e(TAG+" getSharedPreferences(): Preference name cannot be null or empty");
                return null;
            }
        }

        // Return cached instance if available
        synchronized (preferencesMap) {
            if (preferencesMap.containsKey(name)) {
                return preferencesMap.get(name);
            }
        }

        // No cache: Create Pref
        SharedPreferences sharedPrefs = null;
        try{
            String masterKeyAlias = CryptoUtil.getOrCreateMasterKeyAlias();
            // Create the encrypted prefs
            sharedPrefs = EncryptedHarmony.getSharedPreferences(
                    context.getApplicationContext(),
                    name,
                    masterKeyAlias,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        }catch (Exception e){
            if(Utils.isDebuggingMode(context)){
                throw new RuntimeException("Failed to initialize EncryptedSharedPreferences for '" + name + "'. Check logs.", e);
            }else {
                // Production fallback, create Android API preferences
                Logger.w(TAG, "Falling back to unencrypted SharedPreferences for: " + name);
                sharedPrefs = context.getApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE);
            }
        }
        // Save the created prefs object
        synchronized (preferencesMap) {
            preferencesMap.put(name, sharedPrefs);
        }

        // Return the prefs
        return sharedPrefs;
    }
}
