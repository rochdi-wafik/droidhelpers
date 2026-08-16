package com.iorgana.droidhelpers.ui;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * *****************************************************************************
 *  EdgeToEdgeUtils
 * *****************************************************************************
 * - Android 16 (API 36) removed `windowOptOutEdgeToEdgeEnforcement`
 *   which was used in Android-15 to fix the floating ActionBar issue.
 * - Android-16,  every window is forced edge-to-edge regardless of theme settings. 
 * - Static XML/Java padding can't compensate because the real system bar size
 *  is only known at runtime (it varies by device, orientation, vs. 3-button navigation).
 * -----------------------------------------------------------------------------
 * Two strategies. Do not mix them on the same screen:
 *   1. applyBarInsets(Activity) pads the whole content frame and consumes the
 *      insets. Use for standard screens that only need their old spacing back.
 *   2. The per-view methods do not consume, so several can run on one screen.
 *      Use when different views need different edges (a Toolbar in the layout, a
 *      bottom bar under the nav bar, an input field over the keyboard).
 * Because strategy 1 consumes at the frame, per-view listeners under it receive
 * nothing. Pick one approach per screen, and call each method once per view.
 * -----------------------------------------------------------------------------
 * Requirement: remove android:fitsSystemWindows="true" from the theme, or it
 * competes with these listeners for the same insets.
 * -----------------------------------------------------------------------------
 * `inset` is the size of the area along one edge of a window that is covered by 
 * something owned by system.
 */
public final class EdgeToEdgeUtils {
    private EdgeToEdgeUtils() {}

    /**
     * *****************************************************************************
     * applyBarInsets
     * *****************************************************************************
     * - Fix Padding-Gone in the Root Layout. (Android-16)
     *  Whole-screen fix. Pads android.R.id.content (the frame wrapping the
     *  ActionBar and your layout) for status bar, nav bar and cutouts, then
     *  consumes so AppCompat does not re-apply the top inset and double the gap.
     * -----------------------------------------------------------------------------
     * @example
     * - In onCreate(), after setContentView:
     * EdgeToEdgeUtils.applyBarInsets(this);
     */
    public static void applyBarInsets(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        EdgeToEdgeUtils.applyPadding(content,
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout(),
                true, true, true, true);
    }

    /**
     * *****************************************************************************
     * applyTopInset
     * *****************************************************************************
     * - Push a Toolbar clear of the status bar.
     *  Adds the top status bar / cutout inset to one view, on top of its existing
     *  padding. For a Toolbar placed inside a NoActionBar layout, where no window
     *  ActionBar handles the top for you.
     * -----------------------------------------------------------------------------
     * @example
     * - After setContentView, on the Toolbar you inflated:
     * EdgeToEdgeUtils.applyTopInset(toolbar);
     */
    public static void applyTopInset(View view) {
        EdgeToEdgeUtils.applyPadding(view,
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout(),
                true, false, false, false);
    }

    /**
     * *****************************************************************************
     * applyBottomInset
     * *****************************************************************************
     * - Keep a bottom bar above the navigation bar.
     *  Adds the bottom nav bar inset to one view, on top of its existing padding.
     *  For a bottom navigation view, a button bar, or a FAB container that would
     *  otherwise sit behind the gesture / 3-button navigation area.
     * -----------------------------------------------------------------------------
     * @example
     * - After setContentView, on your bottom bar:
     * EdgeToEdgeUtils.applyBottomInset(bottomNav);
     */
    public static void applyBottomInset(View view) {
        EdgeToEdgeUtils.applyPadding(view, WindowInsetsCompat.Type.systemBars(),
                false, true, false, false);
    }

    /**
     * *****************************************************************************
     * applyHorizontalInsets
     * *****************************************************************************
     * - Clear a display cutout in landscape.
     *  Adds the left and right bar / cutout insets to one view, on top of its
     *  existing padding. For content that reaches the side edges when the device
     *  is rotated and the camera cutout sits on a long edge.
     * -----------------------------------------------------------------------------
     * @example
     * - After setContentView, on the side-reaching container:
     * EdgeToEdgeUtils.applyHorizontalInsets(rootRow);
     */
    public static void applyHorizontalInsets(View view) {
        EdgeToEdgeUtils.applyPadding(view,
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout(),
                false, false, true, false);
    }

    /**
     * *****************************************************************************
     * applyImeBottomInset
     * *****************************************************************************
     * - Keep the focused field above the keyboard.
     *  Sets bottom padding to whichever is larger, the open keyboard or the nav
     *  bar, and updates as the keyboard opens and closes. For scroll or form
     *  screens so the field in focus is not hidden behind the IME.
     * -----------------------------------------------------------------------------
     * @example
     * - After setContentView, on the scroll / form container:
     * EdgeToEdgeUtils.applyImeBottomInset(scrollView);
     */
    public static void applyImeBottomInset(View view) {
        final int pl = view.getPaddingLeft();
        final int pt = view.getPaddingTop();
        final int pr = view.getPaddingRight();
        final int pb = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(pl, pt, pr, pb + Math.max(ime, nav));
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * *****************************************************************************
     * setLightStatusBarIcons
     * *****************************************************************************
     * - Keep status bar icons readable. (statusBarColor is a no-op on API 35+)
     *  Switches the status bar icons between dark and light. Pass true when the
     *  bar background is light so icons turn dark, false when it is dark so icons
     *  turn light.
     * -----------------------------------------------------------------------------
     * @example
     * - After setContentView, matching your ActionBar background:
     * EdgeToEdgeUtils.setLightStatusBarIcons(this, true); // light bar, dark icons
     */
    public static void setLightStatusBarIcons(Activity activity, boolean lightBackground) {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(lightBackground);
    }

    // Internal. Adds the chosen edges on top of the view's current padding so
    // XML design padding is kept, not overwritten. consume stops the insets here
    // when the caller has padded every edge (see applyBarInsets).
    private static void applyPadding(View view, int typeMask,
            boolean useTop, boolean useBottom, boolean useSides, boolean consume) {
        final int pl = view.getPaddingLeft();
        final int pt = view.getPaddingTop();
        final int pr = view.getPaddingRight();
        final int pb = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets i = insets.getInsets(typeMask);
            v.setPadding(
                    pl + (useSides ? i.left : 0),
                    pt + (useTop ? i.top : 0),
                    pr + (useSides ? i.right : 0),
                    pb + (useBottom ? i.bottom : 0));
            return consume ? WindowInsetsCompat.CONSUMED : insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
