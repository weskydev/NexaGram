package app.nexagram;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationsController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

/**
 * "Select all" in the chat list selection mode, and bulk read / mute in small
 * batches with pauses, so a few hundred chats don't hit Telegram's flood limits.
 */
public final class NexaSelectAll {

    public static final String KEY = "feature_select_all";
    public static final int ITEM_ID = 9102;

    /** Up to this many chats Telegram's own code handles the action as usual. */
    private static final int BATCH = 20;
    private static final long PAUSE_MS = 1200;

    private NexaSelectAll() {
    }

    public static boolean isEnabled() {
        return NexaDesign.get(KEY);
    }

    /** Adds every chat of the shown list to the selection. Returns how many were added. */
    public static int selectAll(ArrayList<TLRPC.Dialog> shown, ArrayList<Long> selected) {
        int added = 0;
        for (int i = 0; i < shown.size(); i++) {
            TLRPC.Dialog dialog = shown.get(i);
            if (dialog == null || dialog instanceof TLRPC.TL_dialogFolder) {
                continue;
            }
            long did = dialog.id;
            if (did == 0 || DialogObject.isFolderDialogId(did) || selected.contains(did)) {
                continue;
            }
            selected.add(did);
            added++;
        }
        return added;
    }

    /**
     * Marks as read or mutes / unmutes a big selection in batches.
     * Returns false for small selections: Telegram's own code does them.
     */
    public static boolean bulk(BaseFragment fragment, ArrayList<Long> selected, boolean read, boolean unmute) {
        if (!isEnabled() || selected.size() <= BATCH) {
            return false;
        }
        int account = fragment.getCurrentAccount();
        ArrayList<Long> ids = new ArrayList<>(selected);
        runBatch(account, ids, 0, read, unmute);
        if (!read) {
            BulletinFactory.createMuteBulletin(fragment, !unmute, null).show();
        }
        return true;
    }

    private static void runBatch(int account, ArrayList<Long> ids, int from, boolean read, boolean unmute) {
        MessagesController controller = AccountInstance.getInstance(account).getMessagesController();
        NotificationsController notifications = AccountInstance.getInstance(account).getNotificationsController();
        int to = Math.min(ids.size(), from + BATCH);
        for (int i = from; i < to; i++) {
            long did = ids.get(i);
            if (read) {
                readDialog(controller, did);
            } else if (unmute) {
                if (controller.isDialogMuted(did, 0)) {
                    notifications.setDialogNotificationsSettings(did, 0, NotificationsController.SETTING_MUTE_UNMUTE);
                }
            } else if (!controller.isDialogMuted(did, 0)) {
                notifications.setDialogNotificationsSettings(did, 0, NotificationsController.SETTING_MUTE_FOREVER);
            }
        }
        if (to < ids.size()) {
            AndroidUtilities.runOnUIThread(() -> runBatch(account, ids, to, read, unmute), PAUSE_MS);
        }
    }

    private static void readDialog(MessagesController controller, long did) {
        TLRPC.Dialog dialog = controller.dialogs_dict.get(did);
        if (dialog == null) {
            return;
        }
        boolean forum = controller.isForum(did) || controller.isMonoForumWithManageRights(did);
        if (forum) {
            controller.markAllTopicsAsRead(did);
        }
        // nothing to send for chats that are already read
        if (!forum && dialog.unread_count == 0 && !dialog.unread_mark && dialog.unread_mentions_count == 0) {
            return;
        }
        controller.markMentionsAsRead(did, 0);
        controller.markDialogAsRead(did, dialog.top_message, dialog.top_message, dialog.last_message_date, false, 0, 0, true, 0);
    }
}
