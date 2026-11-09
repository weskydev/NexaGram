package app.nexagram;

import android.app.Activity;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
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

    /** "ID" value for the profile list. */
    public static String idText(long id) {
        return Long.toString(Math.abs(id));
    }

    public static void copyId(BaseFragment fragment, long id) {
        if (AndroidUtilities.addToClipboard(idText(id))) {
            BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.NexaIdCopied)).show();
        }
    }
}
