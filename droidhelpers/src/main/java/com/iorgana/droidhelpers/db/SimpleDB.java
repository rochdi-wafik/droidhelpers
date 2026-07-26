package com.iorgana.droidhelpers.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Objects;


/**
 * ************************************************************************
 * SimpleDB
 * ************************************************************************
 * - Save and retrieve serializable objects or lists of objects using
 *   SharedPreferences and Gson for serialization.
 * - Uses prefixes (PREFIX_OBJ, PREFIX_LIST) to prevent key conflicts
 *   between single objects and lists of the same type.
 * - Encryption is enabled by default but can be disabled statically
 *   before initialization (SimpleDB.enableEncryption = false).
 * ------------------------------------------------------------------------
 * @apiNote SharedPreferences are accessible on rooted devices even if
 *          not publicly visible. Encryption is recommended.
 */
public class SimpleDB {
    private static final String TAG = "__SimpleDB__Original";
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "simple_db_pref";
    private static final String PREFIX_OBJ = "pref_obj_";
    private static final String PREFIX_LIST = "pref_list_obj_";
    private boolean allowSaveNull = false; // allow save null (means unset object)

    private static volatile SimpleDB INSTANCE;

    /**
     * Security
     * ------------------------------------------------------------------------
     * - Set this field statically before initializing this class to disable
     *   encryption.
     */
    public static boolean enableEncryption = true;


