package app.nexagram;

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
 *  - links, domains and usernames (telegram.org, t.me/telegram, @telegram).
 */
public final class NexaStrings {

    public static final String APP_NAME = "NexaGram";

    // "Telegram" as a whole word, not part of a link/username/domain,
    // and not followed by the name of a Telegram product.
    private static final Pattern TELEGRAM = Pattern.compile(
            "(?<![\\w@/.\\-])Telegram(?![\\w\\-'’]|\\.[A-Za-z]|\\s+(?:Premium|Stars?|Business|Gateway|Passport|Support|FAQ|Terms|Privacy|Gift|Wallet|Fragment|API|Desktop|Web|X|Bot API|[Vv]olunteers?|[Ss]ervers?|[Tt]eam)\\b)"
    );

    private NexaStrings() {
    }

    public static String brand(String text) {
        if (text == null || text.indexOf("Telegram") < 0) {
            return text;
        }
        Matcher m = TELEGRAM.matcher(text);
        if (!m.find()) {
            return text;
        }
        return m.replaceAll(APP_NAME);
    }
}
