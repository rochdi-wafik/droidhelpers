package com.iorgana.droidhelpers.db;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.frybits.harmony.Harmony;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iorgana.droidhelpers.crypto.CryptoUtil;
import com.iorgana.droidhelpers.utils.Utils;
import com.orhanobut.logger.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ************************************************************************
 * SecurePreferences
 * ************************************************************************
 * - Encrypted, multi-process-safe SharedPreferences.
 * - Storage is plain multi-process Harmony. Values are encrypted with the
 *   Keystore AES key in CryptoUtil; keys are stored as-is.
 * - This replaces the AndroidX EncryptedSharedPreferences path, which is
 *   deprecated. There is no key or master-key alias to manage.
 *
 * [Key names]
 * - Keys are NOT encrypted, only values. A reader with disk access can see
 *   which keys exist, not what they hold. Do not put a secret in a key name.
 *
 * [Unreadable values]
 * - A value that cannot be decrypted (a lost Keystore key) reads back as the
 *   caller's default, never as a crash. Do not store here anything that
 *   cannot be rebuilt.
 * ------------------------------------------------------------------------
 * @author Rochdi Wafik
 * @lastUpdate 28-08-2026
 */
public class SecurePreferences {
    private static final String TAG = "__SecurePreferences";

    /**
     * Type tags
     * ------------------------------------------------------------------------
     * - Prepended to a value before it is encrypted, so the typed getters
     *   hand back the right kind and getAll() can rebuild the map.
     */
    private static final char T_STRING  = 'S';
    private static final char T_INT     = 'I';
    private static final char T_LONG    = 'L';
    private static final char T_FLOAT   = 'F';
    private static final char T_BOOLEAN = 'B';
    private static final char T_SET     = 'G';

    // One instance per preference name.
    private static final Map<String, SharedPreferences> preferencesMap = new ConcurrentHashMap<>();

    private SecurePreferences(){}

    /**
     * ************************************************************************
     * getSharedPreferences()
     * ************************************************************************
     * - Get encrypted, multi-process-safe SharedPreferences.
     * - In debug mode, throws if the store cannot be opened.
     * - In production, falls back to the platform SharedPreferences as the
     *   backing store (values stay encrypted, since the wrapper does that).
     * ------------------------------------------------------------------------
     * @param context Any context (application context is used).
     * @param name    The name of the SharedPreferences file.
     * @return The encrypted SharedPreferences instance, or null on a bad name.
     */
    public static synchronized SharedPreferences getSharedPreferences(Context context, String name){
        if (name == null || name.trim().isEmpty()) {
            if (Utils.isDebuggingMode(context)) {
                throw new IllegalArgumentException("Preference name cannot be null or empty");
            }
            Logger.e(TAG + " getSharedPreferences(): Preference name cannot be null or empty");
            return null;
        }

        SharedPreferences cached = preferencesMap.get(name);
        if (cached != null) return cached;

        Context app = context.getApplicationContext();

        SharedPreferences delegate;
        try {
            delegate = Harmony.getSharedPreferences(app, name);
        } catch (Exception e) {
            if (Utils.isDebuggingMode(context)) {
                throw new RuntimeException("Failed to open Harmony prefs for '" + name + "'.", e);
            }
            Logger.w(TAG + " getSharedPreferences(): Harmony unavailable, using platform prefs for " + name);
            delegate = app.getSharedPreferences(name, Context.MODE_PRIVATE);
        }

        SharedPreferences secure = new EncryptedPrefs(delegate);
        preferencesMap.put(name, secure);
        return secure;
    }

    /**
     * ************************************************************************
     * encrypt() (Private)
     * ************************************************************************
     * - Encrypt a tagged value. Returns null if encryption fails.
     */
    private static @Nullable String encrypt(char tag, String payload){
        String out = CryptoUtil.cipherEncrypt(tag + payload);
        if (out == null) {
            Logger.e(TAG + " encrypt(): encryption returned null, value dropped");
        }
        return out;
    }

    /**
     * ************************************************************************
     * decrypt() (Private)
     * ************************************************************************
     * - Decrypt a stored blob to its "tag + payload" plaintext.
     * - Returns null when the blob is missing or cannot be decrypted.
     */
    private static @Nullable String decrypt(@Nullable String stored){
        if (stored == null) return null;
        String plain = CryptoUtil.cipherDecrypt(stored);
        if (plain == null || plain.isEmpty()) return null;
        return plain;
    }

    /**
     * ************************************************************************
     * EncryptedPrefs
     * ************************************************************************
     * - SharedPreferences facade that encrypts values on write and decrypts
     *   on read, over a plain SharedPreferences delegate.
     */
    static final class EncryptedPrefs implements SharedPreferences {
        private final SharedPreferences delegate;
        private final Gson gson = new Gson();
        private final Map<OnSharedPreferenceChangeListener, OnSharedPreferenceChangeListener> listeners = new ConcurrentHashMap<>();

        EncryptedPrefs(SharedPreferences delegate){
            this.delegate = delegate;
        }

