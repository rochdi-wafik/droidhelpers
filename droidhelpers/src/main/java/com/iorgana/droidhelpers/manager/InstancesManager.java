package com.iorgana.droidhelpers.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * ************************************************************************
 * InstancesManager
 * ************************************************************************
 * - Manages object instances by class for singleton-like access.
 * ------------------------------------------------------------------------
 * @deprecated This class needs to be reviewed and updated.
 */
public class InstancesManager {
    private static volatile InstancesManager INSTANCE;

    Map<Class<?>, Object> instanceList = new ConcurrentHashMap<>();


    /**
     * ************************************************************************
     * InstancesManager (Constructor)
     * ************************************************************************
     * - Use getInstance() for singleton access.
     * - Use this constructor for non-singleton usage.
     */
    public InstancesManager(){}



    /**
     * ************************************************************************
     * getInstance()
     * ************************************************************************
     * - Get the singleton instance of InstancesManager.
     * ------------------------------------------------------------------------
     * @return The singleton InstancesManager instance.
     */
    public static InstancesManager getInstance() {
        if (INSTANCE == null) {
            synchronized (InstancesManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new InstancesManager();
                }
            }
        }
        return INSTANCE;
    }


    /**
     * ************************************************************************
     * add()
     * ************************************************************************
     * - Add an instance only if it does not already exist in the list.
     * ------------------------------------------------------------------------
     * @param object The instance to add.
     */
    public <T> void add(T object){
        Class<?> mClass = object.getClass();
        if(!instanceList.containsKey(mClass)){
            instanceList.put(mClass, object);
        }
    }


    /**
     * ************************************************************************
     * get()
     * ************************************************************************
     * - Get a saved instance by its class type.
     * ------------------------------------------------------------------------
     * @param instanceClass The class of the instance to retrieve.
     * @return The instance, or null if not found.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> instanceClass){
        Object obj = instanceList.get(instanceClass);
        if(obj!=null && instanceClass.isInstance(obj)){
            return (T) obj;
        }
        return null;
    }

    /**
     * ************************************************************************
     * get() with default
     * ************************************************************************
     * - Get a saved instance by its class type, or return a default value.
     * ------------------------------------------------------------------------
     * @param instanceClass  The class of the instance to retrieve.
     * @param defaultInstance The default value if no instance is found.
     * @return The saved instance, or defaultInstance if not found.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> instanceClass, T defaultInstance){
        Object obj = instanceList.get(instanceClass);
        if(obj!=null && instanceClass.isInstance(obj)){
            return (T) obj;
        }
        return defaultInstance;
    }

    /**
     * ************************************************************************
     * addAndGet()
     * ************************************************************************
     * - Add an instance if not already saved, then return it.
     * ------------------------------------------------------------------------
     * @param instance The instance to add and/or retrieve.
     * @return The instance.
     */
    @SuppressWarnings("unchecked")
    public <T> T addAndGet(T instance){
        // Fixed: this previously always returned the passed-in instance unconditionally
        // (a debug short-circuit left in from a prior "component cycle" bug hunt),
        // so it never actually saved or reused anything.
        // Check if already saved, if not? save it first.
        Class<?> mClass = instance.getClass();
        if (!instanceList.containsKey(mClass)) {
            add(instance);
        }
        return (T) get(instance.getClass());
    }


    /**
     * ************************************************************************
     * replace()
     * ************************************************************************
     * - Replace an existing instance with a new instance of the same type.
     * ------------------------------------------------------------------------
     * @param newInstance The new instance to replace with.
     * @return The new instance.
     */
    public <T> T replace(T newInstance){
        // Fixed: this previously always returned newInstance unconditionally
        // (a debug short-circuit left in from a prior "component cycle" bug hunt),
        // so the saved instance was never actually replaced.
        // Get old fragment
        try{
            Class<?> mClass = newInstance.getClass();
            // Replace it with new fragment
            Object obj = instanceList.get(mClass);
            if(obj!=null && mClass.isInstance(obj)){
                instanceList.remove(mClass);
                instanceList.put(mClass, newInstance);
            }
        }catch (Exception ignored){
            // ignored
        }

        return newInstance;
    }

    /**
     * ************************************************************************
     * remove()
     * ************************************************************************
     * - Remove a saved instance by its class type.
     * ------------------------------------------------------------------------
     * @param instanceClass The class of the instance to remove.
     */
    public <T> void remove(Class<T> instanceClass){
        try{
            instanceList.remove(instanceClass);
        }catch (Exception ignored){
            // ignored
        }
    }

    /**
     * ************************************************************************
     * removeAll()
     * ************************************************************************
     * - Remove all saved instances.
     */
    public void removeAll(){
        try {
            instanceList.clear();
        }catch (Exception ignored){
            // ignored
        }
    }

}