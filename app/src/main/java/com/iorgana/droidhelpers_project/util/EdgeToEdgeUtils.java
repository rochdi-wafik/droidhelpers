package com.iorgana.droidhelpers_project.util;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * EdgeToEdgeUtils
 * -----------------------------------------------------------------------------
 * Android 16 (API 36) fully removed the `windowOptOutEdgeToEdgeEnforcement`
 * theme flag - it is silently ignored on API 36 devices, so every window is
 * forced edge-to-edge regardless of theme settings. Static XML/Java padding
 * can't compensate because the real system bar size is only known at runtime
 * (it varies by device, orientation, and gesture vs. 3-button navigation).
 * -----------------------------------------------------------------------------
 * This applies the actual system bar insets (status bar + nav bar + cutouts)
 * as padding on android.R.id.content - the FrameLayout that wraps BOTH the
 * ActionBar and our own content view - so the whole screen shifts down/in
 * together instead of hiding behind the status bar, without touching every
 * individual layout.
 */
public final class EdgeToEdgeUtils {
    private EdgeToEdgeUtils() {}

    public static void applyBarInsets(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
