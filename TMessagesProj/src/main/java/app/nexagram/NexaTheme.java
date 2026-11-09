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

    /** Theme colors to choose from: hue the Telegram blue is turned into. BLUE keeps Telegram's colors. */
    public static final int VIOLET = 0, BLUE = 1, TEAL = 2, GREEN = 3, PINK = 4, RED = 5, ORANGE = 6, GOLD = 7;
    private static final float[] HUES = {250f, 205f, 180f, 145f, 325f, 355f, 22f, 42f};
    private static final String KEY_ACCENT = "accent_color";
    private static Integer choice;

    // Telegram's main blue (0xFF229AF0) has hue ~205°, our violet ~250°.
    private static final float SOURCE_HUE = 205f;
    private static float targetHue() {
        return HUES[choice()];
    }
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

    /** true = NexaGram colors, false = classic Telegram blue. */
    public static boolean isEnabled() {
        return choice() != BLUE;
    }

    /** The chosen theme color, one of VIOLET..GOLD. */
    public static int choice() {
        if (choice == null) {
            Context context = ApplicationLoader.applicationContext;
            if (context == null) {
                return VIOLET;
            }
            android.content.SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            // older versions only had violet on / off
            int def = p.getBoolean(KEY_BRAND_COLORS, true) ? VIOLET : BLUE;
            int c = p.getInt(KEY_ACCENT, def);
            choice = c >= 0 && c < HUES.length ? c : VIOLET;
        }
        return choice;
    }

    /** Saves the choice. Takes effect after the app restarts. */
    public static void setChoice(int value) {
        choice = value;
        ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_ACCENT, value).putBoolean(KEY_BRAND_COLORS, value != BLUE).apply();
    }

    public static int colorCount() {
        return HUES.length;
    }

    /** The main color of a choice, for the picker and the app's own accents. */
    public static int accentOf(int which) {
        if (which == VIOLET) {
            return ACCENT;
        }
        if (which == BLUE) {
            return 0xFF3E88F7;
        }
        return tint(ACCENT, which);
    }

    /** Main accent of the current choice. */
    public static int accent() {
        return accentOf(choice());
    }

    /** One of NexaGram's violet shades turned into the chosen color (same brightness). */
    public static int tint(int violet) {
        return tint(violet, choice());
    }

    private static int tint(int violet, int which) {
        if (which == VIOLET) {
            return violet;
        }
        float[] hsv = new float[3];
        Color.colorToHSV(violet, hsv);
        hsv[0] = (hsv[0] + HUES[which] - HUES[VIOLET] + 360f) % 360f;
        return Color.HSVToColor(Color.alpha(violet), hsv);
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
        hsv[0] = (hsv[0] + (targetHue() - SOURCE_HUE) * weight + 360f) % 360f;
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
