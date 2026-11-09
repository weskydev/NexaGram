package app.nexagram;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.ContactsActivity;
import org.telegram.ui.DialogsActivity;

/**
 * NexaGram bottom bar: a round "new message" button docked right next to
 * Telegram's tab bar. It replaces the floating compose button of the chats list
 * (and the floating story camera, which moves to the chats top bar).
 */
public final class NexaMainTabs {

    /** Action bar item id for the story camera in the chats top bar. */
    public static final int CAMERA_ITEM_ID = 9101;

    private static final int BUTTON_DP = DialogsActivity.MAIN_TABS_HEIGHT;      // same height as the bar
    private static final int GAP_DP = 8;
    private static final int SIDE_DP = DialogsActivity.MAIN_TABS_MARGIN;
    /** Tab bar max width, same as MainTabsActivity sets. */
    private static final int BAR_MAX_DP = 328 + DialogsActivity.MAIN_TABS_MARGIN * 2;
    /** Space the button takes to the right of the bar (bar background is already inset by MAIN_TABS_MARGIN). */
    private static final int BUTTON_SPACE_DP = BUTTON_DP + SIDE_DP + GAP_DP - DialogsActivity.MAIN_TABS_MARGIN;

    private NexaMainTabs() {
    }

    /** True when the chats list lives in the main tabs and our docked button replaces its floating ones. */
    public static boolean replacesFloatingButtons(boolean hasMainTabs) {
        return hasMainTabs && NexaDesign.dockedCompose();
    }

    /**
     * Makes room for the button to the right of the tab bar and adds it.
     * Call right after the tab bar was added to its wrapper.
     */
    public static View attach(BaseFragment fragment, FrameLayout tabsWrapper, View tabsView) {
        Context context = tabsWrapper.getContext();
        tabsView.setForeground(new BarAccent());
        if (!NexaDesign.dockedCompose()) {
            return null;
        }

        // FrameLayout's CENTER_HORIZONTAL shifts a child by -rightMargin, so pin the bar to the left
        // and center the bar + button pair ourselves (matters on wide screens only).
        FrameLayout.LayoutParams tabsParams = (FrameLayout.LayoutParams) tabsView.getLayoutParams();
        tabsParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        tabsParams.rightMargin = dp(BUTTON_SPACE_DP);
        tabsView.setLayoutParams(tabsParams);

        FrameLayout button = new FrameLayout(context);
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{NexaTheme.tint(0xFF8F78FF), NexaTheme.tint(0xFF6A4FF0)});
        background.setShape(GradientDrawable.OVAL);
        button.setBackground(background);
        button.setElevation(dp(4));
        if (Build.VERSION.SDK_INT >= 28) {
            button.setOutlineSpotShadowColor(NexaTheme.accent());
            button.setOutlineAmbientShadowColor(NexaTheme.accent());
        }
        button.setContentDescription(LocaleController.getString(R.string.NewMessageTitle));

