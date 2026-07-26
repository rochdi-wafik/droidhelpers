package com.iorgana.droidhelpers_project.ui.demo;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.iorgana.droidhelpers.notification.NotificationMaker;
import com.iorgana.droidhelpers_project.R;
import com.iorgana.droidhelpers_project.ui.MainActivity;
import com.iorgana.droidhelpers_project.ui.base.BaseDemoActivity;

/**
 * NotificationsActivity
 * -----------------------------------------------------------------------------
 * Live usage examples for com.iorgana.droidhelpers.notification package:
 * NotificationMaker (unique instances or a shared singleton).
 * Requires android.permission.POST_NOTIFICATIONS at runtime on API 33+.
 */
public class NotificationsActivity extends BaseDemoActivity {

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
                    toast(granted ? "Notifications permission granted" : "Notifications permission denied"));

    private NotificationMaker uniqueMaker;

    @Override
    protected String getScreenTitle() {
        return "Notifications";
    }

    @Override
    protected void buildContent() {
        LinearLayout s = addSection("NotificationMaker",
                "Builder for local notifications: unique instances via the constructor, or a shared instance via getInstance().");

        runSafe(addRow(s, "areNotificationsEnabled()"), () ->
                String.valueOf(NotificationMaker.getInstance(this).areNotificationsEnabled()));

        Row permRow = addRow(s, "Request POST_NOTIFICATIONS permission  (API 33+)");
        permRow.button.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    permRow.output.setText("Already granted");
                } else {
                    permRow.output.setText("Requesting...");
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                permRow.output.setText("Not required below API 33");
            }
        });

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        runSafe(addRow(s, "new NotificationMaker(context)  (unique) .setTitle().setContent().setActivityPendingIntent().show()"), () -> {
            uniqueMaker = new NotificationMaker(this)
                    .setTitle("DroidHelpers demo")
                    .setContent("A unique NotificationMaker instance")
                    .setTicker("DroidHelpers")
                    .setResIcon(R.mipmap.ic_launcher)
                    .setActivityPendingIntent(contentIntent);
            uniqueMaker.show();
            return "Shown with id=" + uniqueMaker.getNotificationID();
        });

        runSafe(addRow(s, "setAction(icon, text, pendingIntent)  (adds an action button, then show())"), () -> {
            if (uniqueMaker == null) return "Run the row above first to create a NotificationMaker";
            uniqueMaker.setAction(R.mipmap.ic_launcher, "Open App", contentIntent);
            uniqueMaker.show();
            return "Action button added and notification re-shown";
        });

        runSafe(addRow(s, "getInstance(context)  (shared) .setOnGoing(true).setAlertOnce(true).setChannelID().show()"), () -> {
            NotificationMaker shared = NotificationMaker.getInstance(this)
                    .setChannelID("shared_demo_channel")
                    .setTitle("Shared notification")
                    .setContent("Built via getInstance() singleton")
                    .setOnGoing(true)
                    .setAlertOnce(true);
            shared.show();
            return "Shown (ongoing) with id=" + shared.getNotificationID();
        });

        runSafe(addRow(s, "create()  (build Notification object without showing it)"), () -> {
            android.app.Notification n = NotificationMaker.getInstance(this).create();
            return "Notification object created: " + (n != null);
        });

        runSafe(addRow(s, "updateNotification(title, content, icon)"), () -> {
            NotificationMaker.getInstance(this).updateNotification("Updated title", "Content changed at runtime", null);
            return "Shared notification updated in place";
        });

        runSafe(addRow(s, "Getters: getNotificationID() / getChannelID() / getNotification() / getNotificationManager() / getNotificationChannel() / getNotificationBuilder()"), () -> {
            NotificationMaker shared = NotificationMaker.getInstance(this);
            return "id=" + shared.getNotificationID()
                    + ", channel=" + shared.getChannelID()
                    + ", notification!=null=" + (shared.getNotification() != null)
                    + ", manager!=null=" + (shared.getNotificationManager() != null)
                    + ", channelObj!=null=" + (shared.getNotificationChannel() != null)
                    + ", builder!=null=" + (shared.getNotificationBuilder() != null);
        });

        runSafe(addRow(s, "cancelNotification()  (on the shared instance)"), () -> {
            NotificationMaker.getInstance(this).cancelNotification();
            return "Shared notification cancelled";
        });
    }
}