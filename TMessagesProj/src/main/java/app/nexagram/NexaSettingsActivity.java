package app.nexagram;

import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/** Settings → NexaGram. Everything NexaGram-specific lives here. */
public class NexaSettingsActivity extends UniversalFragment {

    public static final String SOURCE_URL = "https://github.com/weskydev/NexaGram";
    /** Row id in Telegram's main Settings list; far away from Telegram's own ids. */
    public static final int SETTINGS_ROW_ID = 9001;

    private static final int ID_BRAND_COLORS = 1;
    private static final int ID_VERSION = 2;
    private static final int ID_SOURCE = 3;
    private static final int ID_DESIGN_RESET = 4;
    /** Design switches use ids ID_DESIGN_FIRST + index in NexaDesign.ALL. */
    private static final int ID_DESIGN_FIRST = 100;
    private static final int[] DESIGN_TITLES = {
            R.string.NexaChatCards, R.string.NexaSquareAvatars, R.string.NexaPinnedTint,
            R.string.NexaAccentSearch, R.string.NexaTabBar, R.string.NexaDockedCompose
    };

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.NexaSettings);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString(R.string.NexaAppearance)));
        items.add(UItem.asCheck(ID_BRAND_COLORS, getString(R.string.NexaBrandColors)).setChecked(NexaTheme.isEnabled()));
        items.add(UItem.asShadow(getString(R.string.NexaBrandColorsInfo)));

        items.add(UItem.asHeader(getString(R.string.NexaDesign)));
        for (int i = 0; i < NexaDesign.ALL.length; i++) {
            items.add(UItem.asCheck(ID_DESIGN_FIRST + i, getString(DESIGN_TITLES[i])).setChecked(NexaDesign.get(NexaDesign.ALL[i])));
        }
        items.add(UItem.asButton(ID_DESIGN_RESET, R.drawable.msg_reset, getString(R.string.NexaDesignReset)));
        items.add(UItem.asShadow(getString(R.string.NexaDesignInfo)));

        items.add(UItem.asHeader(getString(R.string.NexaFeatures)));
        items.add(UItem.asShadow(getString(R.string.NexaFeaturesSoon)));

        items.add(UItem.asHeader(getString(R.string.NexaAbout)));
        items.add(UItem.asButton(ID_VERSION, R.drawable.nexa_logo, getString(R.string.NexaVersion), BuildVars.BUILD_VERSION_STRING));
        items.add(UItem.asButton(ID_SOURCE, R.drawable.msg2_link2, getString(R.string.NexaSourceCode), "GitHub"));
        items.add(UItem.asShadow(getString(R.string.NexaAboutInfo)));
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
        } else if (item.id == ID_VERSION && BuildVars.DEBUG_VERSION) {
            // debug builds only: preview the welcome screen without logging out
            presentFragment(new NexaIntroActivity());
        } else if (item.id == ID_SOURCE) {
            Browser.openUrl(getParentActivity(), SOURCE_URL);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_VERSION && BuildVars.DEBUG_VERSION) {
            // debug builds only: Telegram's original welcome screen, for side-by-side comparison
            presentFragment(new org.telegram.ui.IntroActivity());
            return true;
        }
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
