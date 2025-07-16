package com.iorgana.droidhelpers.runtime;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;

public class RestartHelper {
    private static final String TAG = "__RestartHelper";

    /**
     * Restart app to Main (Startup activity)
     * ---------------------------------------------------------------------------
     */
    public static void restartToMain(Context context, @Nullable String sendAction) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        assert intent != null;
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);

        // Send action within the intent
        if(sendAction!=null) mainIntent.setAction(sendAction);

        // For API 34 and later, set the package explicitly
        mainIntent.setPackage(context.getPackageName());

        // Exit while Start the activity
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    /**
     * Restart app to specific activity
     * ---------------------------------------------------------------------------
     */
    public static void restartToTarget(Context context, Intent nextIntent) {
        // Create a new intent to launch the target activity
        nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // For API 34 and later, set the package explicitly
        nextIntent.setPackage(context.getPackageName());

        // Start the activity and exit the process
        context.startActivity(nextIntent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
        Runtime.getRuntime().exit(0);
    }

}
