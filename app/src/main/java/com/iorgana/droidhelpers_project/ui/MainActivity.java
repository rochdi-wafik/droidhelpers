package com.iorgana.droidhelpers_project.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iorgana.droidhelpers_project.R;
import com.iorgana.droidhelpers_project.databinding.ActivityMainBinding;
import com.iorgana.droidhelpers_project.ui.adapter.HelperBoxAdapter;
import com.iorgana.droidhelpers_project.ui.demo.AlertsActivity;
import com.iorgana.droidhelpers_project.ui.demo.ConvertersActivity;
import com.iorgana.droidhelpers_project.ui.demo.CryptoActivity;
import com.iorgana.droidhelpers_project.ui.demo.ManagersActivity;
import com.iorgana.droidhelpers_project.ui.demo.NetworkActivity;
import com.iorgana.droidhelpers_project.ui.demo.NotificationsActivity;
import com.iorgana.droidhelpers_project.ui.demo.StorageActivity;
import com.iorgana.droidhelpers_project.ui.demo.SystemUtilsActivity;
import com.iorgana.droidhelpers_project.ui.demo.TimersActivity;
import com.iorgana.droidhelpers_project.ui.model.HelperBox;
import com.iorgana.droidhelpers_project.util.EdgeToEdgeUtils;

import java.util.Arrays;
import java.util.List;

/**
 * MainActivity
 * -----------------------------------------------------------------------------
 * Acts as the "book index" / map of the droidhelpers library: one box per
 * package, each opening a dedicated Activity with live usage examples of every
 * class in that package. This Activity does NOT contain any demo logic itself.
 */
public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeUtils.applyBarInsets(this);
        setTitle("DroidHelpers");

        List<HelperBox> boxes = Arrays.asList(
                new HelperBox("Storage", "SqlPreferences, SimpleDB, SecurePreferences", StorageActivity.class),
                new HelperBox("Networking", "HttpClient, AddressHelper, ConnectivityUtils, WifiHelper", NetworkActivity.class),
                new HelperBox("Encryption", "CryptoUtil, HmacVerifier, Base64Helper", CryptoActivity.class),
                new HelperBox("UI Alerts", "AlertMaker", AlertsActivity.class),
                new HelperBox("Notifications", "NotificationMaker", NotificationsActivity.class),
                new HelperBox("Timers", "CountdownTimer, ChronometerTimer", TimersActivity.class),
                new HelperBox("Converters", "JsonConverter, DataSizeConverter", ConvertersActivity.class),
                new HelperBox("App Managers", "FragmentsManager, InstancesManager", ManagersActivity.class),
                new HelperBox("System & Utils", "RestartHelper, ServiceHelper, StreamUtils, LanguageHelper, Utils, JPatterns", SystemUtilsActivity.class)
        );

        binding.recyclerHelperBoxes.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerHelperBoxes.setAdapter(new HelperBoxAdapter(boxes));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Handle Menu Items
     * ------------------------------------------------------------------------
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        // Item info
        if(itemId == R.id.nav_about_us){
            // Show developer name: Developed By Rochdi Wafik
            // Show Version: 1.0.4
            // Show GitHub link: https://github.com/rochdi-wafik/droidhelpers
            String message = "Developed By: Rochdi Wafik\n" +
                    "Version: 1.0.4\n" +
                    "GitHub: https://github.com/rochdi-wafik/droidhelpers";
            new MaterialAlertDialogBuilder(activity)
                    .setTitle("About DroidHelpers")
                    .setMessage(message)
                    .show();
        }

        // item exit
        if (itemId == R.id.nav_exit) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle("Exit Confirmation")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Exit", (dialogInterface, i)->{
                        activity.finishAffinity();
                        System.exit(0);
                    })
                    .setNegativeButton("Cancel", ((dialogInterface, i) -> dialogInterface.dismiss()))
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }
}