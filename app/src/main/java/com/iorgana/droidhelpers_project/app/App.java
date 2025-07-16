package com.iorgana.droidhelpers_project.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.BuildConfig;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

public class App extends Application {
    private static final String TAG = "__App";
    Context baseContext;
    public static Application application;
    private static SharedPreferences preferences;

    /**
     * ****************************************************************************
     *  attacheBaseContext
     * ****************************************************************************
     * This method is called before onCreate
     * and is used to apply configurations to the base context of the application
     * before return the final context.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        this.baseContext = base;
    }

    /**
     * ****************************************************************************
     *    onCreate
     * ****************************************************************************
     *
     */
    @Override
    public void onCreate() {
        super.onCreate();
        this.baseContext = this;
        application = this;
        preferences = getSharedPreferences();

        // Initialize Logger
        initLogger();

        // Set Light
        setNightMode(false);

    }

    /**
     * ****************************************************************************
     *    Get SharedPreferences
     * ****************************************************************************
     *  Get SharedPreferences from anywhere
     */
    public static SharedPreferences getSharedPreferences(){
        if(preferences==null){
            preferences = application.getSharedPreferences(Constants.APP_PREFS, Context.MODE_PRIVATE);
        }
        return preferences;
    }

    /**
     * ****************************************************************************
     *  Get Context
     * ****************************************************************************
     *  Get Application Context from anywhere
     */
    public static Application getContext(){
        return App.application;
    }



    /**
     * ****************************************************************************
     *   Initialize Logger
     * ****************************************************************************
     * Logger will be used only in dev mode
     */
    private void initLogger(){

        // Setup
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)  // (Optional) Whether to show thread info or not. Default true
                .methodCount(0)         // (Optional) How many method line to show. Default 2
                .methodOffset(7)        // (Optional) Hides internal method calls up to offset. Default 5
                .tag(TAG)   // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .build();

        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));

        // Only run on Debug
        Logger.addLogAdapter(new AndroidLogAdapter() {
            @Override public boolean isLoggable(int priority, String tag) {
                return BuildConfig.DEBUG;
            }
        });
    }



    /**
     * ****************************************************************************
     *  Set Night Mode
     * ****************************************************************************
     *
     */
    public static void setNightMode(boolean setNight){
        if(setNight){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

}
