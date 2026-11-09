package app.nexagram;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LiteMode;
import org.telegram.ui.ActionBar.Theme;

/**
 * "Glass" look (NexaGram → Appearance): see-through bars and header, and our
 * own pills and cards drawn as glass — a light, tinted fill, a soft light on
 * top and a thin bright rim. Off by default; applied after a restart.
 * Turning it on also turns on Telegram's own blur (Power saving settings).
 */
public final class NexaGlass {

    public static final String KEY = "design_glass";
    /** Telegram's blur flags as they were before the glass turned them on. */
    private static final String SAVED_FLAGS = "design_glass_saved_flags";
    private static final int FLAGS = LiteMode.FLAG_CHAT_BLUR | LiteMode.FLAG_LIQUID_GLASS;

    private static Boolean atLaunch;

    private NexaGlass() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("nexagram", Context.MODE_PRIVATE);
    }

    /** The value at launch: the whole UI is built with it. */
    public static boolean on() {
        if (atLaunch == null) {
            if (ApplicationLoader.applicationContext == null) {
                return false;
            }
            atLaunch = NexaDesign.get(KEY);
        }
        return atLaunch;
    }

    /** The switch value (may differ from {@link #on()} until a restart). */
    public static boolean isSet() {
        return NexaDesign.get(KEY);
    }

    public static void set(boolean value) {
        NexaDesign.set(KEY, value);
        SharedPreferences p = prefs();
        if (value) {
            p.edit().putInt(SAVED_FLAGS, LiteMode.getValue(true) & FLAGS).apply();
            LiteMode.setAllFlags(LiteMode.getValue(true) | FLAGS);
        } else if (p.contains(SAVED_FLAGS)) {
            int saved = p.getInt(SAVED_FLAGS, FLAGS);
            LiteMode.setAllFlags((LiteMode.getValue(true) & ~FLAGS) | saved);
            p.edit().remove(SAVED_FLAGS).apply();
        }
    }

    private static boolean dark() {
        return Theme.isCurrentThemeDark();
    }

    // ---- Telegram's bars ----

    /** Fill of the bottom bar and the top panels: more see-through, a touch of the accent. */
    public static int bar(int telegramColor, int background, boolean dark) {
        if (!on()) {
            return telegramColor;
        }
        int c = ColorUtils.blendARGB(background, NexaChatList.accent(), dark ? 0.10f : 0.06f);
        return Theme.multAlpha(c, dark ? 0.52f : 0.58f);
    }

    /** Alpha of the scrim over the blurred chats-list header. */
    public static int headerAlpha(int telegramAlpha, boolean light) {
        if (!on()) {
            return telegramAlpha;
        }
        return light ? 200 : 168;
    }

    // ---- our shapes ----

    private static final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF tmp = new RectF();

    static {
        rimPaint.setStyle(Paint.Style.STROKE);
    }

    /** Glass fill color tinted with {@code tint}. */
    public static int fill(int tint, boolean pressed) {
        boolean dark = dark();
        int base = dark ? 0x14FFFFFF : 0x8CFFFFFF;
        int c = ColorUtils.compositeColors(Theme.multAlpha(tint, dark ? (pressed ? 0.30f : 0.16f) : (pressed ? 0.22f : 0.10f)), base);
        return c;
    }

    /** A whole glass shape: tinted see-through fill, light on top, bright rim. */
    public static void draw(Canvas canvas, RectF rect, float radius, int tint, boolean pressed, float alpha) {
        fillPaint.setColor(fill(tint, pressed));
        fillPaint.setAlpha((int) (fillPaint.getAlpha() * alpha));
        canvas.drawRoundRect(rect, radius, radius, fillPaint);
        rim(canvas, rect, radius, alpha, true);
    }

    /** Light on top and the rim, over a shape someone else has filled. */
    public static void rim(Canvas canvas, RectF rect, float radius, float alpha, boolean withLight) {
        if (rect.width() <= 0 || rect.height() <= 0 || alpha <= 0) {
            return;
        }
        boolean dark = dark();
        if (withLight) {
            int top = dark ? 0x1FFFFFFF : 0x59FFFFFF;
            lightPaint.setShader(new LinearGradient(0, rect.top, 0, rect.top + Math.min(rect.height(), dp(40)) * 0.9f,
                    top, 0x00FFFFFF, Shader.TileMode.CLAMP));
            lightPaint.setAlpha((int) (0xFF * alpha));
            canvas.drawRoundRect(rect, radius, radius, lightPaint);
        }
        float w = dp(1);
        rimPaint.setStrokeWidth(w);
        rimPaint.setShader(new LinearGradient(rect.left, rect.top, rect.right * 0.6f + rect.left * 0.4f, rect.bottom,
                new int[]{dark ? 0x59FFFFFF : 0xE6FFFFFF, dark ? 0x12FFFFFF : 0x40FFFFFF, dark ? 0x26FFFFFF : 0x8CFFFFFF},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        rimPaint.setAlpha((int) (0xFF * alpha));
        tmp.set(rect);
        tmp.inset(w / 2f, w / 2f);
        float r = Math.max(0, radius - w / 2f);
        canvas.drawRoundRect(tmp, r, r, rimPaint);
    }

    /** A glass drawable for a view background. */
    public static Drawable drawable(float radiusDp, int tint) {
        return new GlassDrawable(dp(radiusDp), tint);
    }

    /** Only the light and the rim, for a view that keeps its own fill (use as foreground). */
    public static Drawable rimDrawable(float radiusDp) {
        return new GlassDrawable(dp(radiusDp), 0, true);
    }

    private static final class GlassDrawable extends Drawable {
        private final float radius;
        private final int tint;
        private final boolean rimOnly;
        private final RectF rect = new RectF();
        private int alpha = 0xFF;

        GlassDrawable(float radius, int tint) {
            this(radius, tint, false);
        }

        GlassDrawable(float radius, int tint, boolean rimOnly) {
            this.radius = radius;
            this.tint = tint;
            this.rimOnly = rimOnly;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            rect.set(getBounds());
            float r = Math.min(radius, rect.height() / 2f);
            if (rimOnly) {
                NexaGlass.rim(canvas, rect, r, alpha / 255f, true);
            } else {
                NexaGlass.draw(canvas, rect, r, tint, false, alpha / 255f);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            this.alpha = alpha;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
