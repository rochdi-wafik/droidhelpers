package com.iorgana.droidhelpers_project.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * DemoService
 * -----------------------------------------------------------------------------
 * A trivial, no-op started Service that exists only so SystemUtilsActivity can
 * demonstrate ServiceHelper.isServiceRunning() against a real, running Service.
 */
public class DemoService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}