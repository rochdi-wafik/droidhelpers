package com.iorgana.droidhelpers.notification;


import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.iorgana.droidhelpers.R;

import java.util.Random;
/**
 * Notification Maker
 * -----------------------------------------------------------------------------
 * [Single Notification]
 * - Use Constructor to make unique notification
 * [Shared Notification]
 * - Use getInstance() to make single shared notification
 * - This can be used to share a single notification between multiple services
 * -------------------------------------------------------------------------------
 * @apiNote Runtime Permissions:  android.permission.POST_NOTIFICATIONS (for Android 13 and above)
 * - show() will silently no-op if permission isn't granted.
 */
public class NotificationMaker {
    private static final String TAG = "__SharedNotification";
    public static volatile NotificationMaker INSTANCE;
    public int notificationID;

    public String CHANNEL_ID = "shared_notification_channel";

    Application context;
    NotificationCompat.Builder notificationBuilder;
    NotificationManager notificationManager;
    NotificationChannel notificationChannel;
    Notification notification;

    /*-------- Setters Fields -----------*/
    private String title = "Notification title";
    private String content = "Notification content";
    private String ticker = "Notification ticker";
    private int resIcon = R.drawable.ic_notification;
    private PendingIntent activityPendingIntent;

    // Notification Action Button
    private int actionIcon;
    private String actionText;
    private PendingIntent actionPendingIntent;

    private Boolean onGoing;
    private Boolean alertOnce;

    /**
     * Constructor (Single)
     * ----------------------------------------------------------------
     * - Use this constructor to make single unique notification
     */
    public NotificationMaker(Context context) {
        this.context = (Application) context.getApplicationContext();
        this.notificationID = new Random().nextInt();
    }

    /**
     * Get Instance (Shared)
     * ----------------------------------------------------------------
     * - Use this method to create shared notification (singleton)
     */
    public static NotificationMaker getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (NotificationMaker.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NotificationMaker(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Helper to check if notifications are enabled for the app.
     * ----------------------------------------------------------------
     * - Useful for checking runtime permission status on Android 13 (API 33) and above.
     *
     * @return true if notifications are enabled, false otherwise.
     */
    public boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Get Notification
     * ----------------------------------------------------------------
     */
    public Notification create() {
        // Create Notification Channel
        this.notificationChannel = createNotificationChannel();
        // Create Notification manager
        this.notificationManager = context.getSystemService(NotificationManager.class);
        this.notificationManager.createNotificationChannel(notificationChannel);

        // Build Notification
        this.notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setTicker(ticker)
                .setSmallIcon(resIcon)
                .setChannelId(CHANNEL_ID)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (this.alertOnce != null) {
            notificationBuilder.setOnlyAlertOnce(alertOnce);
        }
        if (this.onGoing != null) {
            notificationBuilder.setOngoing(onGoing);
        }
        if (this.actionPendingIntent != null) {
            notificationBuilder.addAction(this.actionIcon, this.actionText, this.actionPendingIntent);
        }

        if (this.activityPendingIntent != null) {
            notificationBuilder.setContentIntent(this.activityPendingIntent);
        }

        // Create Notification
        this.notification = notificationBuilder.build();
        return notification;
    }

    private NotificationChannel createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, title, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(content);
        channel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
        return channel;
    }

    /**
     * Show Notification
     * -----------------------------------------------------------------------
     */
    public void show() {
        // Silently no-op if permission isn't granted
        if (!areNotificationsEnabled()) {
            return;
        }
        if (notification == null) {
            create();
        }
        if (notification != null && notificationManager != null) {
            notificationManager.notify(notificationID, notification);
        }
    }

    public void display() {
        show();
    }

    /**
     * Update Notification
     * -----------------------------------------------------------------------
     */
    public void updateNotification(@Nullable String mTitle, @Nullable String mContent, @Nullable Integer mResIcon) {
        if (mTitle != null) this.title = mTitle;
        if (mContent != null) this.content = mContent;
        if (mResIcon != null) this.resIcon = mResIcon;

        if (notificationBuilder == null) create();

        notificationChannel.enableVibration(false);

        notificationBuilder.setOnlyAlertOnce(true); // don't re-alert
        notificationBuilder.setContentTitle(this.title);
        notificationBuilder.setContentText(this.content);
        notificationBuilder.setSmallIcon(this.resIcon);

        notificationManager.notify(notificationID, notificationBuilder.build());
    }

    /**
     * Remove Notification
     * -----------------------------------------------------------------------
     */
    public void cancelNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(notificationID);
        }
    }

    /**
     * Setters
     */
    public NotificationMaker setTitle(String title) {
        this.title = title;
        return this;
    }

    public NotificationMaker setContent(String content) {
        this.content = content;
        return this;
    }

    public NotificationMaker setTicker(String ticker) {
        this.ticker = ticker;
        return this;
    }

    public NotificationMaker setResIcon(int resIcon) {
        this.resIcon = resIcon;
        return this;
    }

    public NotificationMaker setAlertOnce(Boolean alertOnce) {
        this.alertOnce = alertOnce;
        return this;
    }

    public NotificationMaker setOnGoing(Boolean onGoing) {
        this.onGoing = onGoing;
        return this;
    }

    public NotificationMaker setActivityPendingIntent(PendingIntent activityPendingIntent) {
        this.activityPendingIntent = activityPendingIntent;
        return this;
    }

    public NotificationMaker setAction(int icon, String text, PendingIntent actionPendingIntent) {
        this.actionIcon = icon;
        this.actionText = text;
        this.actionPendingIntent = actionPendingIntent;
        return this;
    }

    public NotificationMaker setChannelID(String CHANNEL_ID) {
        this.CHANNEL_ID = CHANNEL_ID;
        return this;
    }

    /**
     * Getters
     */
    public NotificationCompat.Builder getNotificationBuilder() {
        return notificationBuilder;
    }

    public NotificationChannel getNotificationChannel() {
        return notificationChannel;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public Notification getNotification() {
        return notification;
    }

    public int getNotificationID() {
        return notificationID;
    }

    public String getChannelID() {
        return CHANNEL_ID;
    }
}