        ImageView icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        icon.setImageResource(R.drawable.filled_fab_compose_32);
        icon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        button.addView(icon, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        ScaleStateListAnimator.apply(button, .08f, 1.4f);
        button.setTag(icon);
        button.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(icon.getTag())) {
                openNewContact(fragment);
            } else {
                openNewMessage(fragment);
            }
        });

        tabsWrapper.addView(button, LayoutHelper.createFrame(BUTTON_DP, BUTTON_DP, Gravity.BOTTOM | Gravity.RIGHT,
                0, 0, SIDE_DP, DialogsActivity.MAIN_TABS_MARGIN));
        tabsWrapper.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> center(tabsWrapper, tabsView, button));
        return button;
    }

    private static void center(FrameLayout wrapper, View tabsView, View button) {
        int available = wrapper.getWidth() - wrapper.getPaddingLeft() - wrapper.getPaddingRight();
        int pair = Math.min(available, dp(BAR_MAX_DP + BUTTON_SPACE_DP));
        int side = Math.max(0, (available - pair) / 2);
        FrameLayout.LayoutParams tabsParams = (FrameLayout.LayoutParams) tabsView.getLayoutParams();
        FrameLayout.LayoutParams buttonParams = (FrameLayout.LayoutParams) button.getLayoutParams();
        if (tabsParams.leftMargin != side) {
            tabsParams.leftMargin = side;
            tabsParams.rightMargin = side + dp(BUTTON_SPACE_DP);
            buttonParams.rightMargin = side + dp(SIDE_DP);
            wrapper.post(() -> {
                tabsView.setLayoutParams(tabsParams);
                button.setLayoutParams(buttonParams);
            });
        }
    }

    /** NexaGram bottom bar: thin accent outline and a faint accent tint over Telegram's glass. */
    private static final class BarAccent extends android.graphics.drawable.Drawable {
        private final android.graphics.Paint fill = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint stroke = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF rect = new android.graphics.RectF();

        BarAccent() {
            stroke.setStyle(android.graphics.Paint.Style.STROKE);
            stroke.setStrokeWidth(dp(1));
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            if (!NexaDesign.get(NexaDesign.TAB_BAR)) {
                return;
            }
            boolean dark = org.telegram.ui.ActionBar.Theme.isCurrentThemeDark();
            int accent = NexaChatList.accent();
            float inset = dp(DialogsActivity.MAIN_TABS_MARGIN - 0.334f) + dp(0.5f);
            rect.set(getBounds());
            rect.inset(inset, inset);
            float r = rect.height() / 2f;
            fill.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accent, dark ? 22 : 14));
            stroke.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accent, dark ? 110 : 80));
            canvas.drawRoundRect(rect, r, r, fill);
            canvas.drawRoundRect(rect, r, r, stroke);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    /** Keeps the button in sync with the tab bar when Telegram hides/shows it. */
    public static void sync(View button, View tabsView) {
        if (button == null) {
            return;
        }
        button.setAlpha(tabsView.getAlpha());
        button.setVisibility(tabsView.getVisibility());
        button.setEnabled(tabsView.getVisibility() == View.VISIBLE && tabsView.getAlpha() > 0.99f);
    }

    /** On the Contacts tab the button adds a contact; elsewhere it writes a new message. */
    public static void setContactsMode(View button, boolean contacts, boolean animated) {
        if (button == null || !(button.getTag() instanceof ImageView)) {
            return;
        }
        ImageView icon = (ImageView) button.getTag();
        if (Boolean.valueOf(contacts).equals(icon.getTag())) {
            return;
        }
        icon.setTag(contacts);
        int res = contacts ? R.drawable.msg_contact_add : R.drawable.filled_fab_compose_32;
        String description = LocaleController.getString(contacts ? R.string.CreateNewContact : R.string.NewMessageTitle);
        button.setContentDescription(description);
        if (!animated) {
            icon.setImageResource(res);
            return;
        }
        icon.animate().cancel();
        icon.animate().alpha(0f).scaleX(.6f).scaleY(.6f).setDuration(120).withEndAction(() -> {
            icon.setImageResource(res);
            icon.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
        }).start();
    }

    /** Same action as Telegram's floating "add contact" button. */
    public static void openNewContact(BaseFragment fragment) {
        if (MessagesController.getInstance(fragment.getCurrentAccount()).isFrozen()) {
            org.telegram.ui.AccountFrozenAlert.show(fragment.getCurrentAccount());
            return;
        }
        if (fragment.getParentActivity() != null) {
            new org.telegram.ui.NewContactBottomSheet(fragment, fragment.getParentActivity()).show();
        }
    }

    /** Same action as Telegram's floating "compose" button. */
    public static void openNewMessage(BaseFragment fragment) {
        if (MessagesController.getInstance(fragment.getCurrentAccount()).isFrozen()) {
            org.telegram.ui.AccountFrozenAlert.show(fragment.getCurrentAccount());
            return;
        }
        Bundle args = new Bundle();
        args.putBoolean("destroyAfterSelect", true);
        fragment.presentFragment(new ContactsActivity(args));
    }

    /** Tints the story camera icon of the chats top bar like the other header icons. */
    public static int cameraIcon() {
        return R.drawable.outline_fab_story_24;
    }
}
