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
import com.iorgana.droidhelpers.crypto.StringCrypto;
import com.orhanobut.logger.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 *          Sql Preferences
 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * - SqlPreferences is an SQLite wrapper used to store, get, and manage data.
 * - SqlPreferences acts like SharedPreferences but uses SQL instead of XML.
 * - SqlPreferences uses caching, which gives you speed and flexibility.
 *
 * --------------------------------------------------------------------------------
 *  Caching:
 * --------------------------------------------------------------------------------
 * - Normally, SQLite database performs I/O operations to write and read from disk.
 *   These I/O operations may take longer time and must be done in separate thread(s),
 *   because if we perform I/O on the main thread, it may block or freeze the application.
 * - If the user wants to save data and get it immediately, they can't, because the data may
 *   still be written in the background to the disk.
 * - Usually, the user may need to pass a callback listener to get notified when
 *   data is saved or fetched. But as we said, we may want to save and get data immediately
 *   without worrying about concurrency (since I/O is executed in the background).
 * - So, we need a way to save and get updated data even if it’s not written yet. This
 *   can be implemented using a caching system.
 * - Caching: instead of saving data directly to disk, we keep the data in memory
 *   (RAM), and then write it in a background task.
 *   So when the user performs a save action, they can immediately retrieve it from cache even
 *   if it hasn't been saved yet to disk (SQLite DB).
 * - In this way, the user can save and get data immediately using the UI/main thread.
 * - When the app is opened, the wrapper will load the data from disk to the cache, so that
 *   it will be available in the main thread when the user needs it.
 *
 * - To load data to cache when the app starts, SqlPreferences.init() is used.
 * - init() must be called before anything, usually in Application->onCreate().
 * - Once init() is called, it will load the data to cache in a background thread, so that
 *   the app startup will not be delayed.
 * - If init() is not called, when initializing the class using getInstance(), this
 *   method will check if the data is loaded into the cache.
 * - getInstance(): if data is not loaded yet using init(), it will load the data
 *   in the main thread, so we recommend loading data in the background via init().
 *
 * --------------------------------------------------------------------------------
 *  Implementation
 * --------------------------------------------------------------------------------
 * [1] Load data from disk to the cache:
 * - Call SqlPreferences.init() at the app start point, like Application.onCreate().
 * - You can optionally pass a callback to get notified when the data is fully loaded.
 *
 * [2] Initialize SqlPreferences
 * - Call SqlPreferences.getInstance() instead of using the constructor.
 *
 * [3] Save Data:
 * - Data is saved as key-value pairs; the same key is used to retrieve the data.
 * - You can save multiple data entries in a chain, then call apply() to commit changes.
 * - You can save various data types like String, Integer, Float, Double, etc.
 * - Example: SqlPreferences.getInstance()
 *                          .putString("my_name", "Sami")
 *                          .putDouble("weight", 35.4)
 *                          .putBoolean("isAdmin", true)
 *                          .apply();
 * - You can also save serializable objects or a list of serializable objects.
 * - Example: SqlPreferences.getInstance()
 *                         .putObject("user", new User(...))
 *                         .putListObject("users", Arrays.asList(new User(...)))
 *                         .apply();
 * - Warning: data will not be saved if not committed using .apply() at the end.
 *
 * [4] Retrieve Data:
 * - We can get the saved data by the key, and also provide a default value.
 * $ String name = SqlPreferences.getInstance().getString("my_name", "Unknown");
 * $ Integer age = SqlPreferences.getInstance().getInt("my_age", null);
 * - We can get saved objects using the key and the class name:
 * $ User user = SqlPreferences.getInstance().getObject("user", User.class);
 * $ List<User> users = SqlPreferences.getInstance().getListObject("users", User.class);
 *
 * [5] Remove Data:
 * - We can remove specific data by its key:
 * $ SqlPreferences.getInstance().remove("my_name");
 * $ SqlPreferences.getInstance().removeObject("specific_user");
 * - We can clear all saved data:
 * $ SqlPreferences.getInstance().clear();
 *
 * [6] Notes:
 * - getInstance() can be used only once:
 * $ SqlPreferences db = SqlPreferences.getInstance();
 * $ db.putString("name", "Nore");
 * $ db.clear();
 * --------------------------------------------------------------------------------
 *   Security
 * --------------------------------------------------------------------------------
 * - Even if the SQLite database is not located in a public directory, it still can
 *   be accessed from rooted devices.
 * - SqlPreferences encrypts the data on save and decrypts it on fetch.
 * - By default, encryption is enabled using the AES algorithm.
 * - You can disable encryption statically before this class is initialized, using
 *   SqlPreferences.enableEncryption = false;
 *
 * --------------------------------------------------------------------------------
 * $ Developed By: Rochdi Wafik
 * $ Last Update: 25-08-2024
 *
 */