        @Override
        public Map<String, ?> getAll() {
            Map<String, Object> out = new HashMap<>();
            for (Map.Entry<String, ?> entry : delegate.getAll().entrySet()) {
                Object raw = entry.getValue();
                if (!(raw instanceof String)) continue;
                String plain = decrypt((String) raw);
                if (plain == null) continue;
                Object value = decode(plain);
                if (value != null) out.put(entry.getKey(), value);
            }
            return out;
        }

        @Override
        public String getString(String key, String defValue) {
            String body = body(key, T_STRING);
            return (body == null) ? defValue : body;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            String body = body(key, T_SET);
            if (body == null) return defValues;
            try {
                Type type = new TypeToken<List<String>>(){}.getType();
                List<String> list = gson.fromJson(body, type);
                return (list == null) ? defValues : new HashSet<>(list);
            } catch (Exception e) {
                return defValues;
            }
        }

        @Override
        public int getInt(String key, int defValue) {
            String body = body(key, T_INT);
            try { return (body == null) ? defValue : Integer.parseInt(body); }
            catch (Exception e) { return defValue; }
        }

        @Override
        public long getLong(String key, long defValue) {
            String body = body(key, T_LONG);
            try { return (body == null) ? defValue : Long.parseLong(body); }
            catch (Exception e) { return defValue; }
        }

        @Override
        public float getFloat(String key, float defValue) {
            String body = body(key, T_FLOAT);
            try { return (body == null) ? defValue : Float.parseFloat(body); }
            catch (Exception e) { return defValue; }
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            String body = body(key, T_BOOLEAN);
            return (body == null) ? defValue : Boolean.parseBoolean(body);
        }

        @Override
        public boolean contains(String key) {
            return delegate.contains(key);
        }

        @Override
        public Editor edit() {
            return new EncryptedEditor(delegate.edit());
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            if (listener == null) return;
            // Keys are stored in plaintext, so the delegate reports the real key.
            OnSharedPreferenceChangeListener wrapper =
                    (sp, key) -> listener.onSharedPreferenceChanged(EncryptedPrefs.this, key);
            listeners.put(listener, wrapper);
            delegate.registerOnSharedPreferenceChangeListener(wrapper);
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            OnSharedPreferenceChangeListener wrapper = listeners.remove(listener);
            if (wrapper != null) delegate.unregisterOnSharedPreferenceChangeListener(wrapper);
        }

        /**
         * Read a value, decrypt it, and return its payload only if the stored
         * type tag matches. A mismatched or unreadable value returns null so
         * the caller falls back to its default.
         */
        private @Nullable String body(String key, char tag){
            String plain = decrypt(delegate.getString(key, null));
            if (plain == null || plain.charAt(0) != tag) return null;
            return plain.substring(1);
        }

        /**
         * Rebuild a typed value from its "tag + payload" plaintext, for getAll().
         */
        private @Nullable Object decode(String plain){
            char tag = plain.charAt(0);
            String body = plain.substring(1);
            try {
                switch (tag) {
                    case T_STRING:  return body;
                    case T_INT:     return Integer.parseInt(body);
                    case T_LONG:    return Long.parseLong(body);
                    case T_FLOAT:   return Float.parseFloat(body);
                    case T_BOOLEAN: return Boolean.parseBoolean(body);
                    case T_SET:
                        Type type = new TypeToken<List<String>>(){}.getType();
                        List<String> list = gson.fromJson(body, type);
                        return (list == null) ? null : new HashSet<>(list);
                    default:        return null;
                }
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * ********************************************************************
         * EncryptedEditor
         * ********************************************************************
         * - Encrypts each value before handing it to the delegate editor.
         * - A null value removes the key, matching SharedPreferences.
         */
        final class EncryptedEditor implements Editor {
            private final Editor delegateEditor;

            EncryptedEditor(Editor delegateEditor){
                this.delegateEditor = delegateEditor;
            }

            @Override
            public Editor putString(String key, String value) {
                return (value == null) ? remove(key) : write(key, T_STRING, value);
            }

            @Override
            public Editor putStringSet(String key, Set<String> values) {
                return (values == null) ? remove(key) : write(key, T_SET, gson.toJson(new ArrayList<>(values)));
            }

            @Override
            public Editor putInt(String key, int value) {
                return write(key, T_INT, String.valueOf(value));
            }

            @Override
            public Editor putLong(String key, long value) {
                return write(key, T_LONG, String.valueOf(value));
            }

            @Override
            public Editor putFloat(String key, float value) {
                return write(key, T_FLOAT, String.valueOf(value));
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                return write(key, T_BOOLEAN, String.valueOf(value));
            }

            @Override
            public Editor remove(String key) {
                delegateEditor.remove(key);
                return this;
            }

            @Override
            public Editor clear() {
                delegateEditor.clear();
                return this;
            }

            @Override
            public boolean commit() {
                return delegateEditor.commit();
            }

            @Override
            public void apply() {
                delegateEditor.apply();
            }

            private Editor write(String key, char tag, String payload){
                String enc = encrypt(tag, payload);
                // Drop the write if encryption failed rather than store plaintext.
                if (enc != null) delegateEditor.putString(key, enc);
                return this;
            }
        }
    }
}