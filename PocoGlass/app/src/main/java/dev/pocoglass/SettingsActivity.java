package dev.pocoglass;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class SettingsActivity extends Activity implements XposedServiceHelper.OnServiceListener {
    private TextView status;
    private LinearLayout root;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);
        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);

        status = new TextView(this);
        status.setText("Waiting for LSPosed… (enable the module and reboot)");
        root.addView(status);
        XposedServiceHelper.registerListener(this);
    }

    @Override
    public void onServiceBind(XposedService service) {
        prefs = service.getRemotePreferences(Config.GROUP);
        runOnUiThread(this::buildUi);
    }

    @Override public void onServiceDied(XposedService service) {
        runOnUiThread(() -> status.setText("LSPosed service disconnected"));
    }

    private void buildUi() {
        status.setText("Connected. Restart SystemUI / launcher after changing values.");
        check("Enabled", Config.K_ENABLED, true);
        slider("Blur radius", Config.K_BLUR, 60, 150);
        slider("Corner radius (dp)", Config.K_CORNER, 24, 48);
        slider("Tint", Config.K_TINT, 18, 100);
        slider("Rim light", Config.K_RIM, 60, 100);
        slider("Bevel depth", Config.K_BEVEL, 40, 100);
        check("Discovery mode (log every view id as PocoGlass in logcat)", Config.K_DISCOVERY, false);

        TextView t = new TextView(this);
        t.setText("Extra target view ids (comma separated, from discovery logs)");
        root.addView(t);
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setText(prefs.getString(Config.K_EXTRA, ""));
        e.setOnFocusChangeListener((v, f) -> {
            if (!f) prefs.edit().putString(Config.K_EXTRA, e.getText().toString()).apply();
        });
        root.addView(e);
    }

    private void check(String label, String key, boolean def) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        c.setChecked(prefs.getBoolean(key, def));
        c.setOnCheckedChangeListener((v, on) -> prefs.edit().putBoolean(key, on).apply());
        root.addView(c);
    }

    private void slider(String label, String key, int def, int max) {
        TextView t = new TextView(this);
        int cur = prefs.getInt(key, def);
        t.setText(label + ": " + cur);
        SeekBar s = new SeekBar(this);
        s.setMax(max);
        s.setProgress(cur);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean user) {
                t.setText(label + ": " + p);
                if (user) prefs.edit().putInt(key, p).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
        root.addView(t);
        root.addView(s);
    }
}
