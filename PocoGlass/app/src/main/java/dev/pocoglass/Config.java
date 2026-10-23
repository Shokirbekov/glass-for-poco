package dev.pocoglass;

import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Reads settings written by SettingsActivity (via libxposed RemotePreferences). */
final class Config {
    static final String GROUP = "glass";

    static final String K_ENABLED = "enabled";
    static final String K_BLUR = "blur_radius";      // px, 0..150
    static final String K_CORNER = "corner_dp";      // 0..48
    static final String K_TINT = "tint_alpha";       // 0..100
    static final String K_RIM = "rim_strength";      // 0..100
    static final String K_BEVEL = "bevel_strength";  // 0..100
    static final String K_DISCOVERY = "discovery";   // log every id'd view
    static final String K_EXTRA = "targets_extra";   // comma separated resource entry names

    /** Best guesses only. Use discovery mode to find the real ids on your ROM. */
    static final String[] DEFAULT_TARGETS = {
            // SystemUI / control center / shade
            "notification_panel", "quick_settings_panel", "qs_frame", "qs_container",
            "notification_stack_scroller", "control_center_container",
            // Launcher3-based
            "folder_content", "apps_view", "hotseat", "overview_panel", "widgets_bottom_sheet",
    };

    private static SharedPreferences prefs;
    private static String cachedExtra = null;
    private static Set<String> cachedTargets = new HashSet<>(Arrays.asList(DEFAULT_TARGETS));

    static void init(SharedPreferences p) { prefs = p; }

    private static boolean b(String k, boolean d) { return prefs == null ? d : prefs.getBoolean(k, d); }
    private static int i(String k, int d) { return prefs == null ? d : prefs.getInt(k, d); }

    static boolean enabled()   { return b(K_ENABLED, true); }
    static boolean discovery() { return b(K_DISCOVERY, false); }
    static int blur()   { return i(K_BLUR, 60); }
    static int corner() { return i(K_CORNER, 24); }
    static int tint()   { return i(K_TINT, 18); }
    static int rim()    { return i(K_RIM, 60); }
    static int bevel()  { return i(K_BEVEL, 40); }

    static synchronized Set<String> targets() {
        String extra = prefs == null ? "" : prefs.getString(K_EXTRA, "");
        if (!extra.equals(cachedExtra)) {
            Set<String> s = new HashSet<>(Arrays.asList(DEFAULT_TARGETS));
            for (String t : extra.split("[,\\s]+")) if (!t.isEmpty()) s.add(t);
            cachedTargets = s;
            cachedExtra = extra;
        }
        return cachedTargets;
    }
}
