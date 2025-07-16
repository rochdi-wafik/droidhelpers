package com.iorgana.droidhelpers.notification;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import com.iorgana.droidhelpers.R;

public class NotificationHelperV1 {
    private final Context context;
    public String notificationChannelId = "notification_service_channel";
    public int notification_icon = R.drawable.ic_notification;

    public String notificationTitle = "My Application";
    public String notificationText = "App service is running";
    public int visibility = NotificationCompat.VISIBILITY_PUBLIC;
    public int priority = NotificationCompat.PRIORITY_DEFAULT;
    // Ongoing notifications cannot be dismissed by the user, so your application or service must take care of canceling them.
    public Boolean setCancelable;

    private PendingIntent mainPendingIntent;
    private PendingIntent actionPendingIntent;
    private int actionIcon;
    private String actionText="Action";
    // Use NotificationManager to show notification using nm.notify(id, notification)
    public NotificationManager notificationManager;
    public static int notify_id = 1;

    public NotificationHelperV1(Context context) {
        this.context = context;
    }

    /**
     * Determine Where to go when notification is clicked
     * @param pendingIntent:
     */
    public void setMainPendingIntent(PendingIntent pendingIntent) {
        this.mainPendingIntent = pendingIntent;
    }

    public void setAction(int icon, String text, PendingIntent actionPendingIntent){
        this.actionIcon = icon;
        this.actionText = text;
        this.actionPendingIntent = actionPendingIntent;
    }
    public Notification make(){
        // Create notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel( notificationChannelId, notificationTitle, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription(this.notificationText);
            notificationChannel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);

            notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationChannelId)
                .setContentTitle(this.notificationTitle)
                .setContentText(this.notificationText)
                .setSmallIcon(this.notification_icon)
                .setChannelId(notificationChannelId)
                .setVisibility(visibility)
                .setPriority(priority);


        if(this.setCancelable !=null){
            builder.setOngoing(!setCancelable);
        }
        if(this.actionPendingIntent!=null){
            builder.addAction(actionIcon, actionText, actionPendingIntent);
        }
        if (this.mainPendingIntent != null) {
            builder.setContentIntent(this.mainPendingIntent);
        }

        return builder.build();
    }
}
