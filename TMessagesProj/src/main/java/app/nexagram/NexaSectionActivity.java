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
    private static final int ID_ROUND_VIDEO = 5;
    private static final int ID_SELECT_ALL = 6;
    private static final int ID_CONFIRM_CALL = 7;
    private static final int ID_HIDE_KEYBOARD = 8;
    private static final int ID_SHOW_ID = 9;
    private static final int ID_REG_DATE = 20;
    /** Theme colors use ids ID_COLOR_FIRST + NexaTheme.VIOLET..GOLD. */
    private static final int ID_COLOR_FIRST = 300;
    private static final int[] COLOR_NAMES = {R.string.NexaColorViolet, R.string.NexaColorBlue, R.string.NexaColorTeal, R.string.NexaColorGreen,
            R.string.NexaColorPink, R.string.NexaColorRed, R.string.NexaColorOrange, R.string.NexaColorGold};

    /** "● Violet" with the dot in that color. */
    private static CharSequence colorName(int which) {
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder("●  ").append(getString(COLOR_NAMES[which]));
        sb.setSpan(new android.text.style.ForegroundColorSpan(NexaTheme.accentOf(which)), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }
    private static final int ID_TRANSLATE = 21;
    private static final int ID_HIDE_STORIES = 22;
    private static final int ID_NOTE_TOP = 23;
    private static final int ID_WIPE = 24;
    /** Chat features with a plain on/off switch use ids ID_CHAT_FIRST + index in CHAT_KEYS. */
    private static final int ID_CHAT_FIRST = 200;
    private static final String[] CHAT_KEYS = {NexaChats.REPEAT, NexaChats.DETAILS, NexaChats.FIRST_MESSAGE, NexaChats.CONFIRM_VOICE, NexaChats.CONFIRM_STICKER};
    private static final int[] CHAT_TITLES = {R.string.NexaRepeatSwitch, R.string.NexaDetailsSwitch, R.string.NexaFirstMessageSwitch, R.string.NexaConfirmVoice, R.string.NexaConfirmSticker};
    private static final int[] CHAT_INFOS = {R.string.NexaRepeatInfo, R.string.NexaDetailsInfo, R.string.NexaFirstMessageInfo, R.string.NexaConfirmVoiceInfo, R.string.NexaConfirmStickerInfo};
    /** Phone modes use ids ID_PHONE_FIRST + NexaProfile.PHONE_*. */
    private static final int ID_PHONE_FIRST = 10;
    /** Design switches use ids ID_DESIGN_FIRST + index in NexaDesign.ALL. */
    private static final int ID_DESIGN_FIRST = 100;
    private static final int[] DESIGN_TITLES = {
            R.string.NexaChatCards, R.string.NexaSquareAvatars, R.string.NexaPinnedTint,
            R.string.NexaAccentSearch, R.string.NexaTabBar, R.string.NexaDockedCompose,
            R.string.NexaContactsDesign, R.string.NexaSettingsDesign, R.string.NexaProfileLine,
            R.string.NexaMusicBlock
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
            int chosen = NexaTheme.choice();
            for (int i = 0; i < NexaTheme.colorCount(); i++) {
                items.add(UItem.asRadio(ID_COLOR_FIRST + i, colorName(i)).setChecked(chosen == i));
            }
            items.add(UItem.asShadow(getString(R.string.NexaBrandColorsInfo)));

            items.add(UItem.asHeader(getString(R.string.NexaDesign)));
            for (int i = 0; i < NexaDesign.ALL.length; i++) {
                items.add(UItem.asCheck(ID_DESIGN_FIRST + i, getString(DESIGN_TITLES[i])).setChecked(NexaDesign.get(NexaDesign.ALL[i])));
            }
            items.add(UItem.asButton(ID_DESIGN_RESET, R.drawable.msg_reset, getString(R.string.NexaDesignReset)));
            items.add(UItem.asShadow(getString(R.string.NexaDesignInfo)));
        } else if (section == CHATS) {
            items.add(UItem.asCheck(ID_ROUND_VIDEO, getString(R.string.NexaRoundVideo)).setChecked(NexaRoundVideo.isEnabled()));
            items.add(UItem.asShadow(getString(R.string.NexaRoundVideoInfo)));
            items.add(UItem.asCheck(ID_CONFIRM_CALL, getString(R.string.NexaConfirmCall)).setChecked(NexaDesign.get(NexaChats.CONFIRM_CALL)));
            items.add(UItem.asShadow(getString(R.string.NexaConfirmCallInfo)));
            items.add(UItem.asCheck(ID_HIDE_KEYBOARD, getString(R.string.NexaHideKeyboard)).setChecked(NexaChats.hideKeyboardOnScroll()));
            items.add(UItem.asShadow(getString(R.string.NexaHideKeyboardInfo)));
            items.add(UItem.asCheck(ID_TRANSLATE, getString(R.string.NexaTranslate)).setChecked(getMessagesController().getTranslateController().isContextTranslateEnabled()));
            items.add(UItem.asShadow(getString(R.string.NexaTranslateInfo)));
            for (int i = 0; i < CHAT_KEYS.length; i++) {
                items.add(UItem.asCheck(ID_CHAT_FIRST + i, getString(CHAT_TITLES[i])).setChecked(NexaDesign.get(CHAT_KEYS[i])));
                items.add(UItem.asShadow(getString(CHAT_INFOS[i])));
            }
        } else if (section == CHAT_LIST) {
            items.add(UItem.asCheck(ID_SELECT_ALL, getString(R.string.NexaSelectAllChats)).setChecked(NexaSelectAll.isEnabled()));
            items.add(UItem.asShadow(getString(R.string.NexaSelectAllInfo)));
            items.add(UItem.asCheck(ID_HIDE_STORIES, getString(R.string.NexaHideStories)).setChecked(NexaDesign.get(NexaChats.HIDE_STORIES)));
            items.add(UItem.asShadow(getString(R.string.NexaHideStoriesInfo)));
            items.add(UItem.asShadow(getString(R.string.NexaFolderReadInfo)));
        } else {
            items.add(UItem.asHeader(getString(R.string.NexaPhoneShown)));
            int mode = NexaProfile.phoneMode();
            items.add(UItem.asRadio(ID_PHONE_FIRST + NexaProfile.PHONE_FULL, getString(R.string.NexaPhoneFull)).setChecked(mode == NexaProfile.PHONE_FULL));
            items.add(UItem.asRadio(ID_PHONE_FIRST + NexaProfile.PHONE_CODE, getString(R.string.NexaPhoneCode)).setChecked(mode == NexaProfile.PHONE_CODE));
            items.add(UItem.asRadio(ID_PHONE_FIRST + NexaProfile.PHONE_HIDDEN, getString(R.string.NexaPhoneHidden)).setChecked(mode == NexaProfile.PHONE_HIDDEN));
            items.add(UItem.asShadow(getString(R.string.NexaPhoneInfo)));
            items.add(UItem.asCheck(ID_SHOW_ID, getString(R.string.NexaShowId)).setChecked(NexaChats.showId()));
            items.add(UItem.asShadow(getString(R.string.NexaShowIdInfo)));
            items.add(UItem.asCheck(ID_REG_DATE, getString(R.string.NexaShowRegDate)).setChecked(NexaChats.showRegDate()));
            items.add(UItem.asShadow(getString(R.string.NexaShowRegDateInfo)));
            items.add(UItem.asCheck(ID_NOTE_TOP, getString(R.string.NexaNoteTop)).setChecked(NexaDesign.get(NexaChats.NOTE_TOP)));
            items.add(UItem.asShadow(getString(R.string.NexaNoteTopInfo)));
            items.add(UItem.asButton(ID_WIPE, R.drawable.msg_delete, getString(R.string.NexaWipe)).red());
            items.add(UItem.asShadow(getString(R.string.NexaWipeInfo)));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ID_COLOR_FIRST && item.id < ID_COLOR_FIRST + NexaTheme.colorCount()) {
            if (NexaTheme.choice() != item.id - ID_COLOR_FIRST) {
                NexaTheme.setChoice(item.id - ID_COLOR_FIRST);
                listView.adapter.update(true);
                askRestart(R.string.NexaRestartText);
            }
        } else if (item.id >= ID_DESIGN_FIRST && item.id < ID_DESIGN_FIRST + NexaDesign.ALL.length) {
            String key = NexaDesign.ALL[item.id - ID_DESIGN_FIRST];
            NexaDesign.set(key, !NexaDesign.get(key));
            listView.adapter.update(true);
            if (NexaDesign.needsRestart(key)) {
                askRestart(R.string.NexaRestartDesignText);
            }
        } else if (item.id == ID_ROUND_VIDEO) {
            NexaDesign.set(NexaRoundVideo.KEY, !NexaRoundVideo.isEnabled());
            listView.adapter.update(true);
        } else if (item.id >= ID_PHONE_FIRST && item.id <= ID_PHONE_FIRST + NexaProfile.PHONE_HIDDEN) {
            NexaProfile.setPhoneMode(item.id - ID_PHONE_FIRST);
            listView.adapter.update(true);
        } else if (item.id == ID_CONFIRM_CALL || item.id == ID_HIDE_KEYBOARD || item.id == ID_SHOW_ID || item.id == ID_REG_DATE) {
            String key = item.id == ID_CONFIRM_CALL ? NexaChats.CONFIRM_CALL : item.id == ID_HIDE_KEYBOARD ? NexaChats.HIDE_KEYBOARD : item.id == ID_SHOW_ID ? NexaChats.SHOW_ID : NexaChats.REG_DATE;
            NexaDesign.set(key, !NexaDesign.get(key));
            listView.adapter.update(true);
        } else if (item.id == ID_HIDE_STORIES || item.id == ID_NOTE_TOP) {
            String key = item.id == ID_HIDE_STORIES ? NexaChats.HIDE_STORIES : NexaChats.NOTE_TOP;
            NexaDesign.set(key, !NexaDesign.get(key));
            listView.adapter.update(true);
        } else if (item.id == ID_WIPE) {
            NexaWipe.ask(getParentActivity());
        } else if (item.id == ID_TRANSLATE) {
            org.telegram.messenger.TranslateController tc = getMessagesController().getTranslateController();
            tc.setContextTranslateEnabled(!tc.isContextTranslateEnabled());
            listView.adapter.update(true);
        } else if (item.id >= ID_CHAT_FIRST && item.id < ID_CHAT_FIRST + CHAT_KEYS.length) {
            String key = CHAT_KEYS[item.id - ID_CHAT_FIRST];
            NexaDesign.set(key, !NexaDesign.get(key));
            listView.adapter.update(true);
        } else if (item.id == ID_SELECT_ALL) {
            NexaDesign.set(NexaSelectAll.KEY, !NexaSelectAll.isEnabled());
            listView.adapter.update(true);
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
