package com.iorgana.droidhelpers.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iorgana.droidhelpers.crypto.CryptoUtil;
import com.orhanobut.logger.Logger;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ************************************************************************
 * SqlPreferences
 * ************************************************************************
 * - SQLite wrapper for storing, retrieving, and managing key-value data.
 * - Acts like SharedPreferences but uses SQL instead of XML.
 * - Uses in-memory caching for speed and UI-thread compatibility.
 *
 * [Caching]
 * - Data is kept in memory (RAM) and written to disk in the background.
 * - This allows immediate retrieval after save, even before disk write
 *   completes.
 * - Call SqlPreferences.init() at app start to preload cache in the
 *   background.
 *
 * [Usage]
 * - SqlPreferences.getInstance(context).putString("key", "value").apply();
 * - String val = SqlPreferences.getInstance(context).getString("key", "default");
 *
 * [Do not close the instance]
 * - This class extends SQLiteOpenHelper, which is Closeable, but the
 *   instance returned by getInstance() is a shared singleton.
 * - Do NOT use it inside try-with-resources:
 *       try (SqlPreferences db = SqlPreferences.getInstance(ctx)) { ... }
 *   Closing it shuts the database down for every other caller, and may hit
 *   a background write that is still running.
 * - Each internal method opens and closes its own database handle already,
 *   so there is nothing for the caller to release.
 *
 * [Security]
 * - Values are encrypted with AES/GCM using a key held in the Android
 *   Keystore (see CryptoUtil). There is no key to pass or configure.
 * - Encryption can be turned off via SqlPreferences.ENABLE_ENCRYPTION =
 *   false before first use, for data that is not sensitive.
 * - The Keystore key is per-install and can be lost on some devices (a
 *   reinstall, or a lock-screen credential change). A row that no longer
 *   decrypts is dropped on load, not surfaced, so a lost key degrades to
 *   "value absent", never a crash. Do not store here anything that cannot
 *   be rebuilt.
 * ------------------------------------------------------------------------
 * @author Rochdi Wafik
 * @lastUpdate 28-08-2026
 */

public class SqlPreferences extends SQLiteOpenHelper {
    private static final String TAG = "__SqlPreferences";
    private static volatile SqlPreferences INSTANCE;

    /**
     * Executors
     * ------------------------------------------------------------------------
     * - IO operations may block the UI thread, so they are executed in the
     *   background. Caching allows this class to be used from the UI thread.
     * - Single thread on purpose: disk writes and deletes must land in the
     *   same order the caller issued them. With a multi-thread pool, a fast
     *   save() followed by remove() could be applied in reverse order.
     * - Reads are served from the cache, so one writer thread costs nothing.
     */
    public static final ExecutorService executors = Executors.newSingleThreadExecutor();

    /**
     * Sqlite Database
     * ----------------------------------------------------------------------
     * - Version 2: data_key became the PRIMARY KEY. Before that there was no
     *   unique constraint, so CONFLICT_REPLACE had no conflict to act on.
     * - Version 3: values moved to a Keystore-backed AES key. Rows written
     *   by any earlier version cannot be decrypted, so onUpgrade drops them.
     */
    private static final String DATABASE_NAME = "sql_preferences.db";
    private static final int DATABASE_VERSION = 3; // incremented on 28-08-2026
    public static String TABLE_NAME = "table_preferences";

    /**
     * Columns
     * ------------------------------------------------------------------------
     */
    public static final String COLUMN_KEY = "data_key";
    public static final String COLUMN_DATA_TYPE = "data_type";
    public static final String COLUMN_DATA_VALUE = "data_value";


