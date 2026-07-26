package com.iorgana.droidhelpers.manager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

/**
 * ************************************************************************
 * FragmentsManager
 * ************************************************************************
 * - Manages fragment instances using the FragmentManager.
 * - Helps preserve fragment state across configuration changes.
 * - Fragments are stored using their class name as a tag.
 * ------------------------------------------------------------------------
 * @deprecated This class does not handle fragment lifecycle, only saves them.
 */
public class FragmentsManager {

    //    Map<Class<?>, Fragment> fragmentsList = new ConcurrentHashMap<>();
//    Map<Class<?>, String> keysList = new ConcurrentHashMap<>();
    FragmentManager fragmentManager;


    /**
     * ************************************************************************
     * FragmentsManager (Constructor)
     * ************************************************************************
     * - Create one instance per Activity (e.g., in onCreate()).
     * - Do not cache it statically across Activity recreation.
     * ------------------------------------------------------------------------
     *
     * @param context The FragmentActivity to get the FragmentManager from.
     */
    public FragmentsManager(FragmentActivity context) {
        this.fragmentManager = context.getSupportFragmentManager();
    }

    /*
     * Fixed: getInstance(FragmentActivity) was removed. It was a static singleton
     * that cached whichever Activity's FragmentManager was passed in first, then kept
     * returning that same (possibly destroyed) Activity's FragmentManager forever after
     * -- an Activity leak, plus a crash/no-op risk once that Activity was recreated
     * (rotation, back navigation, process death). Use `new FragmentsManager(activity)`
     * per Activity instead.
     */

    /**
     * ************************************************************************
     * add()
     * ************************************************************************
     * - Add a fragment to the FragmentManager if it does not already exist.
     * ------------------------------------------------------------------------
     *
     * @param fragment The fragment to add.
     */
    public void add(Fragment fragment) {
        Class<?> mClass = fragment.getClass();
        if (fragmentManager.findFragmentByTag(mClass.getName()) == null) {
            fragmentManager.beginTransaction().add(fragment, mClass.getName()).commit();
        }
    }


    /**
     * ************************************************************************
     * get()
     * ************************************************************************
     * - Get a previously added fragment by its class.
     * ------------------------------------------------------------------------
     *
     * @param fragmentClass The class of the fragment to retrieve.
     * @return The fragment instance, or null if not found.
     */
    public Fragment get(Class<?> fragmentClass) {
        Fragment fragment = fragmentManager.findFragmentByTag(fragmentClass.getName());
        if (fragment != null) {
            return fragment;
        } else {
            return null;
        }
//        Fragment fragment = instanceList.get(fragmentClass.getName());
//        Bundle bundle = fragmentBundles.get(fragmentClass.getName());
//
//        if(fragment!=null && bundle!=null){
//            fragment.setArguments(bundle);
//        }
//
//        return fragment;
    }

    /**
     * ************************************************************************
     * addAndGet()
     * ************************************************************************
     * - Add a fragment if not already saved, then return it.
     * ------------------------------------------------------------------------
     *
     * @param fragment The fragment to add and/or retrieve.
     * @return The fragment instance.
     */
    public Fragment addAndGet(Fragment fragment) {
        // Check if already saved, if not? save it first.
        Class<?> mClass = fragment.getClass();
        Fragment savedFragment = fragmentManager.findFragmentByTag(mClass.getName());
        if (savedFragment == null) {
            fragmentManager.beginTransaction().add(fragment, mClass.getName()).commit();
        }
        return fragmentManager.findFragmentByTag(mClass.getName());
    }


    /**
     * ************************************************************************
     * replace()
     * ************************************************************************
     * - Replace an existing fragment with a new instance of the same class.
     * ------------------------------------------------------------------------
     *
     * @param newFragment The new fragment instance.
     * @return The new fragment instance.
     */
    public Fragment replace(Fragment newFragment) {
        // Get old fragment
        Class<?> mClass = newFragment.getClass();
        Fragment savedFragment = fragmentManager.findFragmentByTag(mClass.getName());

        if (savedFragment != null) {
            // Replace it with new fragment
            fragmentManager.beginTransaction().remove(savedFragment).commit();
        }
        fragmentManager.beginTransaction().add(newFragment, mClass.getName()).commit();

        return newFragment;
    }

    /**
     * ************************************************************************
     * remove()
     * ************************************************************************
     * - Remove a fragment by its class.
     * ------------------------------------------------------------------------
     *
     * @param fragmentClass The class of the fragment to remove.
     */
    public void remove(Class<?> fragmentClass) {
        Fragment savedFragment = fragmentManager.findFragmentByTag(fragmentClass.getName());

        if (savedFragment != null) {
            // Replace it with new fragment
            fragmentManager.beginTransaction().remove(savedFragment).commit();
        }
    }

    /**
     * ************************************************************************
     * removeAll()
     * ************************************************************************
     * - Remove all fragments managed by this instance.
     */
    public void removeAll() {

    }

}