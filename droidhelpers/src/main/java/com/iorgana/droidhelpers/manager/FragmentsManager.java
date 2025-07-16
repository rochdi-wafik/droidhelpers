package com.iorgana.droidhelpers.manager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

/**
 * Fragments Store
 * -------------------------------------------------------------------------------------------------
 * - This class wil help us save the fragment instance (Like Fields, inner classes, etc)
 * - We'll be able to protect all data stored in fragment, from being lost, like Fields
 * ^
 * - Unless we manually change the data inside the fragment. like using onCreate()
 * - We cannot prevent invoke fragment lifecycle methods, like onCreate(),
 * - Because when  we use beginTransaction().replace(), the manager invoke the lifecycle fragment methods,
 * - Instead, is necessary, we can save data in bundle and retrieve it when fragment re-launched
 * ^
 * - If we want to clear a fragment, we simply replace the exist instance with new instance.
 * - When a fragment is saved in this class, if can get it whenever we want using get()
 * -----------------------------------------------------------------------------------------------
 * [Updates]
 * - ActivityContext has no meaning since we don't want to access the activity
 * - We only want to store instances
 */
/**
 * @deprecated This class does not handle fragments lifecycle, only save them.
 */
public class FragmentsManager {
    private static volatile FragmentsManager INSTANCE;

//    Map<Class<?>, Fragment> fragmentsList = new ConcurrentHashMap<>();
//    Map<Class<?>, String> keysList = new ConcurrentHashMap<>();
    FragmentManager fragmentManager;


    /**
     * Constructor
     * -------------------------------------------------------------------------
     * [-] Use this method for none-singleton
     */
    public FragmentsManager(FragmentActivity context){
        this.fragmentManager = context.getSupportFragmentManager();
    }



    /**
     * Get Instance
     * -------------------------------------------------------------------------
     * Use this method for Singleton object
     */
    public static FragmentsManager getInstance(FragmentActivity context) {
        if (INSTANCE == null) {
            synchronized (FragmentsManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FragmentsManager(context);
                }
            }
        }
        return INSTANCE;
    }


    /**
     * Add Fragment
     * -------------------------------------------------------------------------
     * - When adapter initialized, use this method to store all your fragments
     * - Add fragment only if not exists in the List
     */
    public void add(Fragment fragment){
        Class<?> mClass = fragment.getClass();
        if(fragmentManager.findFragmentByTag(mClass.getName())==null) {
            fragmentManager.beginTransaction().add(fragment, mClass.getName()).commit();
        }
    }


    /**
     * Get Fragment
     * -------------------------------------------------------------------------
     * - Get the fragment that has been state-saved by addFragment
     */
    public Fragment get(Class<?> fragmentClass){
        Fragment fragment = fragmentManager.findFragmentByTag(fragmentClass.getName());
        if(fragment!=null){
            return fragment;
        }else{
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

    /*
     *
     * Get Fragment
     * -------------------------------------------------------------------------
     * - If fragment already saved in List, retrieve it
     * - If not: save the given instance in list, and return
     */
    public Fragment addAndGet(Fragment fragment){
        // Check if already saved, if not? save it first.
        Class<?> mClass = fragment.getClass();
        Fragment savedFragment = fragmentManager.findFragmentByTag(mClass.getName());
        if(savedFragment==null){
            fragmentManager.beginTransaction().add(fragment, mClass.getName()).commit();
        }
        return fragmentManager.findFragmentByTag(mClass.getName());
    }


    /**
     * Replace fragment
     * -------------------------------------------------------------------------
     * - Replace old fragment with new fragment (fragment with same class)
     */
    public Fragment replace(Fragment newFragment){
        // Get old fragment
        Class<?> mClass = newFragment.getClass();
        Fragment savedFragment = fragmentManager.findFragmentByTag(mClass.getName());

        if(savedFragment!=null){
            // Replace it with new fragment
            fragmentManager.beginTransaction().remove(savedFragment).commit();
        }
        fragmentManager.beginTransaction().add(newFragment, mClass.getName()).commit();

        return newFragment;
    }

    /**
     * Remove Fragment
     * -------------------------------------------------------------------------
     */
    public void remove(Class<?> fragmentClass){
        Fragment savedFragment = fragmentManager.findFragmentByTag(fragmentClass.getName());

        if(savedFragment!=null){
            // Replace it with new fragment
            fragmentManager.beginTransaction().remove(savedFragment).commit();
        }
    }

    /**
     * Remove All
     * -------------------------------------------------------------------------
     */
    public void removeAll(){

    }

}
