package com.iorgana.droidhelpers_project.ui;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.iorgana.droidhelpers.db.SqlPreferences;
import com.iorgana.droidhelpers_project.R;
import com.iorgana.droidhelpers_project.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "__MainActivity";
    ActivityMainBinding binding;
    Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        SqlPreferences.getInstance(activity).initSync();



    }
}