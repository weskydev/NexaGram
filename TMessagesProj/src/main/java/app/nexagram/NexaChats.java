package app.nexagram;

import android.app.Activity;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

/**
 * Small chat features, each with its own switch in NexaGram → Chats:
 * confirm before a call, hide the keyboard when the chat is scrolled,
 * user / chat ID in the profile.
 */
public final class NexaChats {

    public static final String CONFIRM_CALL = "feature_confirm_call";
    public static final String HIDE_KEYBOARD = "feature_hide_keyboard_scroll";
    public static final String SHOW_ID = "feature_show_id";

    /** Set while the call the user confirmed is being started, so it isn't asked twice. */
    private static boolean confirmed;

    private NexaChats() {
    }

    public static boolean hideKeyboardOnScroll() {
        return NexaDesign.get(HIDE_KEYBOARD);
    }

    public static boolean showId() {
        return NexaDesign.get(SHOW_ID);
    }

    /**
     * Asks "Call Name?" before a call. Returns true when the call has to wait for the answer;
     * {@code call} runs again after "Call".
     */
    public static boolean confirmCall(TLRPC.User user, boolean video, Activity activity, Runnable call) {
        if (confirmed) {
            confirmed = false;
            return false;
        }
        if (!NexaDesign.get(CONFIRM_CALL) || activity == null || user == null) {
            return false;
        }
        String name = UserObject.getUserName(user);
        try {
            new AlertDialog.Builder(activity)
                    .setTitle(getString(video ? R.string.VideoCall : R.string.Call))
                    .setMessage(AndroidUtilities.replaceTags(formatString(video ? R.string.NexaVideoCallConfirm : R.string.NexaCallConfirm, name)))
                    .setPositiveButton(getString(R.string.Call), (d, w) -> {
                        confirmed = true;
                        try {
                            call.run();
                        } finally {
                            confirmed = false;
                        }
                    })
                    .setNegativeButton(getString(R.string.Cancel), null)
                    .show();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // ---- message actions and send confirmations ----

    public static final String HIDE_STORIES = "feature_hide_stories";
    public static final String NOTE_TOP = "feature_note_top";
    public static final String REPEAT = "feature_repeat";
    public static final String DETAILS = "feature_details";
    public static final String FIRST_MESSAGE = "feature_first_message";
    public static final String CONFIRM_VOICE = "feature_confirm_voice";
    public static final String CONFIRM_STICKER = "feature_confirm_sticker";

    public static final int OPTION_REPEAT = 9201;
    public static final int OPTION_DETAILS = 9202;
    public static final int MENU_FIRST_MESSAGE = 9301;

    public static boolean repeat() {
        return NexaDesign.get(REPEAT);
    }

    public static boolean details() {
        return NexaDesign.get(DETAILS);
    }

    public static boolean firstMessage() {
        return NexaDesign.get(FIRST_MESSAGE);
    }

    /** A voice or round message stops in the preview instead of being sent right away. */
    public static boolean confirmVoice() {
        return NexaDesign.get(CONFIRM_VOICE);
    }

    private static boolean stickerConfirmed;

    /** Asks before a sticker is sent. Returns true when sending has to wait for the answer. */
    public static boolean confirmSticker(Activity activity, Runnable send) {
        if (stickerConfirmed) {
            stickerConfirmed = false;
            return false;
        }
        if (!NexaDesign.get(CONFIRM_STICKER) || activity == null) {
            return false;
        }
        try {
            new AlertDialog.Builder(activity)
                    .setTitle(getString(R.string.NexaSendStickerTitle))
                    .setMessage(getString(R.string.NexaSendStickerText))
                    .setPositiveButton(getString(R.string.Send), (d, w) -> {
                        stickerConfirmed = true;
                        try {
                            send.run();
                        } finally {
                            stickerConfirmed = false;
                        }
                    })
                    .setNegativeButton(getString(R.string.Cancel), null)
                    .show();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /** Message details: exact time, edit time, IDs, forward info and the file. */
    public static void showDetails(BaseFragment fragment, MessageObject message) {
        if (message == null || message.messageOwner == null || fragment.getParentActivity() == null) {
            return;
        }
        TLRPC.Message m = message.messageOwner;
        StringBuilder sb = new StringBuilder();
        java.text.SimpleDateFormat full = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault());
        line(sb, getString(R.string.NexaDetailsDate), full.format(new java.util.Date(m.date * 1000L)));
        if (m.edit_date != 0) {
            line(sb, getString(R.string.NexaDetailsEdited), full.format(new java.util.Date(m.edit_date * 1000L)));
        }
        line(sb, getString(R.string.NexaDetailsMessageId), Integer.toString(m.id));
        long from = message.getFromChatId();
        if (from != 0) {
            line(sb, getString(R.string.NexaDetailsSenderId), from > 0 ? Long.toString(from) : idText(0, MessagesController.getInstance(message.currentAccount).getChat(-from)));
        }
        long dialog = message.getDialogId();
        if (dialog > 0 || DialogObject.isEncryptedDialog(dialog)) {
            line(sb, getString(R.string.NexaDetailsChatId), Long.toString(dialog));
        } else {
            line(sb, getString(R.string.NexaDetailsChatId), idText(0, MessagesController.getInstance(message.currentAccount).getChat(-dialog)));
        }
        if (m.fwd_from != null) {
            if (m.fwd_from.date != 0) {
                line(sb, getString(R.string.NexaDetailsForwardDate), full.format(new java.util.Date(m.fwd_from.date * 1000L)));
            }
            if (m.fwd_from.from_name != null) {
                line(sb, getString(R.string.NexaDetailsForwardFrom), m.fwd_from.from_name);
            }
        }
        TLRPC.Document doc = message.getDocument();
        if (doc != null) {
            String name = FileLoader.getDocumentFileName(doc);
            if (!TextUtils.isEmpty(name)) {
                line(sb, getString(R.string.NexaDetailsFile), name);
            }
            line(sb, getString(R.string.NexaDetailsSize), AndroidUtilities.formatFileSize(doc.size));
            if (!TextUtils.isEmpty(doc.mime_type)) {
                line(sb, getString(R.string.NexaDetailsType), doc.mime_type);
            }
            for (TLRPC.DocumentAttribute a : doc.attributes) {
                if (a.w > 0 && a.h > 0) {
                    line(sb, getString(R.string.NexaDetailsResolution), a.w + "×" + a.h);
                }
                if (a.duration > 0) {
                    line(sb, getString(R.string.NexaDetailsDuration), AndroidUtilities.formatShortDuration((int) a.duration));
                }
            }
        } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto && message.messageOwner.media.photo != null) {
            TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(message.messageOwner.media.photo.sizes, AndroidUtilities.getPhotoSize());
            if (size != null) {
                line(sb, getString(R.string.NexaDetailsResolution), size.w + "×" + size.h);
                if (size.size > 0) {
                    line(sb, getString(R.string.NexaDetailsSize), AndroidUtilities.formatFileSize(size.size));
                }
            }
        }
        final String text = sb.toString().trim();
        new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(getString(R.string.NexaDetails))
                .setMessage(text)
                .setPositiveButton(getString(R.string.OK), null)
                .setNeutralButton(getString(R.string.Copy), (d, w) -> {
                    if (AndroidUtilities.addToClipboard(text)) {
                        BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.TextCopied)).show();
                    }
                })
                .show();
    }