    /**
     * Objects
     * ------------------------------------------------------------------------
     * - Prefixes are what separate a saved object from a saved list.
     * - Without them, if we save an object then save a list of the same
     *   object under the same key, the list would replace the object.
     * - Example:
     * $ User user = getUser()
     * $ List<User> users = getUsers();
     * $ save(user);
     * $ save(users);
     * -> Without prefixes, saving users overrides the saved user.
     * -> With prefixes, both are stored side by side.
     *
     * - The class name is NOT part of the key. Two reasons:
     *   1. The prefixes alone already separate object from list, which is
     *      the only thing the class name was there for.
     *   2. Class.getName() returns the obfuscated name under R8. That name
     *      changes between builds, so any key built from it stops matching
     *      after the host app ships a new release.
     * - legacyListKey() below documents the old layout, kept only so
     *   getListObject() can migrate rows written by older versions.
     */
    private static final String PREFIX_OBJ = "pref_obj_";
    private static final String PREFIX_LIST = "pref_list_obj_";

    /**
     * Encryption
     * ------------------------------------------------------------------------
     * - On by default. The AES key is managed by CryptoUtil in the Android
     *   Keystore; there is no key to set here.
     * - Set to false before first use only for data that is not sensitive.
     */
    public static boolean ENABLE_ENCRYPTION = true;

    /**
     * Caching
     * ------------------------------------------------------------------------
     * - cache: holds saved data in memory (RAM).
     * - tempMap: holds data added by put___() until apply() is called.
     *
     * @warning ConcurrentHashMap throws a NullPointerException if we attempt
     *          to insert a null value or a null key, unlike HashMap
     */
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> tempMap = new ConcurrentHashMap<>();

    /**
     * OnLoadListener
     * ------------------------------------------------------------------------
     * - Callback to notify when data is fully loaded into the cache.
     */
    public interface OnLoadListener{
        void onLoaded();
    }

    Boolean allowSaveNull = true; // we can assign null to an item