public class SqlPreferences extends SQLiteOpenHelper {
    private static final String TAG = "__SqlPreferences";
    private static volatile SqlPreferences INSTANCE;

    /**
     * Executors
     * ---------------------------------------------------------------------
     * - IO operations take take and may block ui if executed in UI Thread.
     * - Therefore we need to perform IO in background.
     * - Caching are used to let user use this class in UI Thread.
     */
    public static final ExecutorService executors = Executors.newFixedThreadPool(4);

    /**
     * Sqlite Database
     * ----------------------------------------------------------------------
     */
    private static final String DATABASE_NAME = "sql_preferences.db";
    private static final int DATABASE_VERSION = 1;
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
     * - Prefix are important, to detect is is object or list of objects
     * - Without prefix, if we save object then we save list of the same object
     *   Then the list object will replace the saved object.
     * - Example:
     * $ User user = getUser()
     * $ List<User> users = getUsers();
     * $ save(user);
     * $ save(users);
     * -> Without prefixes, when we save users, we override/replace saved user.
     * -> With prefixes, both user and users will be saved without lost
     *
     */
    private static final String PREFIX_OBJ = "pref_obj_";
    private static final String PREFIX_LIST = "pref_list_obj_";

    /**
     * Encryption
     * ------------------------------------------------------------------------
     * - Rooted devices may access the saved data.
     * - Therefore, It's recommended to keep encryption enabled. IF you want to
     *   disable it, make sure to edit it before this class initialized,
     *   usually in application->onCreate() or any app startup point
     */
    public static final String SECRET_KEY = "Ser5@3h6K#t5?f&58";
    public static final boolean ENABLE_ENCRYPTION = true;

    /**
     * Caching
     * -------------------------------------------------------------------------
     * - cache: hold saved data in-memory (ram)
     * - tempMap: hold data added by put___() until user call apply()
     */
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> tempMap = new ConcurrentHashMap<>();

    /**
     * Callback
     * -------------------------------------------------------------------------
     * - This callback used to notify us when data is loaded to the cache
     * - This callback used when we want to load data in background when app started.
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
     * --------------------------------------------------------------------------------
     *   Get Instance
     * --------------------------------------------------------------------------------
     * - Use this method to get singleton instance
     * @param context any context
     * --------------------------------------------------------------------------------
     * - When getInstance() is called, it will check if data loaded into the cache,
     * - IF data not loaded yet, this method will loaded in UI Thread.
     * - So that it's better to call init() to load data in background one app opened.
     */
    public static SqlPreferences getInstance(Context context){
        if(INSTANCE==null){
            synchronized (SqlPreferences.class){
                if(INSTANCE==null){
                    INSTANCE = new SqlPreferences(context.getApplicationContext());
                }
            }
        }

        // Check if data loaded to in-memory (Cache)
        INSTANCE.initSync();

        return INSTANCE;
    }


    /**
     * ------------------------------------------------------------------------
     * On Create
     * ------------------------------------------------------------------------
     * - This called when database create and installed
     * - We use this method to create our table(s) and any necessary data
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE IF NOT EXISTS "+TABLE_NAME+" ("
                + COLUMN_KEY +" TEXT, " // identifier
                + COLUMN_DATA_TYPE +" TEXT, " // String, Integer, etc
                + COLUMN_DATA_VALUE + " TEXT)";

        db.execSQL(query);
        // Logger.i(TAG + " onCreate(): $Sql Table Has been created");
    }


    /**
     * ------------------------------------------------------------------------
     * On Upgrade
     * ------------------------------------------------------------------------
     * - This is called when we want to upgrade db to new version
     * - This case we should to drop tables, add tables,
     *   or do anything else it needs to upgrade to the new schema version
     * - We use it to check if the table already exists. if not: create it
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
        // Logger.i(TAG + " onUpgrade(): $Sql DB has been upgraded");
    }

    /**
     * ---------------------------------------------------------------------------------
     *   Init (Async)
     * ---------------------------------------------------------------------------------
     * - Use this method once app opened to load data into cache in background.
     * - This can method accept callback to notify when data is fully loaded into cache.
     */
    public static void init(Context anyContext, @Nullable OnLoadListener onLoadListener){
        // Create Instance
        if(INSTANCE==null){
            synchronized (SqlPreferences.class){
                if(INSTANCE==null){
                    INSTANCE = new SqlPreferences(anyContext.getApplicationContext());
                }
            }
        }


        executors.execute(()->{
            // Load all data from Sql to Cache
            if(INSTANCE.cache.isEmpty()) {
                // Logger.d(TAG + " init(): Data not loaded to cache yet, Loading in background");
                INSTANCE.cache.putAll(INSTANCE.getAll());
                // Logger.d(TAG + " init(): Data has been loaded to cache");
            }

            // Notify listener
            if(onLoadListener!=null) {
                onLoadListener.onLoaded();
            }
        });
    }
    /**
     * ---------------------------------------------------------------------------------
     *   Init (Sync)
     * ---------------------------------------------------------------------------------
     * - Use this method used to load stored data into the cache memory.
     * - This method executed synchronously (in Main Thread).
     * - Everytime this class is called, this method invoked to make sure data is loaded.
     * - For better performance, use init() once app opened,
     *   so that this method will not block the main thread, since data will be already loaded
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
     * ---------------------------------------------------------------------------------
     * Allow Save Key With Nullable Value
     * ---------------------------------------------------------------------------------
     * - By-default nullable objects can be saved.
     * - IF a key saved with null object, then it will also returned as null.
     * - But sometime we don't want to save an object if it's null. (i.e: from an API)
     * @apiNote If we need to disallow save null objects, then we have to return this
     * method to it's default, by assign null
     */
    public SqlPreferences setAllowSaveNull(Boolean allowNull){
        this.allowSaveNull = allowNull;
        return this;
    }

