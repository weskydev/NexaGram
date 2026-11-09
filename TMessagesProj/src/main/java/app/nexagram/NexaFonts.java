package app.nexagram;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * App font (Settings → NexaGram → Appearance → Font).
 * Fonts are open (SIL OFL) and live in assets/fonts/nexa as <id>_r.ttf (regular) and <id>_b.ttf (bold),
 * see assets/fonts/nexa/LICENSES.txt. The choice is applied once when the app starts.
 */
public final class NexaFonts {

    /** "" = Telegram's own look (system font + Roboto Medium), "system" = the phone's font everywhere. */
    public static final String TELEGRAM = "", SYSTEM = "system";
    public static final String[] IDS = {TELEGRAM, SYSTEM, "inter", "manrope", "montserrat", "nunito", "rubik", "golostext", "comfortaa", "onest"};
    private static final String[] NAMES = {null, null, "Inter", "Manrope", "Montserrat", "Nunito", "Rubik", "Golos Text", "Comfortaa", "Onest"};
    private static final String KEY = "font";

    private static String current;
    private static Typeface regular, bold;

    private NexaFonts() {
    }

    public static String current() {
        if (current == null) {
            Context context = ApplicationLoader.applicationContext;
            current = context == null ? TELEGRAM : context.getSharedPreferences("nexagram", Context.MODE_PRIVATE).getString(KEY, TELEGRAM);
            if (indexOf(current) < 0) {
                current = TELEGRAM;
            }
        }
        return current;
    }

    /** Saves the choice. Takes effect after the app restarts. */
    public static void set(String id) {
        ApplicationLoader.applicationContext.getSharedPreferences("nexagram", Context.MODE_PRIVATE).edit().putString(KEY, id).apply();
        current = id;
    }

    public static int indexOf(String id) {
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public static String name(int index) {
        if (index == 0) {
            return org.telegram.messenger.LocaleController.getString(R.string.NexaFontTelegram);
        }
        if (index == 1) {
            return org.telegram.messenger.LocaleController.getString(R.string.NexaFontSystem);
        }
        return NAMES[index];
    }

    /** Typeface to show a font's name in the picker. */
    public static Typeface preview(int index) {
        if (index < 2) {
            return null;
        }
        return load(IDS[index], "r");
    }

    private static Typeface load(String id, String weight) {
        try {
            return Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), "fonts/nexa/" + id + "_" + weight + ".ttf");
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    /** Our bold for AndroidUtilities.bold(), or null to keep Telegram's Roboto Medium. */
    public static Typeface bold() {
        String id = current();
        if (TELEGRAM.equals(id)) {
            return null;
        }
        if (bold == null) {
            if (SYSTEM.equals(id)) {
                bold = Build.VERSION.SDK_INT >= 28 ? Typeface.create(null, 500, false) : Typeface.create("sans-serif-medium", Typeface.NORMAL);
            } else {
                bold = load(id, "b");
            }
        }
        return bold;
    }

    /** Replaces Telegram's Roboto files (AndroidUtilities.getTypeface) with the chosen font, or returns null. */
    public static Typeface replace(String assetPath) {
        if (TELEGRAM.equals(current()) || assetPath == null) {
            return null;
        }
        switch (assetPath) {
            case "fonts/rmedium.ttf":
                return bold();
            case "fonts/rmediumitalic.ttf":
                return bold() == null ? null : Typeface.create(bold(), Typeface.ITALIC);
            case "fonts/ritalic.ttf":
                Typeface r = SYSTEM.equals(current()) ? Typeface.DEFAULT : regular();
                return r == null ? null : Typeface.create(r, Typeface.ITALIC);
            default:
                return null;
        }
    }

    private static Typeface regular() {
        if (regular == null && !TELEGRAM.equals(current()) && !SYSTEM.equals(current())) {
            regular = load(current(), "r");
        }
        return regular;
    }

    /**
     * Makes the chosen font the default one, so every text without its own font uses it.
     * Android has no public call for this, so it goes through the same fields Typeface itself uses.
     */
    public static void apply() {
        Typeface r = regular();
        if (r == null) {
            return;
        }
        Typeface b = bold() != null ? bold() : r;
        int done = 0;
        try {
            Method m = Typeface.class.getDeclaredMethod("setDefault", Typeface.class);
            m.setAccessible(true);
            m.invoke(null, r);
            done |= 1;
        } catch (Throwable ignore) {
        }
        done |= setStatic("DEFAULT", r) ? 2 : 0;
        done |= setStatic("SANS_SERIF", r) ? 4 : 0;
        done |= setStatic("DEFAULT_BOLD", b) ? 8 : 0;
        try {
            Field f = Typeface.class.getDeclaredField("sDefaults");
            f.setAccessible(true);
            Typeface[] defaults = (Typeface[]) f.get(null);
            if (defaults != null && defaults.length >= 4) {
                defaults[0] = r;
                defaults[1] = b;
                defaults[2] = Typeface.create(r, Typeface.ITALIC);
                defaults[3] = Typeface.create(b, Typeface.ITALIC);
                done |= 16;
            }
        } catch (Throwable ignore) {
        }
        try {
            Field f = Typeface.class.getDeclaredField("sSystemFontMap");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Typeface> map = (Map<String, Typeface>) f.get(null);
            if (map != null) {
                map.put("sans-serif", r);
                map.put("sans-serif-medium", b);
                done |= 32;
            }
        } catch (Throwable ignore) {
        }
        FileLog.d("NexaFonts: applied " + current() + ", steps " + Integer.toBinaryString(done));
    }

    /**
     * Android 9+ does not let apps change the default font of a plain Paint, so Telegram's own text
     * paints get the chosen font directly. Paints that already have a font (bold, mono...) are kept.
     */
    public static <T extends Paint> T fix(T paint) {
        Typeface r = regular();
        if (paint != null && r != null && paint.getTypeface() == null) {
            paint.setTypeface(r);
        }
        return paint;
    }

    /** Runs fix() over every static Paint of a class (Theme keeps the chat and chat list paints there). */
    public static void fixPaints(Class<?> cls) {
        if (regular() == null) {
            return;
        }
        for (Field f : cls.getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            Class<?> type = f.getType();
            try {
                if (Paint.class.isAssignableFrom(type)) {
                    f.setAccessible(true);
                    fix((Paint) f.get(null));
                } else if (type.isArray() && Paint.class.isAssignableFrom(type.getComponentType())) {
                    f.setAccessible(true);
                    Paint[] paints = (Paint[]) f.get(null);
                    if (paints != null) {
                        for (Paint p : paints) {
                            fix(p);
                        }
                    }
                }
            } catch (Throwable ignore) {
            }
        }
    }

    private static boolean setStatic(String name, Typeface value) {
        try {
            Field f = Typeface.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(null, value);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /** Draws text in a given typeface (TypefaceSpan(Typeface) needs Android 9). */
    public static class Span extends MetricAffectingSpan {
        private final Typeface typeface;

        public Span(Typeface typeface) {
            this.typeface = typeface;
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            apply(tp);
        }

        @Override
        public void updateMeasureState(TextPaint tp) {
            apply(tp);
        }

        private void apply(Paint p) {
            if (typeface != null) {
                p.setTypeface(typeface);
            }
        }
    }
}
