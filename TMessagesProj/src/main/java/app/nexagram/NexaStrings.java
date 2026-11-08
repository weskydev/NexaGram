package app.nexagram;

import android.util.SparseBooleanArray;

import org.telegram.messenger.R;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces the app name "Telegram" with "NexaGram" in UI texts.
 *
 * Most texts come from Telegram's cloud language packs, so changing strings.xml
 * is not enough: every string goes through {@link #brand(String)} instead.
 *
 * Left untouched on purpose:
 *  - Telegram's own paid products and services (Premium, Stars, Business, ...):
 *    they are sold by Telegram, not by us;
 *  - links, domains and usernames (telegram.org, t.me/telegram, @telegram);
 *  - our own strings (keys starting with "Nexa"): they say "Telegram" on purpose.
 */
public final class NexaStrings {

    public static final String APP_NAME = "NexaGram";

    // "Telegram" as a whole word, not part of a link/username/domain,
    // and not followed by the name of a Telegram product.
    private static final Pattern TELEGRAM = Pattern.compile(
            "(?<![\\w@/.\\-])Telegram(?![\\w\\-'’]|\\.[A-Za-z]|\\s+(?:Premium|Stars?|Business|Gateway|Passport|Support|FAQ|Features|Terms|Privacy|Gift|Wallet|Fragment|API|Desktop|Web|X|Bot API|[Vv]olunteers?|[Ss]ervers?|[Cc]loud|[Tt]eam)\\b)"
    );

    // "Telegram" right after a word about Telegram's cloud or servers (RU, UK, BE word order):
    // "в облаке Telegram", "у хмарі Telegram", "на серверах Telegram". Data lives there, not with us.
    private static final Pattern CLOUD_BEFORE = Pattern.compile(
            "(?:облак|облач|хмар|воблак|воблач|сервер|хранилищ|сховищ)[а-яіїєўґ]*\\s+$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private NexaStrings() {
    }

    private static final String OWN_PREFIX = "Nexa";

    public static String brand(String key, int res, String text) {
        if (text == null || text.indexOf("Telegram") < 0 || isOwn(key, res)) {
            return text;
        }
        return brand(text);
    }

    public static String brand(String text) {
        if (text == null || text.indexOf("Telegram") < 0) {
            return text;
        }
        Matcher m = TELEGRAM.matcher(text);
        if (!m.find()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length() + 8);
        int last = 0;
        do {
            int from = Math.max(last, m.start() - 24);
            boolean cloud = CLOUD_BEFORE.matcher(text.substring(from, m.start())).find();
            out.append(text, last, m.start()).append(cloud ? m.group() : APP_NAME);
            last = m.end();
        } while (m.find());
        return out.append(text, last, text.length()).toString();
    }

    private static boolean isOwn(String key, int res) {
        if (key != null) {
            return key.startsWith(OWN_PREFIX);
        }
        return res != 0 && ownResIds().get(res);
    }

    // Ids of R.string.Nexa*; built once. Telegram's build strips string names from
    // the resource table, so getResourceEntryName() can't be used here.
    private static volatile SparseBooleanArray ownResIds;

    private static SparseBooleanArray ownResIds() {
        SparseBooleanArray ids = ownResIds;
        if (ids == null) {
            ids = new SparseBooleanArray();
            for (Field field : R.string.class.getFields()) {
                if (field.getName().startsWith(OWN_PREFIX)) {
                    try {
                        ids.put(field.getInt(null), true);
                    } catch (Exception ignored) {
                    }
                }
            }
            ownResIds = ids;
        }
        return ids;
    }
}
