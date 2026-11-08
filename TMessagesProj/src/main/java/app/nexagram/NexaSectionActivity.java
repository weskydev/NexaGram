package app.nexagram;

import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * One section of Settings → NexaGram. Each feature adds its switches and options
 * to its section here, so the entry page stays short.
 */
public class NexaSectionActivity extends UniversalFragment {

    public static final int APPEARANCE = 0;
    public static final int CHATS = 1;
    public static final int CHAT_LIST = 2;
    public static final int ACCOUNTS = 3;
    public static final int COUNT = 4;

    private static final int ID_BRAND_COLORS = 1;
    private static final int ID_DESIGN_RESET = 4;
    /** Design switches use ids ID_DESIGN_FIRST + index in NexaDesign.ALL. */
    private static final int ID_DESIGN_FIRST = 100;
    private static final int[] DESIGN_TITLES = {
            R.string.NexaChatCards, R.string.NexaSquareAvatars, R.string.NexaPinnedTint,
            R.string.NexaAccentSearch, R.string.NexaTabBar, R.string.NexaDockedCompose,
            R.string.NexaContactsDesign
    };

    private final int section;

    public NexaSectionActivity(int section) {
        this.section = section;
    }

    public static int title(int section) {
        switch (section) {
            case APPEARANCE: return R.string.NexaAppearance;
            case CHATS: return R.string.NexaSectionChats;
            case CHAT_LIST: return R.string.NexaSectionChatList;
            default: return R.string.NexaSectionAccounts;
        }
    }

    public static int icon(int section) {
        switch (section) {
            case APPEARANCE: return R.drawable.msg_palette;
            case CHATS: return R.drawable.msg_msgbubble3;
            case CHAT_LIST: return R.drawable.msg_list;
            default: return R.drawable.msg_openprofile;
        }
    }

    @Override
    protected CharSequence getTitle() {
        return getString(title(section));
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (section == APPEARANCE) {
            items.add(UItem.asHeader(getString(R.string.NexaColors)));
            items.add(UItem.asCheck(ID_BRAND_COLORS, getString(R.string.NexaBrandColors)).setChecked(NexaTheme.isEnabled()));
            items.add(UItem.asShadow(getString(R.string.NexaBrandColorsInfo)));

            items.add(UItem.asHeader(getString(R.string.NexaDesign)));
            for (int i = 0; i < NexaDesign.ALL.length; i++) {
                items.add(UItem.asCheck(ID_DESIGN_FIRST + i, getString(DESIGN_TITLES[i])).setChecked(NexaDesign.get(NexaDesign.ALL[i])));
            }
            items.add(UItem.asButton(ID_DESIGN_RESET, R.drawable.msg_reset, getString(R.string.NexaDesignReset)));
            items.add(UItem.asShadow(getString(R.string.NexaDesignInfo)));
        } else {
            // features for this section are on the way (see nexagram/FEATURES.md)
            items.add(UItem.asShadow(getString(R.string.NexaFeaturesSoon)));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_BRAND_COLORS) {
            NexaTheme.setEnabled(!NexaTheme.isEnabled());
            listView.adapter.update(true);
            askRestart(R.string.NexaRestartText);
        } else if (item.id >= ID_DESIGN_FIRST && item.id < ID_DESIGN_FIRST + NexaDesign.ALL.length) {
            String key = NexaDesign.ALL[item.id - ID_DESIGN_FIRST];
            NexaDesign.set(key, !NexaDesign.get(key));
            listView.adapter.update(true);
            if (NexaDesign.needsRestart(key)) {
                askRestart(R.string.NexaRestartDesignText);
            }
        } else if (item.id == ID_DESIGN_RESET) {
            boolean restart = false;
            for (String key : NexaDesign.ALL) {
                if (NexaDesign.get(key)) {
                    restart |= NexaDesign.needsRestart(key);
                    NexaDesign.set(key, false);
                }
            }
            listView.adapter.update(true);
            if (restart) {
                askRestart(R.string.NexaRestartDesignText);
            }
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private void askRestart(int text) {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        showDialog(new AlertDialog.Builder(activity, getResourceProvider())
                .setTitle(getString(R.string.NexaRestartTitle))
                .setMessage(getString(text))
                .setPositiveButton(getString(R.string.NexaRestart), (dialog, which) -> restartApp(activity))
                .setNegativeButton(getString(R.string.NexaLater), null)
                .create());
    }

    private static void restartApp(Activity activity) {
        Context context = activity.getApplicationContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        activity.finishAffinity();
        Runtime.getRuntime().exit(0);
    }
}
