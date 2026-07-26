package com.iorgana.droidhelpers_project.ui.demo;

import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import com.iorgana.droidhelpers.manager.FragmentsManager;
import com.iorgana.droidhelpers.manager.InstancesManager;
import com.iorgana.droidhelpers_project.ui.base.BaseDemoActivity;

/**
 * ManagersActivity
 * -----------------------------------------------------------------------------
 * Live usage examples for com.iorgana.droidhelpers.manager package:
 * FragmentsManager (save/retrieve headless Fragment instances by class) and
 * InstancesManager (a typed, class-keyed instance store; singleton or local).
 */
public class ManagersActivity extends BaseDemoActivity {

    /** Headless demo Fragment used only to exercise FragmentsManager (no UI of its own). */
    public static class DemoFragment extends Fragment {
        public final String createdAt = String.valueOf(System.currentTimeMillis());
    }

    /** Simple POJO used only to exercise InstancesManager. */
    public static class DemoConfig {
        public String label = "initial";
    }

    private FragmentsManager fragmentsManager;

    @Override
    protected String getScreenTitle() {
        return "App Managers";
    }

    @Override
    protected void buildContent() {
        buildFragmentsManagerSection();
        buildInstancesManagerSection();
    }

    /* ------------------------------------------------------------------ */
    private void buildFragmentsManagerSection() {
        LinearLayout s = addSection("FragmentsManager",
                "Saves/retrieves headless Fragment instances by class, so their in-memory fields survive across UI rebuilds.");

        runSafe(addRow(s, "new FragmentsManager(activity)"), () -> {
            fragmentsManager = new FragmentsManager(this);
            return "Created for this Activity's FragmentManager";
        });

        runSafe(addRow(s, "add(fragment)  (adds only if not already present)"), () -> {
            fragmentsManager.add(new DemoFragment());
            DemoFragment saved = (DemoFragment) fragmentsManager.get(DemoFragment.class);
            return "Added, createdAt=" + (saved != null ? saved.createdAt : "null");
        });

        runSafe(addRow(s, "get(DemoFragment.class)"), () -> {
            DemoFragment saved = (DemoFragment) fragmentsManager.get(DemoFragment.class);
            return saved != null ? "Found, createdAt=" + saved.createdAt : "Not found";
        });

        runSafe(addRow(s, "addAndGet(fragment)"), () -> {
            Fragment result = fragmentsManager.addAndGet(new DemoFragment());
            return "Returned instance = " + (result != null);
        });

        runSafe(addRow(s, "replace(newFragment)  (removes old tagged instance, adds the new one)"), () -> {
            DemoFragment before = (DemoFragment) fragmentsManager.get(DemoFragment.class);
            fragmentsManager.replace(new DemoFragment());
            DemoFragment after = (DemoFragment) fragmentsManager.get(DemoFragment.class);
            return "before=" + (before != null ? before.createdAt : "null") + ", after=" + (after != null ? after.createdAt : "null");
        });

        runSafe(addRow(s, "remove(DemoFragment.class)"), () -> {
            fragmentsManager.remove(DemoFragment.class);
            return "removed, get() now = " + fragmentsManager.get(DemoFragment.class);
        });

        runSafe(addRow(s, "removeAll()  (currently a no-op in this library version)"), () -> {
            fragmentsManager.removeAll();
            return "Called (method body is currently empty in the library)";
        });
    }

    /* ------------------------------------------------------------------ */
    private void buildInstancesManagerSection() {
        LinearLayout s = addSection("InstancesManager",
                "A typed, class-keyed instance store (one instance per Class) - either the shared singleton or a local instance.");

        runSafe(addRow(s, "new InstancesManager()  (local, non-singleton)"), () ->
                "created, independent from getInstance() = " + (new InstancesManager() != InstancesManager.getInstance()));

        runSafe(addRow(s, "getInstance()  (shared singleton)"), () -> "ready = " + (InstancesManager.getInstance() != null));

        runSafe(addRow(s, "add(object)"), () -> {
            InstancesManager.getInstance().add(new DemoConfig());
            return "Added a DemoConfig instance";
        });

        runSafe(addRow(s, "get(Class)"), () -> {
            DemoConfig config = InstancesManager.getInstance().get(DemoConfig.class);
            return config != null ? "Found, label=" + config.label : "Not found";
        });

        runSafe(addRow(s, "get(Class, defaultInstance)"), () -> {
            InstancesManager fresh = new InstancesManager();
            DemoConfig fallback = new DemoConfig();
            fallback.label = "fallback-default";
            DemoConfig result = fresh.get(DemoConfig.class, fallback);
            return "label=" + result.label + "  (nothing saved yet in this fresh manager, so default was returned)";
        });

        runSafe(addRow(s, "addAndGet(instance)"), () -> {
            DemoConfig result = InstancesManager.getInstance().addAndGet(new DemoConfig());
            return "label=" + result.label;
        });

        runSafe(addRow(s, "replace(newInstance)"), () -> {
            DemoConfig replacement = new DemoConfig();
            replacement.label = "replaced-" + System.currentTimeMillis();
            InstancesManager.getInstance().replace(replacement);
            DemoConfig after = InstancesManager.getInstance().get(DemoConfig.class);
            return "label after replace=" + (after != null ? after.label : "null");
        });

        runSafe(addRow(s, "remove(Class)"), () -> {
            InstancesManager.getInstance().remove(DemoConfig.class);
            return "removed, get() now = " + InstancesManager.getInstance().get(DemoConfig.class);
        });

        runSafe(addRow(s, "removeAll()"), () -> {
            InstancesManager.getInstance().removeAll();
            return "All stored instances cleared";
        });
    }
}