    /**
     * ---------------------------------------------------------------------------------
     * Apply
     * ---------------------------------------------------------------------------------
     * - when we put the necessary data, it's stored in temp map (cache)
     * - Now, we move the temp map to the main map (Cache) and clear the temp map.
     * - Asynchronously, we'll also write the data to disk (to sql database)
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
     * ---------------------------------------------------------------------------------
     * Clear
     * ---------------------------------------------------------------------------------
     * - Clear data from cache and disk
     * - This will drop the table and any saved data will be erased
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



    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    /*++++++++++++++++++++++++++++++[ DB PUT ]+++++++++++++++++++++++++++++++++++*/
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/


    /**
     * ---------------------------------------------------------------------------------
     * Put String
     * ---------------------------------------------------------------------------------
     * @Note Example: putString("author", "Sami") | don't forget to call apply()
     * @param key identifier
     * @param value to save
     * ---------------------------------------------------------------------------------
     * - Put data in the temporary map,
     * - once apply() is called, put temp map to cache (Sync), then write to disk (Async)
     */
    public SqlPreferences putString(String key, String value){
        tempMap.put(key, value);
        return this;
    }


    /**
     * ---------------------------------------------------------------------------------
     *   Put Integer
     * ---------------------------------------------------------------------------------
     * @Note Example: putInt("age", 23) | don't forget to call apply()
     * @param key identifier
     * @param value to save
     * ---------------------------------------------------------------------------------
     * - Put data in the temporary map,
     * - once apply() is called, put temp map to cache (Sync), then write to disk (Async)
     */

    public SqlPreferences putInt(String key, int value){
        tempMap.put(key, value);
        return this;
    }


    /**
     * ---------------------------------------------------------------------------------
     *   Put Boolean
     * ---------------------------------------------------------------------------------
     * @Note Example: putBoolean("isAdmin", true) | don't forget to call apply()
     * @param key identifier
     * @param value to save
     * ---------------------------------------------------------------------------------
     * - Put data in the temporary map,
     * - once apply() is called, put temp map to cache (Sync), then write to disk (Async)
     */
    public SqlPreferences putBoolean(String key, Boolean value){
        tempMap.put(key, value);
        return this;
    }

    /**
     * ---------------------------------------------------------------------------------
     *   Put Float
     * ---------------------------------------------------------------------------------
     * @Note Example: putFloat("degree", 8.5) | don't forget to call apply()
     * @param key identifier
     * @param value to save
     * ---------------------------------------------------------------------------------
     * - Put data in the temporary map,
     * - once apply() is called, put temp map to cache (Sync), then write to disk (Async)
     */
    public SqlPreferences putFloat(String key, float value){
        tempMap.put(key, value);
        return this;
    }

    /**
     * ---------------------------------------------------------------------------------
     *   Put Long
     * ---------------------------------------------------------------------------------
     * @Note Example: putLong("time", 836876786845) | don't forget to call apply()
     * @param key identifier
     * @param value to save
     * ---------------------------------------------------------------------------------
     * - Put data in the temporary map,
     * - once apply() is called, put temp map to cache (Sync), then write to disk (Async)
     */
    public SqlPreferences putLong(String key, float value){
        tempMap.put(key, value);
        return this;
    }

