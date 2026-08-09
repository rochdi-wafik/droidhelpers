package com.iorgana.droidhelpers.db;

import android.app.Application;
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
import com.iorgana.droidhelpers.utils.Utils;
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
 * - Data is encrypted using AES by default.
 * - Can be disabled via SqlPreferences.ENABLE_ENCRYPTION = false.
 * - Even if the database file is not public, it can be read on rooted
 *   devices.
 * ------------------------------------------------------------------------
 * @implNote The library source (including DEFAULT_SECRET_KEY) is public on
 *           GitHub. Always call setSecretKey() with an app-specific key.
 * @author Rochdi Wafik
 * @lastUpdate 08-08-2026
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
     *   unique constraint, so CONFLICT_REPLACE had no conflict to act on and
     *   every apply() appended a new row instead of replacing the old one.
     */
    private static final String DATABASE_NAME = "sql_preferences.db";
    private static final int DATABASE_VERSION = 2;
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
     * - PREFIX_LIST_LEGACY_* below documents the old layout, kept only so
     *   getListObject() can migrate rows written by older versions.
     */
    private static final String PREFIX_OBJ = "pref_obj_";
    private static final String PREFIX_LIST = "pref_list_obj_";

    /**
     * Encryption
     * ------------------------------------------------------------------------
     * - Rooted devices may access the saved data, so encryption is recommended.
     * - Disable via SqlPreferences.ENABLE_ENCRYPTION = false before init.
     * @implNote The library source (including DEFAULT_SECRET_KEY) is public on
     *           GitHub. Always call setSecretKey() with an app-specific key
     *           before first use for meaningful encryption.
     */
    public static final String DEFAULT_SECRET_KEY = "Ser5@3h6K#t5?f&5";
    public static String SECRET_KEY = DEFAULT_SECRET_KEY;
    public static boolean ENABLE_ENCRYPTION = true;

    /**
     * Caching
     * ------------------------------------------------------------------------
     * - cache: holds saved data in memory (RAM).
     * - tempMap: holds data added by put___() until apply() is called.
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

    Application context;
    Boolean allowSaveNull = true; // we can assign null to an item


    /**
     * ------------------------------------------------------------------------
     * Constructor
     * ------------------------------------------------------------------------
     * - Use getInstance() to get an instance
     * @param context any context
     */
    public SqlPreferences(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = (Application) context.getApplicationContext();
    }

    /**
     * ---------------------------------------------------------------------------------
     *  Get Instance (Singleton)
     * ---------------------------------------------------------------------------------
     * - Returns the singleton instance of SqlPreferences.
     * - Initializes with the default secret key if the instance is not yet created.
     * - Automatically ensures data is loaded into the in-memory cache synchronously
     *   if it hasn't been loaded yet.
     *
     * @param context Any valid context (will be safely converted to ApplicationContext)
     * @return The singleton SqlPreferences instance
     * @apiNote Do not close the returned instance. See the class header.
     */
    public static SqlPreferences getInstance(Context context) {
        return getInstance(context, null);
    }

    /**
     * ---------------------------------------------------------------------------------
     *  Get Instance (Singleton with Custom Secret Key)
     * ---------------------------------------------------------------------------------
     * - Returns the singleton instance of SqlPreferences.
     * - Initializes with the provided custom secret key if the instance is not yet created.
     * - The secret key must be 16, 24, or 32 bytes long (128, 192, or 256 bits).
     * - Automatically ensures data is loaded into the in-memory cache synchronously
     *   if it hasn't been loaded yet.
     *
     * @param context   Any valid context (will be safely converted to ApplicationContext)
     * @param secretKey Custom secret key for encryption. Pass null to use the default key.
     * @return The singleton SqlPreferences instance
     * @throws IllegalArgumentException if the secret key length is invalid (in debug mode)
     *
     * @implNote Singleton Behavior: The first call to any getInstance() or init() method
     *           dictates the configuration (including the secret key). Subsequent calls
     *           with different keys will be silently ignored, as the instance is already created.
     */
    public static SqlPreferences getInstance(Context context, String secretKey) {
        if (INSTANCE == null) {
            synchronized (SqlPreferences.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SqlPreferences(context.getApplicationContext());
                    if (secretKey != null) {
                        INSTANCE.setSecretKey(secretKey);
                    } else {
                        warnIfUsingDefaultSecretKey();
                    }
                }
            }
        }

        // Check if data is loaded into in-memory (Cache).
        // If already loaded (e.g., via init()), this returns immediately without blocking.
        INSTANCE.initSync();

        return INSTANCE;
    }

    /**
     * ************************************************************************
     * warnIfUsingDefaultSecretKey() (Private)
     * ************************************************************************
     * - Log a warning if the default secret key is being used.
     * - The library source (including DEFAULT_SECRET_KEY) is public on GitHub.
     */
    private static void warnIfUsingDefaultSecretKey(){
        if(ENABLE_ENCRYPTION && DEFAULT_SECRET_KEY.equals(SECRET_KEY)){
            String err = "SqlPreferences: using the library's default SECRET_KEY, which is " +
                    "public (this library's source is on GitHub). Call setSecretKey() with " +
                    "your own app-specific key before first use, or your \"encrypted\" data " +
                    "is only as safe as a key anyone can read from the repo.";
            Logger.e(TAG+" warnIfUsingDefaultSecretKey(): "+err);
        }
    }

    /**
     * ************************************************************************
     * getCurrentSecretKey()
     * ************************************************************************
     * - Get the current secret key used for encryption.
     * ------------------------------------------------------------------------
     * @return The current secret key string.
     */
    public String getCurrentSecretKey() {
        return SECRET_KEY;
    }



    /**
     * ************************************************************************
     * onCreate()
     * ************************************************************************
     * - Called when the database is created for the first time.
     * - Creates the preferences table.
     * - COLUMN_KEY is the PRIMARY KEY, which is what makes CONFLICT_REPLACE
     *   in insertMap() actually replace an existing row instead of adding a
     *   duplicate one.
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
        // Logger.i(TAG + " onCreate(): $Sql Table Has been created");
    }


    /**
     * ************************************************************************
     * onUpgrade()
     * ************************************************************************
     * - Called when the database version is increased.
     * - Copies the existing rows into a table that has COLUMN_KEY as PRIMARY
     *   KEY, then swaps it in.
     * - Dropping the table here instead would delete everything the host app
     *   had saved, on every device, the moment it picks up a new version of
     *   this library.
     * - INSERT OR REPLACE collapses the duplicate rows left behind by
     *   version 1, where the missing unique constraint made every apply()
     *   append instead of replace. The last row wins, which matches what
     *   getAll() already returned.
     * ------------------------------------------------------------------------
     * @param db         The SQLite database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String tempTable = TABLE_NAME + "_v2";

            db.execSQL("DROP TABLE IF EXISTS " + tempTable);
            db.execSQL("CREATE TABLE " + tempTable + " ("
                    + COLUMN_KEY + " TEXT PRIMARY KEY, "
                    + COLUMN_DATA_TYPE + " TEXT, "
                    + COLUMN_DATA_VALUE + " TEXT)");

            db.execSQL("INSERT OR REPLACE INTO " + tempTable
                    + " (" + COLUMN_KEY + ", " + COLUMN_DATA_TYPE + ", " + COLUMN_DATA_VALUE + ") "
                    + "SELECT " + COLUMN_KEY + ", " + COLUMN_DATA_TYPE + ", " + COLUMN_DATA_VALUE
                    + " FROM " + TABLE_NAME);

            db.execSQL("DROP TABLE " + TABLE_NAME);
            db.execSQL("ALTER TABLE " + tempTable + " RENAME TO " + TABLE_NAME);
            // Logger.i(TAG + " onUpgrade(): migrated to primary key layout");
        }
    }

    /**
     * ************************************************************************
     * init() (Async)
     * ************************************************************************
     * - Load data into the cache in the background (uses default secret key).
     * - Call this at app startup (e.g., Application.onCreate()).
     * ------------------------------------------------------------------------
     * @param anyContext      Any valid context.
     * @param onLoadListener  Optional callback when loading is complete.
     */
    public static void init(Context anyContext, @Nullable OnLoadListener onLoadListener) {
        // Delegate to the overloaded method with a null secret key
        init(anyContext, null, onLoadListener);
    }

    /**
     * ************************************************************************
     * init() (Async) with Custom Secret Key
     * ************************************************************************
     * - Load data into the cache in the background using a custom secret key.
     * - Recommended way to initialize the library with a custom key.
     * ------------------------------------------------------------------------
     * @param anyContext      Any valid context.
     * @param secretKey       Custom secret key (16, 24, or 32 bytes), or null
     *                        to use the default key.
     * @param onLoadListener  Optional callback when loading is complete.
     */
    public static void init(Context anyContext, @Nullable String secretKey, @Nullable OnLoadListener onLoadListener) {
        // Create Instance (Double-checked locking)
        if (INSTANCE == null) {
            synchronized (SqlPreferences.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SqlPreferences(anyContext.getApplicationContext());
                    if (secretKey != null) {
                        INSTANCE.setSecretKey(secretKey);
                    } else {
                        warnIfUsingDefaultSecretKey();
                    }
                }
            }
        }

        // Execute background loading
        executors.execute(() -> {
            // Load all data from Sql to Cache
            if (INSTANCE.cache.isEmpty()) {
                // Logger.d(TAG + " init(): Data not loaded to cache yet, Loading in background");
                INSTANCE.cache.putAll(INSTANCE.getAll());
                // Logger.d(TAG + " init(): Data has been loaded to cache");
            }

            // Notify listener on the same background thread (or post to main thread if preferred)
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
        // Load all data from Sql to Cache
        if(cache.isEmpty()){
            // Logger.d(TAG + " initSync(): Data not loaded to cache yet, Loading...");
            cache.putAll(this.getAll());
            // Logger.d(TAG + " initSync(): Data has been loaded");
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
        // Logger.d(TAG + " apply(): number of item to saves: "+dataToWrite.size());

        // Add data in temp map to the Cache
        if(!tempMap.isEmpty()){
            cache.putAll(tempMap);
            tempMap.clear();
        }

        // Add To Sql DB:
        // insertMap() will Write the data to the disk (sql) in background
        this.insertMap(dataToWrite);
        // Clear data to write
        // insertMap() will create a copy once receive the map,
        // so we can clear the map immediately, since insertMap() has its copy
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
                // Logger.d(TAG + " clear(): data has been removed from disk");
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
        // Logger.i(TAG + " putObject(): "+object.getClass().getSimpleName()+" | data = "+jsonObj);

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

        // Logger.i(TAG + " putListObject(): size = "+listObject.size()+" | data = "+jsonObj);

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
            // Logger.i(TAG + " getObject(): Object "+key+" is not found!");
            return null;
        }

        // Deserialize the object to its original type
        // Logger.i(TAG + " getObject(): "+classType.getSimpleName()+" | data = "+serialized);
        Gson gson = new Gson();

        // Deserialize String to Object
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
                // Logger.i(TAG + " getListObject(): migrating legacy key "+legacyKey);
                tempMap.put(LIST_OBJ_KEY, serialized);
                apply();
                remove(legacyKey);
            }
        }

        // Check if list found
        if(serialized==null) {
            // Logger.i(TAG + " getListObject(): List Object "+key+" is not found!");
            return null;
        }

        // Deserialize the list to its original type
        // Logger.i(TAG + " getListObject(): "+classType.getSimpleName()+" | data = "+serialized);
        Gson gson = new Gson();
        Type type = TypeToken.getParameterized(List.class, classType).getType();

        // Deserialize String to List
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
                // Logger.e(TAG + " remove(): Unable to perform delete operation");
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
     * - This now builds the same key that putListObject() writes, which it
     *   did not do before: it used to skip the class name that put and get
     *   both included, so the delete matched no row and the data stayed.
     * ------------------------------------------------------------------------
     * @param key The list identifier key.
     * @apiNote A list written by an older version of the library sits under a
     *          key that includes the element class name. Use
     *          removeListObject(String, Class) to clear that one too, or call
     *          getListObject() first, which migrates it.
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
     * - Remove this method, and its two call sites, once enough time has
     *   passed for installed apps to have read their lists back at least
     *   once.
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
            // Logger.w(TAG + " insertMap(): dataSet is empty or null");
            return;
        }
        //// Logger.d(TAG + " insertMap(): number of data to save in db: "+dataSet.size());

        // Create a copy of data before handle it inside threads
        AtomicReference<ConcurrentHashMap<String, Object>> dataSetCopy = new AtomicReference<>(new ConcurrentHashMap<>(dataSet));
        // Make sure that given map is cleaned
        dataSet.clear();

        // Add the map items to database
        executors.execute(()->{
            try (SQLiteDatabase db = getWritableDatabase()) {
                for (Map.Entry<String, Object> data : dataSetCopy.get().entrySet()) {
                    try {
                        ContentValues cv = new ContentValues();
                        // Put key
                        cv.put(COLUMN_KEY, data.getKey());
                        // Put data type (ClassName)
                        cv.put(COLUMN_DATA_TYPE, data.getValue().getClass().getSimpleName());
                        // Put value (Check if encryption needed)
                        String original_val = String.valueOf(data.getValue());
                        String final_val = (ENABLE_ENCRYPTION) ? CryptoUtil.cipherEncrypt(original_val, SECRET_KEY) : original_val;
                        cv.put(COLUMN_DATA_VALUE, final_val);
                        // Add to Database.
                        // CONFLICT_REPLACE relies on COLUMN_KEY being the primary key,
                        // see onCreate().
                        //// Logger.d(TAG + " insertMap(): Insert map: " + cv.toString());
                        db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                        cv.clear();
                    } catch (Exception e) {
                        // Logger.e(TAG + " insertMap(): Error inserting data into database", e);
                    }
                }
            } catch (Exception e) {
                // Logger.e(TAG + " insertMap(): Error getting writable database", e);
                e.printStackTrace();
            }

            // Clean the copied map:
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
     * @return A map of all stored key-value pairs.
     */
    public  <T> Map<String, T> getAll() {
        Map<String, T> dataSet = new HashMap<>();


        try (SQLiteDatabase db = getWritableDatabase()) {
            Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_KEY, COLUMN_DATA_VALUE, COLUMN_DATA_TYPE}, null, null, null, null, null);

            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                String columnResult = (ENABLE_ENCRYPTION) ? CryptoUtil.cipherDecrypt(cursor.getString(1), SECRET_KEY) : cursor.getString(1);
                String type = cursor.getString(2);

                // Convert object type from string to its original
                Object value = null;
                try {
                    if (type.equals(String.class.getSimpleName())) {
                        value = columnResult;
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
                        // if the type is not supported, skip this entry
                        continue;
                    }
                } catch (Exception e) {
                    // if we cant parse value, skip this entry
                    continue;
                }
                dataSet.put(key, (T) value);
            }
            cursor.close();
            // Logger.d(TAG + " getAll(): dataset = "+dataSet);
            return dataSet;
        }catch (Exception e){
            e.printStackTrace();
            return dataSet;
        }

    }


    /**
     * ************************************************************************
     * setSecretKey() (Private)
     * ************************************************************************
     * - Set the encryption secret key.
     * - Key must be 16, 24, or 32 bytes long (128, 192, or 256 bits).
     * ------------------------------------------------------------------------
     * @param secretKey The secret key string.
     */
    private void setSecretKey(String secretKey) {
        if (secretKey == null) {
            return; // Safeguard against null
        }

        if (secretKey.length() != 16 && secretKey.length() != 24 && secretKey.length() != 32) {
            String err = "Secret Key must be 16, 24, or 32 bytes long. Falling back to default.";
            Logger.e(TAG + " setSecretKey(): " + err);
            if (Utils.isDebuggingMode(context)) {
                throw new IllegalArgumentException(err);
            }
            // In production, it does nothing here, leaving SECRET_KEY as DEFAULT_SECRET_KEY.
        } else {
            SECRET_KEY = secretKey;
            ENABLE_ENCRYPTION = true;
        }
    }
}