    private static void line(StringBuilder sb, String title, String value) {
        sb.append(title).append(": ").append(value).append('\n');
    }

    /** ID as bots and other apps write it: user 123, group -123, channel / supergroup -100123. */
    public static String idText(long userId, TLRPC.Chat chat) {
        if (userId != 0) {
            return Long.toString(userId);
        }
        if (chat == null) {
            return "";
        }
        return (ChatObject.isChannel(chat) ? "-100" : "-") + chat.id;
    }

    // ---- approximate registration date ----

    public static final String REG_DATE = "feature_reg_date";

    public static boolean showRegDate() {
        return NexaDesign.get(REG_DATE);
    }

    // Known user ids and when they were registered (unix seconds), ascending.
    // Ids grow with time, so the date of any id is interpolated between neighbours.
    private static final long[][] ID_DATES = {
            {2768409L, 1383264000L},
            {7679610L, 1388448000L},
            {11538514L, 1391212000L},
            {15835244L, 1392940000L},
            {23646077L, 1393459000L},
            {38015510L, 1393632000L},
            {44634663L, 1399334000L},
            {46145305L, 1400198000L},
            {54845238L, 1411257000L},
            {63263518L, 1414454000L},
            {101260938L, 1425600000L},
            {111220210L, 1429574000L},
            {116812045L, 1437696000L},
            {124872445L, 1439856000L},
            {133909606L, 1444176000L},
            {143445125L, 1448928000L},
            {152079341L, 1453420000L},
            {171295414L, 1457481000L},
            {181783990L, 1460246000L},
            {222021233L, 1465344000L},
            {278941742L, 1473465000L},
            {294851037L, 1479600000L},
            {328594461L, 1482969000L},
            {369669043L, 1490918000L},
            {400169472L, 1501459000L},
            {805158066L, 1563208000L},
            {1974255900L, 1634000000L},
            {5000000000L, 1642000000L},
            {5500000000L, 1660000000L},
            {6000000000L, 1683000000L},
            {6500000000L, 1697000000L},
            {7000000000L, 1712000000L},
            {7500000000L, 1727000000L},
            {8000000000L, 1745000000L},
    };

    /** Estimated registration time of a user id, unix seconds. */
    public static long estimateRegDate(long id) {
        long[][] t = ID_DATES;
        if (id <= t[0][0]) {
            return t[0][1];
        }
        for (int i = 1; i < t.length; i++) {
            if (id <= t[i][0]) {
                double k = (double) (id - t[i - 1][0]) / (t[i][0] - t[i - 1][0]);
                return t[i - 1][1] + (long) (k * (t[i][1] - t[i - 1][1]));
            }
        }
        // newer than the table: continue the last trend, but not into the future
        long[] a = t[t.length - 2], b = t[t.length - 1];
        long date = b[1] + (long) ((double) (id - b[0]) / (b[0] - a[0]) * (b[1] - a[1]));
        return Math.min(date, System.currentTimeMillis() / 1000);
    }

    /** "≈ Jul 2023". */
    public static String regDateText(long id) {
        // "LLLL" is the stand-alone month name: "сентябрь 2021", not "сентября 2021"
        java.util.Locale locale = LocaleController.getInstance().getCurrentLocale();
        String text = new java.text.SimpleDateFormat("LLLL yyyy", locale != null ? locale : java.util.Locale.getDefault())
                .format(new java.util.Date(estimateRegDate(id) * 1000L));
        return "≈ " + (text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1));
    }

    public static void copyId(BaseFragment fragment, String id) {
        if (AndroidUtilities.addToClipboard(id)) {
            BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.NexaIdCopied)).show();
        }
    }
}
