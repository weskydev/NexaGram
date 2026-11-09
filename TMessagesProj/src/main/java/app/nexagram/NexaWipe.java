package app.nexagram;

import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;

/**
 * "Erase all NexaGram data": logs out of every account on Telegram's servers
 * (so the sessions really end), then deletes everything the app keeps on the phone.
 */
public final class NexaWipe {

    private NexaWipe() {
    }

    /** Two questions, the second one with a red button, then the wipe. */
    public static void ask(Activity activity) {
        if (activity == null) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(getString(R.string.NexaWipe))
                .setMessage(getString(R.string.NexaWipeText))
                .setPositiveButton(getString(R.string.NexaWipeContinue), (d, w) -> {
                    AlertDialog second = new AlertDialog.Builder(activity)
                            .setTitle(getString(R.string.NexaWipeSure))
                            .setMessage(getString(R.string.NexaWipeSureText))
                            .setPositiveButton(getString(R.string.NexaWipeButton), (d2, w2) -> wipe(activity))
                            .setNegativeButton(getString(R.string.Cancel), null)
                            .create();
                    second.show();
                    android.view.View b = second.getButton(AlertDialog.BUTTON_POSITIVE);
                    if (b instanceof android.widget.TextView) {
                        ((android.widget.TextView) b).setTextColor(0xFFE53935);
                    }
                })
                .setNegativeButton(getString(R.string.Cancel), null)
                .show();
    }

    private static void wipe(Activity activity) {
        AlertDialog progress = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
        progress.setCanCancel(false);
        progress.show();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (AccountInstance.getInstance(a).getUserConfig().isClientActivated()) {
                AccountInstance.getInstance(a).getMessagesController().performLogout(1);
            }
        }
        // give the log out requests a moment to reach Telegram, then erase the app's data
        AndroidUtilities.runOnUIThread(() -> {
            ActivityManager am = (ActivityManager) activity.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.clearApplicationUserData();
            }
        }, 3000);
    }
}
