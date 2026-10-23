package dev.pocoglass;

import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.lang.reflect.Method;

/**
 * Glass material: real backdrop blur (the compositor's BackgroundBlurDrawable) plus a tint,
 * inner glow, thick-glass bevel and a diagonal specular rim.
 *
 * NOTE: this is not true refraction. Android has no public way to sample what is behind a
 * window from app code, so edge "lensing" is faked with shading, not by bending the backdrop.
 */
final class GlassDrawable extends Drawable {
    private final Drawable blur;          // may be null if the hidden API is unavailable
    private final float density;
    private final boolean night;
    private final float cornerDp;
    private final int tint, rim, bevel;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bevelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF rect = new RectF();
    private float radiusPx;

    GlassDrawable(View v) {
        density = v.getResources().getDisplayMetrics().density;
        night = (v.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        cornerDp = Config.corner();
        tint = Config.tint();
        rim = Config.rim();
        bevel = Config.bevel();
        blur = createBlur(v, Config.blur(), cornerDp * density);

        bevelPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(1.5f * density);
    }

    private static Drawable createBlur(View v, int radius, float cornerPx) {
        try {
            Method g = View.class.getDeclaredMethod("getViewRootImpl");
            g.setAccessible(true);
            Object root = g.invoke(v);
            if (root == null) return null;
            Method c = root.getClass().getDeclaredMethod("createBackgroundBlurDrawable");
            c.setAccessible(true);
            Drawable d = (Drawable) c.invoke(root);
            Class<?> k = d.getClass();
            k.getMethod("setBlurRadius", int.class).invoke(d, radius);
            k.getMethod("setCornerRadius", float.class).invoke(d, cornerPx);
            return d;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    protected void onBoundsChange(Rect b) {
        float w = b.width(), h = b.height();
        if (w <= 0 || h <= 0) return;
        radiusPx = Math.min(cornerDp * density, Math.min(w, h) / 2f);
        if (blur != null) {
            blur.setBounds(b);
            try { blur.getClass().getMethod("setCornerRadius", float.class).invoke(blur, radiusPx); }
            catch (Throwable ignored) { }
        }
        rect.set(b);
        clip.reset();
        clip.addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW);

        int base = night ? 0x1C1C1E : 0xFFFFFF;
        int aTop = Math.min(255, (int) (tint * 2.55f * 1.5f));
        int aBot = Math.min(255, (int) (tint * 2.55f * 0.6f));
        fill.setShader(new LinearGradient(b.left, b.top, b.left, b.bottom,
                (aTop << 24) | base, (aBot << 24) | base, Shader.TileMode.CLAMP));

        // soft bloom from the top edge
        int glowA = (int) (rim * 2.55f * 0.22f);
        glow.setShader(new RadialGradient(b.centerX(), b.top, Math.max(w, h) * 0.9f,
                (glowA << 24) | 0xFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));

        // thick-glass bevel: dark band just inside the edge
        float bw = Math.min(8f * density, Math.min(w, h) / 4f);
        bevelPaint.setStrokeWidth(bw * 2);
        bevelPaint.setColor(((int) (bevel * 2.55f * 0.35f) << 24) | 0x000000);

        // diagonal specular rim: bright top-left and bottom-right, quiet elsewhere
        int hi = ((int) (rim * 2.55f) << 24) | 0xFFFFFF;
        int mid = ((int) (rim * 2.55f * 0.12f) << 24) | 0xFFFFFF;
        int lo = ((int) (rim * 2.55f * 0.55f) << 24) | 0xFFFFFF;
        rimPaint.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{hi, mid, mid, lo}, new float[]{0f, 0.35f, 0.65f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas c) {
        if (rect.isEmpty()) return;
        if (blur != null) blur.draw(c);
        c.drawRoundRect(rect, radiusPx, radiusPx, fill);
        c.drawRoundRect(rect, radiusPx, radiusPx, glow);

        int save = c.save();
        c.clipPath(clip);
        if (bevel > 0) c.drawRoundRect(rect, radiusPx, radiusPx, bevelPaint);
        c.restoreToCount(save);

        if (rim > 0) {
            float inset = rimPaint.getStrokeWidth() / 2f;
            RectF r = new RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset);
            c.drawRoundRect(r, Math.max(0, radiusPx - inset), Math.max(0, radiusPx - inset), rimPaint);
        }
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