    /**
     * ---------------------------------------------------------------------------------
     *   Put Double
     * ---------------------------------------------------------------------------------
     * @Note Example: putDouble("weight", 120.5) | don't forget to call apply()
     * @param key identifier
     * @param value to save
     * ---------------------------------------------------------------------------------
     * - Put data in the temporary map,
     * - once apply() is called, put temp map to cache (Sync), then write to disk (Async)
     */
    public SqlPreferences putDouble(String key, double value){
        tempMap.put(key, value);
        return this;
    }

    /**
     * ---------------------------------------------------------------------------------
     *   Put Object
     * ---------------------------------------------------------------------------------
     * @Note Example: User user = getUser(); putObject(User.KEY, user);
     *                don't forget to call apply()
     * @param key identifier
     * @param object Serializable object
     * ---------------------------------------------------------------------------------
     * - Put data in the temporary map,
     * - once apply() is called, put temp map to cache (Sync), then write to disk (Async)
     */
    public  <T> SqlPreferences putObject(String key, T object){

        // Create DB Key
        String OBJ_KEY = PREFIX_OBJ+/*object.getClass().getName()+*/key;

        // Serialize object to String
        Gson gson = new Gson();
        String jsonObj = gson.toJson(object);
        // Logger.i(TAG + " putObject(): "+object.getClass().getSimpleName()+" | data = "+jsonObj);

        // Save serialized object
        tempMap.put(OBJ_KEY, jsonObj);

        return this;
    }

    /**
     * ---------------------------------------------------------------------------------
     *   Put List Of Object
     * ---------------------------------------------------------------------------------
     * @Note Example: List<User> users = getUsers(); putListObject(User.KEY, users);
     *                don't forget to call apply()
     * @param key identifier
     * @param listObject List of Serializable object
     * ---------------------------------------------------------------------------------
     * - Put data in the temporary map,
     * - once apply() is called, put temp map to cache (Sync), then write to disk (Async)
     */
    public <T> SqlPreferences putListObject(String key, List<T> listObject){
        // todo: if listObject is null or empty, then get(0) will throw indexOutOfBounds
        if(listObject==null || listObject.isEmpty()) {
            Logger.e(TAG+" putListObject(): null or empty list");
            return this;
        }
        // Create DB Key
        Class<?> className = listObject.get(0).getClass();
        String LIST_OBJ_KEY = PREFIX_LIST+ className.getName()+key;

        // Serialize Object to String
        Gson gson = new Gson();
        String jsonObj = gson.toJson(listObject);

        // Logger.i(TAG + " putListObject(): "+listObject.get(0).getClass().getSimpleName()+" | data = "+jsonObj);

        // Save serialized object
        tempMap.put(LIST_OBJ_KEY, jsonObj);
        return this;
    }






    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
    /*++++++++++++++++++++++++++++++[ DB GET ]+++++++++++++++++++++++++++++++++++*/
    /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/