    /**
     * ------------------------------------------------------------------------
     * Constructor
     * ------------------------------------------------------------------------
     * - Use getInstance() to get an instance
     * @param context any context
     */
    public SqlPreferences(@NonNull Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * ---------------------------------------------------------------------------------
     *  Get Instance (Singleton)
     * ---------------------------------------------------------------------------------
     * - Returns the singleton instance of SqlPreferences.
     * - Ensures data is loaded into the in-memory cache synchronously if it
     *   has not been loaded yet.
     *
     * @param context Any valid context (will be safely converted to ApplicationContext)
     * @return The singleton SqlPreferences instance
     * @apiNote Do not close the returned instance. See the class header.
     */
    public static SqlPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SqlPreferences.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SqlPreferences(context.getApplicationContext());
                }
            }
        }

        // Ensure the cache is loaded. Returns immediately if init() already did it.
        INSTANCE.initSync();

        return INSTANCE;
    }

    /**
     * ************************************************************************
     * onCreate()
     * ************************************************************************
     * - Called when the database is created for the first time.
     * - COLUMN_KEY is the PRIMARY KEY, which is what makes CONFLICT_REPLACE
     *   in insertMap() replace an existing row instead of adding a duplicate.
     * ------------------------------------------------------------------------
     * @param db The SQLite database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE IF NOT EXISTS "+TABLE_NAME+" ("
                + COLUMN_KEY +" TEXT PRIMARY KEY, " // identifier
                + COLUMN_DATA_TYPE +" TEXT, " // String, Integer, etc
                + COLUMN_DATA_VALUE + " TEXT)";

        db.execSQL(query);
    }


    /**
     * ************************************************************************
     * onUpgrade()
     * ************************************************************************
     * - Values are now encrypted with a Keystore-backed key, so rows written
     *   by any earlier version cannot be decrypted. Drop and recreate rather
     *   than keep data that will only fail to load.
     * ------------------------------------------------------------------------
     * @param db         The SQLite database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * ************************************************************************
     * init() (Async)
     * ************************************************************************
     * - Load data into the cache in the background.
     * - Call this at app startup (e.g., Application.onCreate()).
     * ------------------------------------------------------------------------
     * @param anyContext      Any valid context.
     * @param onLoadListener  Optional callback when loading is complete.
     */
    public static void init(Context anyContext, @Nullable OnLoadListener onLoadListener) {
        // Create Instance (Double-checked locking)
        if (INSTANCE == null) {
            synchronized (SqlPreferences.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SqlPreferences(anyContext.getApplicationContext());
                }
            }
        }

        // Execute background loading
        executors.execute(() -> {
            if (INSTANCE.cache.isEmpty()) {
                INSTANCE.cache.putAll(INSTANCE.getAll());
            }
            if (onLoadListener != null) {
                onLoadListener.onLoaded();
            }
        });
    }

    /**
     * ************************************************************************
     * initSync()
     * ************************************************************************
     * - Load stored data into the cache synchronously (on the calling thread).
     * - Called automatically by getInstance() if data is not yet loaded.
     * - For better performance, use init() at app startup so this does not
     *   block the main thread.
     */
    public void initSync(){
        if(cache.isEmpty()){
            cache.putAll(this.getAll());
        }
    }


    /**
     * ************************************************************************
     * setAllowSaveNull()
     * ************************************************************************
     * - Set whether nullable values can be saved.
     * - By default, nullable objects can be saved.
     * ------------------------------------------------------------------------
     * @param allowNull true to allow saving null values, false to disallow.
     * @return This SqlPreferences instance for chaining.
     * @apiNote If disallowing null, set to its default by passing null.
     */
    public SqlPreferences setAllowSaveNull(Boolean allowNull){
        this.allowSaveNull = allowNull;
        return this;
    }

    /**
     * ************************************************************************
     * apply()
     * ************************************************************************
     * - Commit pending data: move temp data to cache and write to disk
     *   asynchronously.
     * - Must be called after put___() calls to persist the data.
     */
    public void apply(){
        // Check if null value allowed (allow save null)
        if(allowSaveNull!=null && !allowSaveNull){
            for(Map.Entry<String, Object> item: tempMap.entrySet()){
                if(item.getValue()==null){
                    // remove null values
                    tempMap.remove(item.getKey());
                }
            }
        }

        Map<String, Object> dataToWrite = new HashMap<>(tempMap);

        // Add data in temp map to the Cache
        if(!tempMap.isEmpty()){
            cache.putAll(tempMap);
            tempMap.clear();
        }

        // Add To Sql DB:
        // insertMap() will Write the data to the disk (sql) in background
        this.insertMap(dataToWrite);
        // insertMap() copies the map, so we can clear our reference immediately
        dataToWrite.clear();

    }
    /**
     * ************************************************************************
     * clear()
     * ************************************************************************
     * - Clear all data from cache and disk.
     * - This will delete the table and all saved data.
     */
    public void clear(){
        // Clear cache
        cache.clear();

        // Clear disk (sql)
        executors.execute(()->{
            try (SQLiteDatabase db = getWritableDatabase()) {
                String query = "DELETE FROM " + TABLE_NAME;
                db.execSQL(query);
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }



    /*==========================[ DB PUT ]==========================*/


    /**
     * ************************************************************************
     * putString()
     * ************************************************************************
     * - Store a string value. Call apply() to persist.
     * ------------------------------------------------------------------------
     * @param key   The identifier key.
     * @param value The value to save.
     * @return This SqlPreferences instance for chaining.
     */
    public SqlPreferences putString(String key, String value){
        tempMap.put(key, value);
        return this;
    }


    /**
     * ************************************************************************
     * putInt()
     * ************************************************************************
     * - Store an integer value. Call apply() to persist.
     * ------------------------------------------------------------------------
     * @param key   The identifier key.
     * @param value The value to save.
     * @return This SqlPreferences instance for chaining.
     */

    public SqlPreferences putInt(String key, int value){
        tempMap.put(key, value);
        return this;
    }


    /**
     * ************************************************************************
     * putBoolean()
     * ************************************************************************
     * - Store a boolean value. Call apply() to persist.
     * ------------------------------------------------------------------------
     * @param key   The identifier key.
     * @param value The value to save.
     * @return This SqlPreferences instance for chaining.
     */
    public SqlPreferences putBoolean(String key, Boolean value){
        tempMap.put(key, value);
        return this;
    }

    /**
     * ************************************************************************
     * putFloat()
     * ************************************************************************
     * - Store a float value. Call apply() to persist.
     * ------------------------------------------------------------------------
     * @param key   The identifier key.
     * @param value The value to save.
     * @return This SqlPreferences instance for chaining.
     */
    public SqlPreferences putFloat(String key, float value){
        tempMap.put(key, value);
        return this;
    }

    /**
     * ************************************************************************
     * putLong()
     * ************************************************************************
     * - Store a long value. Call apply() to persist.
     * ------------------------------------------------------------------------
     * @param key   The identifier key.
     * @param value The value to save.
     * @return This SqlPreferences instance for chaining.
     */
    public SqlPreferences putLong(String key, long value){
        tempMap.put(key, value);
        return this;
    }

    /**
     * ************************************************************************
     * putDouble()
     * ************************************************************************
     * - Store a double value. Call apply() to persist.
     * ------------------------------------------------------------------------
     * @param key   The identifier key.
     * @param value The value to save.
     * @return This SqlPreferences instance for chaining.
     */
    public SqlPreferences putDouble(String key, double value){
        tempMap.put(key, value);
        return this;
    }

    /**
     * ************************************************************************
     * putObject()
     * ************************************************************************
     * - Store a serializable object. Call apply() to persist.
     * ------------------------------------------------------------------------
     * @param key    The identifier key.
     * @param object The object to serialize and save.
     * @return This SqlPreferences instance for chaining.
     */
    public  <T> SqlPreferences putObject(String key, T object){

        // Create DB Key
        String OBJ_KEY = objectKey(key);

        // Serialize object to String
        Gson gson = new Gson();
        String jsonObj = gson.toJson(object);

        // Save serialized object
        tempMap.put(OBJ_KEY, jsonObj);

        return this;
    }

    /**
     * ************************************************************************
     * putListObject()
     * ************************************************************************
     * - Store a list of objects. Call apply() to persist.
     * - The key no longer includes the element class name, so the list is
     *   found again by getListObject() and removeListObject() using nothing
     *   but the key the caller passed.
     * ------------------------------------------------------------------------
     * @param key        The identifier key.
     * @param listObject The list of objects to serialize and save.
     * @return This SqlPreferences instance for chaining.
     * @apiNote Saving two different list types under the same key is no
     *          longer possible: the second call replaces the first, the same
     *          way putString() replaces a previous string.
     */
    public <T> SqlPreferences putListObject(String key, List<T> listObject){
        if(listObject==null) {
            Logger.e(TAG+" putListObject(): null list");
            return this;
        }

        // Create DB Key
        String LIST_OBJ_KEY = listKey(key);

        // Serialize Object to String
        Gson gson = new Gson();
        String jsonObj = gson.toJson(listObject);

        // Save serialized object
        tempMap.put(LIST_OBJ_KEY, jsonObj);
        return this;
    }






    /*==========================[ DB GET ]==========================*/

    /**
     * ************************************************************************
     * getString()
     * ************************************************************************
     * - Retrieve a string value by key.
     * ------------------------------------------------------------------------
     * @param key          The identifier key.
     * @param defaultValue The default value if the key is not found.
     * @return The stored string value, or defaultValue if not found.
     */
    public String getString(String key, String defaultValue){
        if(cache.containsKey(key)){
            Object item = cache.get(key);
            if(item instanceof String){
                return (String) item;
            }
        }
        return defaultValue;
    }


    /**
     * ************************************************************************
     * getInt()
     * ************************************************************************
     * - Retrieve an integer value by key.
     * ------------------------------------------------------------------------
     * @param key          The identifier key.
     * @param defaultValue The default value if not found.
     * @return The stored integer, or defaultValue if not found.
     */
    public Integer getInt(String key, Integer defaultValue) {
        if(cache.containsKey(key)){
            Object item = cache.get(key);
            if(item instanceof Integer){
                return (Integer) item;
            }
        }
        return defaultValue;
    }

    /**
     * ************************************************************************
     * getBoolean()
     * ************************************************************************
     * - Retrieve a boolean value by key.
     * ------------------------------------------------------------------------
     * @param key          The identifier key.
     * @param defaultValue The default value if not found.
     * @return The stored boolean, or defaultValue if not found.
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        if(cache.containsKey(key)){
            Object item = cache.get(key);
            if(item instanceof Boolean){
                return (Boolean) item;
            }
        }
        return defaultValue;
    }

    /**
     * ************************************************************************
     * getFloat()
     * ************************************************************************
     * - Retrieve a float value by key.
     * ------------------------------------------------------------------------
     * @param key          The identifier key.
     * @param defaultValue The default value if not found.
     * @return The stored float, or defaultValue if not found.
     */
    public Float getFloat(String key, Float defaultValue) {
        if(cache.containsKey(key)){
            Object item = cache.get(key);
            if(item instanceof Float){
                return (Float) item;
            }
        }
        return defaultValue;
    }

    /**
     * ************************************************************************
     * getLong()
     * ************************************************************************
     * - Retrieve a long value by key.
     * ------------------------------------------------------------------------
     * @param key          The identifier key.
     * @param defaultValue The default value if not found.
     * @return The stored long, or defaultValue if not found.
     */
    public Long getLong(String key, Long defaultValue) {
        if(cache.containsKey(key)){
            Object item = cache.get(key);
            if(item instanceof Long){
                return (Long) item;
            }
        }
        return defaultValue;
    }

    /**
     * ************************************************************************
     * getObject()
     * ************************************************************************
     * - Retrieve a deserialized object by key.
     * ------------------------------------------------------------------------
     * @param key       The identifier key.
     * @param classType The class to deserialize to.
     * @return The deserialized object, or null if not found.
     */
    public <T> @Nullable T getObject(String key, Class<T> classType){
        // Create DB Key
        String OBJ_KEY = objectKey(key);

        // Retrieve the serialized object from cache
        String serialized = readSerialized(OBJ_KEY);

        // Check if object found
        if(serialized==null) {
            return null;
        }

        // Deserialize String to Object
        Gson gson = new Gson();
        return gson.fromJson(serialized, classType);
    }

    /**
     * ************************************************************************
     * getListObject()
     * ************************************************************************
     * - Retrieve a deserialized list of objects by key.
     * - classType is still required, because Gson needs the element type to
     *   rebuild List<T>. It is no longer part of the storage key.
     * - If nothing is found under the current key, this looks once under the
     *   pre-migration key (prefix + class name + key), moves the value over,
     *   and drops the old row. Data saved by older versions of the library
     *   keeps working without the caller doing anything.
     * ------------------------------------------------------------------------
     * @param key       The identifier key.
     * @param classType The element class to deserialize to.
     * @return The deserialized list, or null if not found.
     */
    public <T> @Nullable List<T> getListObject(String key, Class<T> classType){

        // Create DB Key
        String LIST_OBJ_KEY = listKey(key);

        // Retrieve the serialized list from cache
        String serialized = readSerialized(LIST_OBJ_KEY);

        // Not found: look for a value written by an older version
        if(serialized==null){
            String legacyKey = legacyListKey(key, classType);
            serialized = readSerialized(legacyKey);
            if(serialized!=null){
                tempMap.put(LIST_OBJ_KEY, serialized);
                apply();
                remove(legacyKey);
            }
        }

        // Check if list found
        if(serialized==null) {
            return null;
        }

        // Deserialize String to List
        Gson gson = new Gson();
        Type type = TypeToken.getParameterized(List.class, classType).getType();
        return gson.fromJson(serialized, type);
    }


    /*==========================[ DELETE ]==========================*/

    /**
     * ************************************************************************
     * remove()
     * ************************************************************************
     * - Remove a data entry by key from cache and disk.
     * ------------------------------------------------------------------------
     * @param key The identifier key of the item to remove.
     */
    public synchronized void remove(String key) {
        // Remove the entry from cache
        cache.remove(key);

        // Remove the entry from disk
        executors.execute(()->{
            try (SQLiteDatabase db = getWritableDatabase()) {
                String query = "DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_KEY + " = ?";
                db.execSQL(query, new Object[]{key});
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    /**
     * ************************************************************************
     * removeObject()
     * ************************************************************************
     * - Remove a saved object by key (adds PREFIX_OBJ internally).
     * ------------------------------------------------------------------------
     * @param key The object identifier key.
     */
    public void removeObject(String key){
        // Remove the object by its prefixed key
        remove(objectKey(key));
    }

    /**
     * ************************************************************************
     * removeListObject()
     * ************************************************************************
     * - Remove a saved list of objects by key (adds PREFIX_LIST internally).
     * - This builds the same key that putListObject() writes.
     * ------------------------------------------------------------------------
     * @param key The list identifier key.
     * @apiNote A list written by an older version of the library sits under a
     *          key that includes the element class name. Call getListObject()
     *          first, which migrates it, to clear that one too.
     */
    public void removeListObject(String key){
        // Remove the list by its prefixed key
        remove(listKey(key));
    }





    /*==========================[ PRIVATE ]==========================*/

    /**
     * ************************************************************************
     * objectKey() (Private)
     * ************************************************************************
     * - Build the storage key for a single object.
     * - One place builds it, so put, get and remove cannot drift apart.
     * ------------------------------------------------------------------------
     * @param key The identifier key passed by the caller.
     * @return The prefixed storage key.
     */
    private static String objectKey(String key){
        return PREFIX_OBJ + key;
    }

    /**
     * ************************************************************************
     * listKey() (Private)
     * ************************************************************************
     * - Build the storage key for a list of objects.
     * - One place builds it, so put, get and remove cannot drift apart.
     * ------------------------------------------------------------------------
     * @param key The identifier key passed by the caller.
     * @return The prefixed storage key.
     */
    private static String listKey(String key){
        return PREFIX_LIST + key;
    }

    /**
     * ************************************************************************
     * legacyListKey() (Private)
     * ************************************************************************
     * - Rebuild the list key used before the class name was dropped.
     * - Only for reading and cleaning up old rows. Nothing writes this key.
     * ------------------------------------------------------------------------
     * @param key       The identifier key passed by the caller.
     * @param classType The element class.
     * @return The old storage key.
     */
    private static String legacyListKey(String key, Class<?> classType){
        return PREFIX_LIST + classType.getName() + key;
    }

    /**
     * ************************************************************************
     * readSerialized() (Private)
     * ************************************************************************
     * - Read a serialized JSON string out of the cache.
     * ------------------------------------------------------------------------
     * @param storageKey The full prefixed key.
     * @return The stored JSON string, or null if absent or not a string.
     */
    private @Nullable String readSerialized(String storageKey){
        Object item = cache.get(storageKey);
        return (item instanceof String) ? (String) item : null;
    }

    /**
     * ************************************************************************
     * insertMap() (Private)
     * ************************************************************************
     * - Insert a map of data into the SQLite database.
     * ------------------------------------------------------------------------
     * @param dataSet The data map to insert.
     */
    private synchronized void insertMap(Map<String, Object> dataSet){
        if(dataSet==null || dataSet.isEmpty()){
            return;
        }

        // Create a copy of data before handling it inside threads
        AtomicReference<ConcurrentHashMap<String, Object>> dataSetCopy = new AtomicReference<>(new ConcurrentHashMap<>(dataSet));
        // Make sure that given map is cleaned
        dataSet.clear();

        // Add the map items to database
        executors.execute(()->{
            try (SQLiteDatabase db = getWritableDatabase()) {
                for (Map.Entry<String, Object> data : dataSetCopy.get().entrySet()) {
                    try {
                        // Encrypt first: a null result means we could not encrypt,
                        // so skip the row rather than store a null that fails on load.
                        String original_val = String.valueOf(data.getValue());
                        String final_val = (ENABLE_ENCRYPTION) ? CryptoUtil.cipherEncrypt(original_val) : original_val;
                        if (final_val == null) {
                            Logger.e(TAG + " insertMap(): skip \"" + data.getKey() + "\", encryption returned null");
                            continue;
                        }

                        ContentValues cv = new ContentValues();
                        cv.put(COLUMN_KEY, data.getKey());
                        cv.put(COLUMN_DATA_TYPE, data.getValue().getClass().getSimpleName());
                        cv.put(COLUMN_DATA_VALUE, final_val);
                        // CONFLICT_REPLACE relies on COLUMN_KEY being the primary key, see onCreate().
                        db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                        cv.clear();
                    } catch (Exception e) {
                        Logger.e(TAG + " insertMap(): error inserting \"" + data.getKey() + "\": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Clean the copied map
            dataSetCopy.get().clear();
            dataSetCopy.set(null);
        });
    }

    /**
     * ************************************************************************
     * getAll()
     * ************************************************************************
     * - Return all data saved in SqlPreferences.
     * - This method executes synchronously (may block the calling thread).
     * - All data types are saved as strings and converted back to their
     *   original types.
     * ------------------------------------------------------------------------
     * @warning this method should not return null values (or keys),
     *          Otherwise ConcurrentHashMap (cache) will throw NPE exception.
     * - A row that cannot be decrypted (older scheme, or a lost Keystore key)
     *   is skipped. The cache is a ConcurrentHashMap and will not accept a
     *   null value, so an unreadable row is dropped, never loaded.
     * ------------------------------------------------------------------------
     * @return A map of all stored key-value pairs.
     */
    public  <T> Map<String, T> getAll() {
        Map<String, T> dataSet = new HashMap<>();

        try (SQLiteDatabase db = getWritableDatabase()) {
            Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_KEY, COLUMN_DATA_VALUE, COLUMN_DATA_TYPE}, null, null, null, null, null);

            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                String columnResult = (ENABLE_ENCRYPTION) ? CryptoUtil.cipherDecrypt(cursor.getString(1)) : cursor.getString(1);
                String type = cursor.getString(2);

                // Unreadable row (failed decrypt, or a stored null). Skip it so the
                // cache never receives a null value.
                if (columnResult == null) {
                    continue;
                }

                // Convert object type from string to its original
                Object value;
                try {
                    if (type.equals(String.class.getSimpleName())) {
                        value = columnResult; // stays null if decryption returned null
                    } else if (type.equals(Boolean.class.getSimpleName())) {
                        value = Boolean.parseBoolean(columnResult);
                    } else if (type.equals(Integer.class.getSimpleName())) {
                        value = Integer.parseInt(columnResult);
                    } else if (type.equals(Long.class.getSimpleName())) {
                        value = Long.parseLong(columnResult);
                    } else if (type.equals(Float.class.getSimpleName())) {
                        value = Float.parseFloat(columnResult);
                    } else if (type.equals(Double.class.getSimpleName())) {
                        value = Double.parseDouble(columnResult);
                    } else {
                        // Unsupported type, skip this entry
                        continue;
                    }
                } catch (Exception e) {
                    // Cannot parse value, skip this entry
                    continue;
                }

                dataSet.put(key, (T) value);
            }
            cursor.close();
            return dataSet;
        }catch (Exception e){
            e.printStackTrace();
            return dataSet;
        }

    }
}