package com.iorgana.droidhelpers_project.ui.demo;

import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.iorgana.droidhelpers.db.SecurePreferences;
import com.iorgana.droidhelpers.db.SimpleDB;
import com.iorgana.droidhelpers.db.SqlPreferences;
import com.iorgana.droidhelpers_project.model.UserModel;
import com.iorgana.droidhelpers_project.ui.base.BaseDemoActivity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * StorageActivity
 * -----------------------------------------------------------------------------
 * Live usage examples for com.iorgana.droidhelpers.db package:
 * SqlPreferences (SQLite-backed, cached, encrypted key/value store),
 * SimpleDB (Gson + encrypted SharedPreferences object store),
 * SecurePreferences (raw EncryptedSharedPreferences accessor).
 */
public class StorageActivity extends BaseDemoActivity {

    @Override
    protected String getScreenTitle() {
        return "Storage";
    }

    @Override
    protected void buildContent() {
        buildSqlPreferencesSection();
        buildSimpleDBSection();
        buildSecurePreferencesSection();
    }

    /* ------------------------------------------------------------------ */
    private void buildSqlPreferencesSection() {
        LinearLayout s = addSection("SqlPreferences",
                "SQLite-backed key/value store with in-memory caching and AES encryption.");

        EditText keyInput = addInput(s, "Key", "demo_key");
        EditText valueInput = addInput(s, "Value", "Hello DroidHelpers");

        SqlPreferences prefs = SqlPreferences.getInstance(this);

        runSafe(addRow(s, "putString(key, value).apply()"), () -> {
            prefs.putString(keyInput.getText().toString(), valueInput.getText().toString()).apply();
            return "Saved -> " + keyInput.getText() + " = " + valueInput.getText();
        });

        runSafe(addRow(s, "putInt/putBoolean/putFloat/putLong/putDouble(...).apply()"), () -> {
            prefs.putInt("demo_int", 42)
                    .putBoolean("demo_bool", true)
                    .putFloat("demo_float", 3.14f)
                    .putLong("demo_long", 123456789L)
                    .putDouble("demo_double", 2.71828)
                    .apply();
            return "Saved 5 chained primitive values";
        });

        runSafe(addRow(s, "getString(key, default)"), () ->
                prefs.getString(keyInput.getText().toString(), "Unknown"));

        runSafe(addRow(s, "getInt/getBoolean/getFloat/getLong(key, default)"), () ->
                "int=" + prefs.getInt("demo_int", 0)
                        + ", bool=" + prefs.getBoolean("demo_bool", false)
                        + ", float=" + prefs.getFloat("demo_float", 0f)
                        + ", long=" + prefs.getLong("demo_long", 0L));

        runSafe(addRow(s, "putObject(user).apply() / getObject(User.class)"), () -> {
            prefs.putObject("demo_user", new UserModel("Sami", 28, true)).apply();
            UserModel user = prefs.getObject("demo_user", UserModel.class);
            return String.valueOf(user);
        });

        runSafe(addRow(s, "putListObject(users).apply() / getListObject(User.class)"), () -> {
            List<UserModel> users = Arrays.asList(
                    new UserModel("Sami", 28, true),
                    new UserModel("Nore", 24, false));
            prefs.putListObject("demo_users", users).apply();
            List<UserModel> saved = prefs.getListObject("demo_users", UserModel.class);
            return String.valueOf(saved);
        });

        runSafe(addRow(s, "remove(key)"), () -> {
            prefs.remove(keyInput.getText().toString());
            return "Removed key: " + keyInput.getText();
        });

        runSafe(addRow(s, "removeObject(key) / removeListObject(key)"), () -> {
            prefs.removeObject("demo_user");
            prefs.removeListObject("demo_users");
            return "removed=" + (prefs.getObject("demo_user", UserModel.class) == null);
        });

        runSafe(addRow(s, "getAll()"), () -> {
            Map<String, Object> all = prefs.getAll();
            return "entries=" + all.size() + " -> " + all;
        });

        runSafe(addRow(s, "getCurrentSecretKey()"), prefs::getCurrentSecretKey);

        runSafe(addRow(s, "setAllowSaveNull(false) then putObject(key, null).apply() / getObject(...)"), () -> {
            prefs.setAllowSaveNull(false);
            prefs.putObject("demo_nullable_obj", (UserModel) null).apply();
            UserModel result = prefs.getObject("demo_nullable_obj", UserModel.class);
            return "allowSaveNull=false; putObject(null) -> getObject() = " + result;
        });

        Row initSyncRow = addRow(s, "initSync()  (loads SQLite -> cache, blocking)");
        runSafe(initSyncRow, () -> {
            prefs.initSync();
            return "Cache is loaded";
        });

        Row initRow = addRow(s, "static init(context, OnLoadListener)  (async)");
        initRow.button.setOnClickListener(v -> {
            initRow.output.setText("Loading...");
            SqlPreferences.init(getApplicationContext(), null,
                    () -> runOnUiThread(() -> initRow.output.setText("onLoaded(): cache ready")));
        });

        runSafe(addRow(s, "clear()  (wipes all SqlPreferences data)"), () -> {
            prefs.clear();
            return "All SqlPreferences data cleared";
        });
    }

