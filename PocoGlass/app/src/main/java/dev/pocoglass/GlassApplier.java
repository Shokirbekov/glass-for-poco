package dev.pocoglass;

import android.util.Log;
import android.view.View;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Decides which attached views get the glass background. */
final class GlassApplier {
    private static final Map<Integer, String> NAMES = new HashMap<>();
    private static final Set<Integer> LOGGED = new HashSet<>();

    static void onAttached(View v, GlassModule log) {
        int id = v.getId();
        if (id == View.NO_ID || (id >>> 24) == 0) return; // skip generated ids
        if (!Config.enabled()) return;

        String name;
        synchronized (NAMES) {
            name = NAMES.get(id);
            if (name == null) {
                try { name = v.getResources().getResourceEntryName(id); }
                catch (Throwable t) { name = ""; }
                NAMES.put(id, name);
            }
        }
        if (name.isEmpty()) return;

        if (Config.discovery() && LOGGED.add(id)) {
            log.log(Log.INFO, GlassModule.TAG, "view id=" + name + " class=" + v.getClass().getName()
                    + " parent=" + (v.getParent() == null ? "-" : v.getParent().getClass().getSimpleName())
                    + " size=" + v.getWidth() + "x" + v.getHeight());
        }

        if (!Config.targets().contains(name)) return;
        final String targetName = name;
        v.post(() -> {
            try {
                if (v.getBackground() instanceof GlassDrawable) return;
                v.setBackground(new GlassDrawable(v));
            } catch (Throwable t) {
                log.log(Log.WARN, GlassModule.TAG, "glass failed on " + targetName, t);
            }
        });
    }

    private GlassApplier() {}
}
