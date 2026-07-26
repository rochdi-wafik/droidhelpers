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
 * ************************************************************************
 * NotificationMaker
 * ************************************************************************
 * - Build and manage Android notifications.
 * - Use the constructor for single unique notifications.
 * - Use getInstance() for a shared notification across multiple services.
 * ------------------------------------------------------------------------
 * @apiNote Requires android.permission.POST_NOTIFICATIONS (Android 13+).
 *          show() will silently no-op if permission is not granted.
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
     * ************************************************************************
     * NotificationMaker (Constructor - Single)
     * ************************************************************************
     * - Create a single unique notification instance.
     * ------------------------------------------------------------------------
     * @param context Any valid context.
     */
    public NotificationMaker(Context context) {
        this.context = (Application) context.getApplicationContext();
        this.notificationID = new Random().nextInt();
    }

    /**
     * ************************************************************************
     * getInstance() (Shared)
     * ************************************************************************
     * - Get a shared singleton notification instance.
     * ------------------------------------------------------------------------
     * @param context Any valid context.
     * @return The singleton NotificationMaker instance.
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
     * ************************************************************************
     * areNotificationsEnabled()
     * ************************************************************************
     * - Check if notifications are enabled for the app.
     * - Useful for checking runtime permission status on Android 13+.
     * ------------------------------------------------------------------------
     * @return true if notifications are enabled, false otherwise.
     */
    public boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * ************************************************************************
     * create()
     * ************************************************************************
     * - Build and return the notification object.
     * ------------------------------------------------------------------------
     * @return The constructed Notification object.
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
     * ************************************************************************
     * show()
     * ************************************************************************
     * - Display the notification.
     * - Silently no-ops if POST_NOTIFICATIONS permission is not granted.
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
     * ************************************************************************
     * updateNotification()
     * ************************************************************************
     * - Update the existing notification with new title, content, or icon.
     * ------------------------------------------------------------------------
     * @param mTitle   Optional new title (null to keep current).
     * @param mContent Optional new content (null to keep current).
     * @param mResIcon Optional new icon resource ID (null to keep current).
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
     * ************************************************************************
     * cancelNotification()
     * ************************************************************************
     * - Cancel (remove) the notification.
     */
    public void cancelNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(notificationID);
        }
    }

    /*------------------------------------------------------------------------*/
    /*  Setters                                                               */
    /*------------------------------------------------------------------------*/
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

    /*------------------------------------------------------------------------*/
    /*  Getters                                                               */
    /*------------------------------------------------------------------------*/
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