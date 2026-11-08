package app.nexagram;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * NexaGram look switches. Every item is on by default; turning it off gives
 * Telegram's original look back for that part.
 */
public final class NexaDesign {

    public static final String CHAT_CARDS = "design_chat_cards";
    public static final String SQUARE_AVATARS = "design_square_avatars";
    public static final String PINNED_TINT = "design_pinned_tint";
    public static final String ACCENT_SEARCH = "design_accent_search";
    public static final String TAB_BAR = "design_tab_bar";
    public static final String DOCKED_COMPOSE = "design_docked_compose";

    public static final String[] ALL = {CHAT_CARDS, SQUARE_AVATARS, PINNED_TINT, ACCENT_SEARCH, TAB_BAR, DOCKED_COMPOSE};

    /** Bumped on every change so lists can notice and redraw. */
    private static int version;
    /** Docked compose button is built once per launch, so its value is frozen until restart. */
    private static Boolean dockedComposeAtLaunch;

    private NexaDesign() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("nexagram", Context.MODE_PRIVATE);
    }

    public static boolean get(String key) {
        if (ApplicationLoader.applicationContext == null) {
            return true;
        }
        return prefs().getBoolean(key, true);
    }

    public static void set(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
        version++;
    }

    public static int version() {
        return version;
    }

    /** Settings that only apply after a restart. */
    public static boolean needsRestart(String key) {
        return DOCKED_COMPOSE.equals(key) || ACCENT_SEARCH.equals(key);
    }

    public static boolean dockedCompose() {
        if (dockedComposeAtLaunch == null) {
            dockedComposeAtLaunch = get(DOCKED_COMPOSE);
        }
        return dockedComposeAtLaunch;
    }
}