    /**
     * ---------------------------------------------------------------------------------
     * Get String
     * ---------------------------------------------------------------------------------
     * @apiNote Example: String city = getString("city", "rabat");
     * @param key item identifier
     * @param defaultValue if not found
     * ---------------------------------------------------------------------------------
     * - When using getInstance() or init(), the data will be loaded into the Cache.
     * - Since data is stored in cache, we'll get it from cache not from disk.
     * - (If key not exists in cache, means also not exists in disk)
     * - In this way we'll get performance and UI Thread compatibility.
     * - Which means we can use UI Thread to query our data  synchronously.
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
     * ---------------------------------------------------------------------------------
     *   Get String
     * ---------------------------------------------------------------------------------
     * @Note Example: String name = getString("author", "not set");
     * @param key identifier
     * @param defaultValue if key not saved/exists
     * @return result
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
     * ---------------------------------------------------------------------------------
     *   Get String
     * ---------------------------------------------------------------------------------
     * @Note Example: String name = getString("author", "not set");
     * @param key identifier
     * @param defaultValue if key not saved/exists
     * @return result
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
     * ---------------------------------------------------------------------------------
     *   Get String
     * ---------------------------------------------------------------------------------
     * @Note Example: String name = getString("author", "not set");
     * @param key identifier
     * @param defaultValue if key not saved/exists
     * @return result
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
     * ---------------------------------------------------------------------------------
     *   Get Long
     * ---------------------------------------------------------------------------------
     * @Note Example: String name = getString("author", "not set");
     * @param key identifier
     * @param defaultValue if key not saved/exists
     * @return result
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
     * ---------------------------------------------------------------------------------
     *   Get Long
     * ---------------------------------------------------------------------------------
     * @Note Example: User user = getObject(User.class, User.KEY);
     * @param key identifier
     * @param classType to deserialize object to that type
     * @return Object
     */
    public <T> @Nullable T getObject(String key, Class<T> classType){
        // Create DB Key
        String OBJ_KEY = PREFIX_OBJ+key;

        // Retrieve the serialized object from cache
        String serialized = null;
        if(cache.containsKey(OBJ_KEY)){
            Object item = cache.get(OBJ_KEY);
            if(item instanceof String){
                serialized = (String) item;
            }
        }

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
     * ---------------------------------------------------------------------------------
     *   Get List of Object
     * ---------------------------------------------------------------------------------
     * @Note Example: List<User> users = getListObject(User.class, User.KEY);
     * @param key identifier
     * @param classType to deserialize object to that type
     * @return List of Object
     */
    public <T> @Nullable List<T> getListObject(String key, Class<T> classType){

        // Create DB Key
        String LIST_OBJ_KEY = PREFIX_LIST+ classType.getName()+key;

        // Retrieve the serialized object from cache
        String serialized = null;
        if(cache.containsKey(LIST_OBJ_KEY)){
            Object item = cache.get(LIST_OBJ_KEY);
            if(item instanceof String){
                serialized = (String) item;
            }
        }

        // Check if object found
        if(serialized==null) {
            // Logger.i(TAG + " getListObject(): List Object "+key+" is not found!");
            return null;
        }

        // Deserialize the object to its original type
        // Logger.i(TAG + " getListObject(): "+classType.getSimpleName()+" | data = "+serialized);
        Gson gson = new Gson();
        Type type = TypeToken.getParameterized(List.class, classType).getType();

        // Deserialize String to Object
        return gson.fromJson(serialized, type);
    }


    /*++++++++++++++++++++++++++++++[ DELETE ]+++++++++++++++++++++++++++++++++++*/

    /**
     * ---------------------------------------------------------------------------------
     *   Remove
     * ---------------------------------------------------------------------------------
     * @Note Example: remove("author");
     * @param key identifier
     * ---------------------------------------------------------------------------------
     * - First remove the item from cache if isset
     * - Then remove the item from disk in background
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
     * ---------------------------------------------------------------------------------
     *  Remove Object
     * ---------------------------------------------------------------------------------
     * When object, we need to re-create the key by adding PREFIX_OBJ to the key
     */
    public void removeObject(String key){
        // Create Object Key
        String OBJ_KEY = PREFIX_OBJ+key;
        // Remove the object by key
        remove(OBJ_KEY);
    }

    /**
     * ---------------------------------------------------------------------------------
     *  Remove List Object
     * ---------------------------------------------------------------------------------
     * When list object, we need to re-create the key by adding PREFIX_LIST to the key
     */
    public void removeListObject(String key){
        // Create Object Key
        String OBJ_KEY = PREFIX_LIST+key;
        // Remove the object by key
        remove(OBJ_KEY);
    }




    /*++++++++++++++++++++++++++++++[ PRIVATE ]+++++++++++++++++++++++++++++++++++*/

    /**
     * ---------------------------------------------------------------------------------
     *   Insert Map
     * ---------------------------------------------------------------------------------
     * @param dataSet map
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
                        String final_val = (ENABLE_ENCRYPTION) ? StringCrypto.cipherEncrypt(original_val, SECRET_KEY) : original_val;
                        cv.put(COLUMN_DATA_VALUE, final_val);
                        // Add to Database
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
     * ---------------------------------------------------------------------------------
     *   Get All
     * ---------------------------------------------------------------------------------
     * - Return All data saved in SqlPreferences
     * - This method executed synchronously, which means it may block the UI if it's
     *   called from Main Thread, make sure to call it in separate thread.
     * - IF there any saved Object [putObject()], it will returned as Json (serialized).
     * - All DataTypes are saved as String, then this method convert them to its original.
     * @Note Example: Map<String, Object> dataSet = getAll();
     * @return result Map
     */
    public  <T> Map<String, T> getAll() {
        Map<String, T> dataSet = new HashMap<>();


        try (SQLiteDatabase db = getWritableDatabase()) {
            Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_KEY, COLUMN_DATA_VALUE, COLUMN_DATA_TYPE}, null, null, null, null, null);

            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                String columnResult = (ENABLE_ENCRYPTION) ? StringCrypto.cipherDecrypt(cursor.getString(1), SECRET_KEY) : cursor.getString(1);
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



    /*#################################[Test Only]##############################*/
    // helper method to deserialize a string to a serializable object
    private @Nullable Object deserialize(String serialized) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(serialized.getBytes());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
