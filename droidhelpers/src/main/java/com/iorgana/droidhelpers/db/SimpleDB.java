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
 * SimpleDB
 * ---------------------------------------------------------------------------
 * This class used to save/retrieve Object or List of Object (Serializable)
 * This class not used to save/retrieve abstract ui classes like Drawable
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * - We use Gson to Serialize/Deserialize objects from/to String
 * - We use SharedPreferences to Save/Get Serialized Classes
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * [OBJ_KEY]
 * - OBJ_KEY is used to identify the object we want to save/ retrieve
 * - If the user didn't specify OBJ_KEY, the className will be used as OBJ_KEY
 *  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * [List]
 * - To prevent conflict between Object and List of the same Object,
 *   We added a prefix to object and prefix to List of Object
 * - Example: Suppose we have this Object:
 * > class User{static String OBJ_KEY = "OBJ_KEY_user"; //other fields}
 * - We'll use OBJ_KEY as key to save the object,
 * - But if we want to save a List of Object: List<User> then what?
 *   Then both User & List<User> share the same OBJ_KEY "OBJ_KEY_user" !!
 * - So that we have to add prefix for single Object and prefix for List
 *  +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 *  [Security]
 * - Although SharedPreferences is not public/visible to users
 * - But its still accessible by Rooted devices
 * - By-default: the encryption is enabled, but it can be disabled statically
 *   before initialize the class by using: SimpleDB__Original.enableEncryption=false
 *   (In application class or any start point)
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
     * - You can set these fields statically before initialize this class
     */
    public static boolean enableEncryption = true;
    public static final String SECRET_KEY = "Ser85630Klt20876"; // 128bit key



    /**
     * -------------------------------------------------------------------------
     * Constructor
     * -------------------------------------------------------------------------
     * @param anyContext use any context
     */
    public SimpleDB(Context anyContext){
        try{
            this.sharedPreferences = getSharedPreferences(anyContext);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * -------------------------------------------------------------------------
     * Get Shared Preferences
     * -------------------------------------------------------------------------
     * - Get normal shared preferences or encrypted if encryption is enabled
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
     * -------------------------------------------------------------------------
     *  Get Instance
     * -------------------------------------------------------------------------
     * - Get singleton instance
     * @param anyContext use any context
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
     * -------------------------------------------------------------------------
     *  Allow Save Null
     * -------------------------------------------------------------------------
     * - Allow save null value, we may try to save object that has null value
     * - IF data saved with null, means the object value will be cleaned.
     */
    public SimpleDB setAllowSaveNull(boolean allowSaveNull) {
        this.allowSaveNull = allowSaveNull;
        return this;
    }



    /**
     * -------------------------------------------------------------------------
     * Save an Object
     * -------------------------------------------------------------------------
     * @param object: Generic type: can pass any serializable type
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
     * Save a List of Object
     * --------------------------------------------------------------------
     * @param listObject: object
     *
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
     * Get Object
     * --------------------------------------------------------------------
     * @param classType: Object Type
     * @return object:
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
     * Remove Object
     * ----------------------------------------------------------------
     * [-] Use the key to reach the specified object
     * [-] Edit SharedPreferences, Remove the object by that Key
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
     * --------------------------------------------------------------------
     * Clear All data
     * --------------------------------------------------------------------
     */
    public void clear(){
        if(sharedPreferences==null) return;
        Logger.d(TAG + " clear(): clear all data");
        mainThread(()-> sharedPreferences.edit().clear().apply());
    }

    /**
     * Get UI Thread
     * -------------------------------------------------------------------
     * - We cannot edit or save in background thread
     * - user may forgot to call simpleDB in ui thread
     * - We can use this method to get UI Thread to use it to edit prefs
     */
    private void mainThread(Runnable... runnable){
        Handler handler = new Handler(Looper.getMainLooper());
        for(Runnable r: runnable){
            handler.post(r);
        }
    }
}
