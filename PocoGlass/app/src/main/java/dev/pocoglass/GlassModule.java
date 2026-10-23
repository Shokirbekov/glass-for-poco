package dev.pocoglass;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GlassModule extends XposedModule {
    static final String TAG = "PocoGlass";

    private static final Set<String> PACKAGES = new HashSet<>(Arrays.asList(
            "com.android.systemui", "miui.systemui.plugin", "com.mi.android.globallauncher"));

    private boolean hooked;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        log(Log.INFO, TAG, "loaded in " + param.getProcessName());
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        if (!PACKAGES.contains(param.getPackageName()) || hooked) return;
        hooked = true;
        try {
            Config.init(getRemotePreferences(Config.GROUP));
            // Hooking View itself covers every view in the process, including views that
            // the control center creates from the separate miui.systemui.plugin classloader.
            Method m = View.class.getDeclaredMethod("onAttachedToWindow");
            hook(m).intercept(chain -> {
                Object result = chain.proceed();
                try {
                    GlassApplier.onAttached((View) chain.getThisObject(), this);
                } catch (Throwable t) {
                    log(Log.WARN, TAG, "apply failed", t);
                }
                return result;
            });
            log(Log.INFO, TAG, "hooked " + param.getPackageName());
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "install failed", t);
        }
    }
}