    /* ------------------------------------------------------------------ */
    private void buildSimpleDBSection() {
        LinearLayout s = addSection("SimpleDB",
                "Gson-serialized objects/lists stored in encrypted SharedPreferences. Simpler than SqlPreferences, no SQLite.");

        SimpleDB simpleDB = SimpleDB.getInstance(this);

        runSafe(addRow(s, "getInstance(context)"), () ->
                "instance ready = " + (SimpleDB.getInstance(getApplicationContext()) != null));

        runSafe(addRow(s, "setAllowSaveNull(true)"), () -> {
            simpleDB.setAllowSaveNull(true);
            return "allowSaveNull = true";
        });

        runSafe(addRow(s, "saveObject(key, user) / getObject(key, User.class)"), () -> {
            simpleDB.saveObject("simpledb_user", new UserModel("Amine", 31, false));
            UserModel user = simpleDB.getObject("simpledb_user", UserModel.class);
            return String.valueOf(user);
        });

        runSafe(addRow(s, "saveListObject(key, users) / getListObject(key, User.class)"), () -> {
            List<UserModel> users = Arrays.asList(
                    new UserModel("Amine", 31, false),
                    new UserModel("Yassine", 22, true));
            simpleDB.saveListObject("simpledb_users", users);
            List<UserModel> saved = simpleDB.getListObject("simpledb_users", UserModel.class);
            return String.valueOf(saved);
        });

        runSafe(addRow(s, "removeObject(key)"), () -> {
            simpleDB.removeObject("simpledb_user");
            return "removed=" + (simpleDB.getObject("simpledb_user", UserModel.class) == null);
        });

        runSafe(addRow(s, "clear()  (wipes all SimpleDB data)"), () -> {
            simpleDB.clear();
            return "All SimpleDB data cleared";
        });
    }

    /* ------------------------------------------------------------------ */
    private void buildSecurePreferencesSection() {
        LinearLayout s = addSection("SecurePreferences",
                "Direct access to a named, encrypted, multi-process-safe SharedPreferences file.");

        EditText nameInput = addInput(s, "Preferences file name", "secure_demo_prefs");
        EditText valueInput = addInput(s, "Value to round-trip", "top-secret-value");

        runSafe(addRow(s, "getSharedPreferences(context, name) -> edit().putString().apply() -> getString()"), () -> {
            SharedPreferences prefs = SecurePreferences.getSharedPreferences(this, nameInput.getText().toString());
            if (prefs == null) return "getSharedPreferences() returned null";
            prefs.edit().putString("demo_value", valueInput.getText().toString()).apply();
            String roundTrip = prefs.getString("demo_value", null);
            return "Stored & decrypted back -> " + roundTrip;
        });
    }
}