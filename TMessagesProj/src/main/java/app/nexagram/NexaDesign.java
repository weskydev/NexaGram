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
    public static final String CONTACTS_DESIGN = "design_contacts";
    public static final String SETTINGS_DESIGN = "design_settings";
    public static final String PROFILE_LINE = "design_profile_line";
    public static final String MUSIC_BLOCK = "design_music_block";

    public static final String[] ALL = {CHAT_CARDS, SQUARE_AVATARS, PINNED_TINT, ACCENT_SEARCH, TAB_BAR, DOCKED_COMPOSE, CONTACTS_DESIGN, SETTINGS_DESIGN, PROFILE_LINE, MUSIC_BLOCK};

    /** Bumped on every change so lists can notice and redraw. */
    private static int version;
    /** Docked compose button is built once per launch, so its value is frozen until restart. */
    private static Boolean dockedComposeAtLaunch;
    /** Profile header heights are set once per launch too. */
    private static Boolean profileLineAtLaunch, musicBlockAtLaunch;

    private NexaDesign() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("nexagram", Context.MODE_PRIVATE);
    }

    /** Features that change how Telegram behaves start switched off. */
    private static final java.util.Set<String> OFF_BY_DEFAULT = new java.util.HashSet<>(java.util.Arrays.asList(
            "feature_confirm_voice", "feature_confirm_sticker", "feature_hide_stories", NexaGlass.KEY));

    public static boolean get(String key) {
        boolean def = !OFF_BY_DEFAULT.contains(key);
        if (ApplicationLoader.applicationContext == null) {
            return def;
        }
        return prefs().getBoolean(key, def);
    }

    public static void set(String key, boolean value) {
        prefs().edit().putBoolean(key, value).apply();
        version++;
    }

    /** Marks a change of some other NexaGram option, so lists redraw. */
    public static void bump() {
        version++;
    }

    public static int version() {
        return version;
    }

    /** Settings that only apply after a restart. */
    public static boolean needsRestart(String key) {
        return DOCKED_COMPOSE.equals(key) || ACCENT_SEARCH.equals(key) || SETTINGS_DESIGN.equals(key)
                || PROFILE_LINE.equals(key) || MUSIC_BLOCK.equals(key);
    }

    public static boolean dockedCompose() {
        if (dockedComposeAtLaunch == null) {
            dockedComposeAtLaunch = get(DOCKED_COMPOSE);
        }
        return dockedComposeAtLaunch;
    }

    public static boolean profileLine() {
        if (profileLineAtLaunch == null) {
            profileLineAtLaunch = get(PROFILE_LINE);
        }
        return profileLineAtLaunch;
    }

    public static boolean musicBlock() {
        if (musicBlockAtLaunch == null) {
            musicBlockAtLaunch = get(MUSIC_BLOCK);
        }
        return musicBlockAtLaunch;
    }
}