    /**
     * ************************************************************************
     * SimpleDB (Constructor)
     * ************************************************************************
     * - Create a new SimpleDB instance.
     * ------------------------------------------------------------------------
     * @param anyContext Any valid context.
     */
    public SimpleDB(Context anyContext){
        try{
            this.sharedPreferences = getSharedPreferences(anyContext);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * ************************************************************************
     * getSharedPreferences()
     * ************************************************************************
     * - Get normal or encrypted SharedPreferences based on enableEncryption.
     * ------------------------------------------------------------------------
     * @param context Any valid context.
     * @return The SharedPreferences instance.
     * @todo 'androidx.security.crypto.MasterKey' is deprecated.
     * @todo Use Harmony EncryptedSharedPreferences when deprecation is resolved.
     */
    private SharedPreferences getSharedPreferences(Context context){
        if(!enableEncryption){
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        try{
            // Create Key for encryption/decryption
            MasterKey masterKeyAlias = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Create Encrypted SharedPreferences
            return  EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    PREF_NAME,
                    masterKeyAlias,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        }catch (GeneralSecurityException | IOException e) {
            Logger.e(TAG + " getSharedPreferences(): Unable to create/get EncryptedSharedPreferences: "+e.getMessage());
            e.printStackTrace();
            Logger.d(TAG + " getSharedPreferences(): return normal SharedPreferences");
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }



    /**
     * ************************************************************************
     * getInstance()
     * ************************************************************************
     * - Get the singleton instance of SimpleDB.
     * ------------------------------------------------------------------------
     * @param anyContext Any valid context.
     * @return The singleton SimpleDB instance.
     */
    public static SimpleDB getInstance(Context anyContext){
        if(INSTANCE==null){
            synchronized (SimpleDB.class){
                if(INSTANCE==null){
                    INSTANCE = new SimpleDB(anyContext);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * ************************************************************************
     * setAllowSaveNull()
     * ************************************************************************
     * - Allow or disallow saving null values.
     * - If null is saved, the object value will be cleared.
     * ------------------------------------------------------------------------
     * @param allowSaveNull true to allow saving null values.
     * @return This SimpleDB instance for chaining.
     */
    public SimpleDB setAllowSaveNull(boolean allowSaveNull) {
        this.allowSaveNull = allowSaveNull;
        return this;
    }



    /**
     * ************************************************************************
     * saveObject()
     * ************************************************************************
     * - Save a single object by key using Gson serialization.
     * ------------------------------------------------------------------------
     * @param key    The key to identify the object.
     * @param object The object to save (any serializable type).
     */
    public <T> void saveObject(String key, T object){
        Logger.d(TAG + " saveObject(): "+key);

        if(sharedPreferences==null) return;

        // Check if allow save null
        if(Objects.isNull(object) && !allowSaveNull){
            return;
        }

        // Create Object Key
        String OBJ_KEY = PREFIX_OBJ+"_"+key;
        Logger.d(TAG + " saveObject(): original="+object.toString());

        // Convert object to String (Serialize to json)
        Gson gson = new Gson();
        String jsonObj = gson.toJson(object);

        Logger.d(TAG + " saveObject(): serialized = "+jsonObj);

        // Save serialized object
        mainThread(()-> sharedPreferences.edit().putString(OBJ_KEY, jsonObj).apply());
    }


    /**
     * ************************************************************************
     * saveListObject()
     * ************************************************************************
     * - Save a list of objects by key using Gson serialization.
     * ------------------------------------------------------------------------
     * @param key        The key to identify the list.
     * @param listObject The list of objects to save.
     */
    public <T> void saveListObject(String key, List<T> listObject){
        if(sharedPreferences==null) return;

        // Check if allow save null
        if(Objects.isNull(listObject) && !allowSaveNull) return;
        Logger.d(TAG+" saveListObject(): "+key);

        // Create Obj Key
        String OBJ_KEY = PREFIX_LIST+"_"+key;

        // Convert object to String (Serialize to json)
        Gson gson = new Gson();
        String jsonObj = gson.toJson(listObject);
        Logger.d(TAG + " saveListObject(): serialized = "+listObject);


        // Save serialized object
        mainThread(()-> sharedPreferences.edit().putString(OBJ_KEY, jsonObj).apply());
    }

    /**
     * ************************************************************************
     * getObject()
     * ************************************************************************
     * - Retrieve a saved object by key and deserialize it to the specified type.
     * ------------------------------------------------------------------------
     * @param key       The key identifying the object.
     * @param classType The class type to deserialize to.
     * @return The deserialized object, or null if not found.
     */
    public <T> @Nullable T getObject(String key, Class<T> classType){
        Logger.d(TAG+" getObject(): "+key);
        if(sharedPreferences==null) return null;

        // Create Object Key
        String OBJ_KEY = PREFIX_OBJ+"_"+key;

        // Get saved object
        String json = sharedPreferences.getString(OBJ_KEY, null);

        if(json==null){
            Logger.w(TAG + " getObject(): Null serialized ");
            return null;
        }
        Logger.d(TAG + " getObject(): serialized = "+json);

        // Deserialize object to its original type
        Gson gson = new Gson();
        return gson.fromJson(json, classType);
    }


    /**
     * ************************************************************************
     * getListObject()
     * ************************************************************************
     * - Retrieve a saved list of objects by key.
     * ------------------------------------------------------------------------
     * @param key    The key identifying the list.
     * @param mClass The class type of the list elements.
     * @return The deserialized list, or null if not found.
     */
    public <T> @Nullable List<T> getListObject(String key, Class<T> mClass){
        Logger.d(TAG+" getListObject(): "+key);
        if(sharedPreferences==null) return null;

        // Create Object Key
        String OBJ_KEY = PREFIX_LIST+"_"+key;

        // Get saved object
        String json = sharedPreferences.getString(OBJ_KEY, null);

        if(json==null){
            Logger.w(TAG + " getObject(): Null serialized ");
            return null;
        }
        Logger.d(TAG + " getObject(): serialized = "+json);

        // Deserialize object to its original type
        Gson gson = new Gson();
        Type type = TypeToken.getParameterized(List.class, mClass).getType();
        return gson.fromJson(json, type);
    }


    /**
     * ************************************************************************
     * removeObject()
     * ************************************************************************
     * - Remove a saved object by its key.
     * ------------------------------------------------------------------------
     * @param key The key identifying the object to remove.
     */
    public void removeObject(String key){
        Logger.d(TAG + " removeObject(): key="+key);
        if(sharedPreferences==null) return;

        // Create Object Key
        String OBJ_KEY = PREFIX_OBJ+"_"+key;

        // Edit preferences using the OBJ_KEY, put null value
        try{
            mainThread(()-> sharedPreferences.edit().remove(OBJ_KEY).apply());
        }catch (Exception ignored){}
    }


    /**
     * ************************************************************************
     * clear()
     * ************************************************************************
     * - Clear all saved data from SharedPreferences.
     */
    public void clear(){
        if(sharedPreferences==null) return;
        Logger.d(TAG + " clear(): clear all data");
        mainThread(()-> sharedPreferences.edit().clear().apply());
    }

    /**
     * ************************************************************************
     * mainThread()
     * ************************************************************************
     * - Execute runnable(s) on the main (UI) thread.
     * - SharedPreferences edits must be done on the UI thread.
     * ------------------------------------------------------------------------
     * @param runnable The runnable tasks to execute on the main thread.
     */
    private void mainThread(Runnable... runnable){
        Handler handler = new Handler(Looper.getMainLooper());
        for(Runnable r: runnable){
            handler.post(r);
        }
    }
}