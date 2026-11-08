package app.nexagram;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseIntArray;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.Theme;

/**
 * NexaGram brand colors.
 *
 * Telegram's built-in themes are full of hand-picked blues. Instead of editing
 * hundreds of values in upstream files (and fighting merge conflicts on every
 * update), we shift the hue of every "Telegram blue" to the NexaGram accent,
 * keeping its brightness so contrast stays the same. Greens, reds, grays and
 * user-made custom themes are left alone.
 */
public final class NexaTheme {

    /** Main NexaGram accent (violet). */
    public static final int ACCENT = 0xFF7B61FF;

    // Telegram's main blue (0xFF229AF0) has hue ~205°, our violet ~250°.
    private static final float SOURCE_HUE = 205f;
    private static final float TARGET_HUE = 250f;
    // Hue band treated as "Telegram blue"; colors fade out of the shift near the edges.
    private static final float BAND_START = 178f;
    private static final float BAND_FULL_START = 188f;
    private static final float BAND_FULL_END = 228f;
    private static final float BAND_END = 238f;
    // Telegram blue is more saturated than our violet; soften it a bit.
    private static final float SATURATION_SCALE = 0.75f;
    private static final float MIN_SATURATION = 0.12f;

    private static final String PREFS = "nexagram";
    private static final String KEY_BRAND_COLORS = "brand_colors";
    private static Boolean enabled;

    private NexaTheme() {
    }

    /** true = NexaGram violet, false = classic Telegram blue. */
    public static boolean isEnabled() {
        if (enabled == null) {
            Context context = ApplicationLoader.applicationContext;
            if (context == null) {
                return true;
            }
            enabled = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_BRAND_COLORS, true);
        }
        return enabled;
    }

    /** Saves the choice. Takes effect after the app restarts. */
    public static void setEnabled(boolean value) {
        enabled = value;
        ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_BRAND_COLORS, value).apply();
    }

    /** Recolors a single ARGB color. Non-blue colors come back unchanged. */
    public static int recolor(int color) {
        if (!isEnabled()) {
            return color;
        }
        int alpha = Color.alpha(color);
        if (alpha == 0) {
            return color;
        }
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        if (hsv[1] < MIN_SATURATION || hsv[2] < 0.08f) {
            return color;
        }
        float weight = bandWeight(hsv[0]);
        if (weight <= 0f) {
            return color;
        }
        hsv[0] = hsv[0] + (TARGET_HUE - SOURCE_HUE) * weight;
        hsv[1] = hsv[1] * (1f - (1f - SATURATION_SCALE) * weight);
        return Color.HSVToColor(alpha, hsv);
    }

    /** Recolors the base color table (Theme.defaultColors). */
    public static void recolor(int[] colors) {
        if (colors == null) {
            return;
        }
        for (int i = 0; i < colors.length; i++) {
            colors[i] = recolor(colors[i]);
        }
    }

    /** Recolors colors loaded from a built-in .attheme file. */
    public static void recolor(SparseIntArray colors) {
        if (colors == null) {
            return;
        }
        for (int i = 0; i < colors.size(); i++) {
            int key = colors.keyAt(i);
            if (key == Theme.key_wallpaperFileOffset) {
                continue;
            }
            colors.setValueAt(i, recolor(colors.valueAt(i)));
        }
    }

    /** Recolors the accent options of a built-in theme (the color picker in Settings). */
    public static void recolor(Theme.ThemeInfo theme) {
        if (theme == null || theme.themeAccents == null) {
            return;
        }
        for (Theme.ThemeAccent accent : theme.themeAccents) {
            accent.accentColor = recolor(accent.accentColor);
            accent.accentColor2 = recolor(accent.accentColor2);
            accent.myMessagesAccentColor = recolor(accent.myMessagesAccentColor);
            accent.myMessagesGradientAccentColor1 = recolor(accent.myMessagesGradientAccentColor1);
            accent.myMessagesGradientAccentColor2 = recolor(accent.myMessagesGradientAccentColor2);
            accent.myMessagesGradientAccentColor3 = recolor(accent.myMessagesGradientAccentColor3);
            accent.backgroundOverrideColor = recolor(accent.backgroundOverrideColor);
            accent.backgroundGradientOverrideColor1 = recolor(accent.backgroundGradientOverrideColor1);
            accent.backgroundGradientOverrideColor2 = recolor(accent.backgroundGradientOverrideColor2);
            accent.backgroundGradientOverrideColor3 = recolor(accent.backgroundGradientOverrideColor3);
        }
        theme.accentBaseColor = recolor(theme.accentBaseColor);
    }

    // Background overrides are stored as long; values outside int range are "not set" markers.
    private static long recolor(long color) {
        if (color == 0 || color > 0xFFFFFFFFL || color < 0) {
            return color;
        }
        return recolor((int) color) & 0xFFFFFFFFL;
    }

    private static float bandWeight(float hue) {
        if (hue < BAND_START || hue > BAND_END) {
            return 0f;
        }
        if (hue < BAND_FULL_START) {
            return (hue - BAND_START) / (BAND_FULL_START - BAND_START);
        }
        if (hue > BAND_FULL_END) {
            return (BAND_END - hue) / (BAND_END - BAND_FULL_END);
        }
        return 1f;
    }
}
