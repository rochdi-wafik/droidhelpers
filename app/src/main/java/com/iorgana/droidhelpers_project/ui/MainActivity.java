package com.iorgana.droidhelpers_project.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

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
}