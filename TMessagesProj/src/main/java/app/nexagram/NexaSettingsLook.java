package app.nexagram;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.SettingsActivity;

import static org.telegram.messenger.LocaleController.getString;

/**
 * NexaGram look of the Settings tab: soft tinted icon tiles instead of bright
 * ones, and a NexaGram card on top. Off → Telegram's look.
 */
public final class NexaSettingsLook {

    private NexaSettingsLook() {
    }

    public static boolean isEnabled() {
        return NexaDesign.get(NexaDesign.SETTINGS_DESIGN);
    }

    /**
     * Soft tile: the row's own color, light, with the icon in that color.
     * Returns false when Telegram's bright tile should be drawn.
     */
    public static boolean tile(SettingsActivity.SettingCell.Background background, ImageView icon, int top, int bottom, int iconRes, boolean dark) {
        if (!isEnabled() || (top == 0 && bottom == 0) || iconRes == R.drawable.nexa_logo) {
            icon.setColorFilter(null);
            return false;
        }
        background.setColor(Theme.multAlpha(top, dark ? 0.22f : 0.14f), Theme.multAlpha(bottom, dark ? 0.28f : 0.20f));
        icon.setColorFilter(new PorterDuffColorFilter(dark ? top : bottom, PorterDuff.Mode.SRC_IN));
        return true;
    }

    /** The NexaGram card shown above the Settings list. */
    public static FrameLayout banner(Context context, Runnable onClick) {
        FrameLayout root = new FrameLayout(context);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{NexaTheme.tint(0xFF8C73FF), NexaTheme.tint(0xFF6A4FF0), NexaTheme.tint(0xFF8E45E8)});
        bg.setCornerRadius(dp(20));
        card.setBackground(bg);
        if (NexaGlass.on()) {
            bg.setAlpha(0xC8);
            card.setForeground(NexaGlass.rimDrawable(20));
        }
        card.setPadding(dp(14), 0, dp(14), 0);
        card.setOnClickListener(v -> onClick.run());
        ScaleStateListAnimator.apply(card, 0.03f, 1.2f);

        FrameLayout logoBox = new FrameLayout(context);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setShape(GradientDrawable.OVAL);
        logoBg.setColor(0x33FFFFFF);
        logoBox.setBackground(logoBg);
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.nexa_logo);
        logo.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        logoBox.addView(logo, LayoutHelper.createFrame(26, 26, Gravity.CENTER));
        card.addView(logoBox, LayoutHelper.createLinear(46, 46));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(context);
        title.setText(getString(R.string.NexaSettings));
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        title.setTypeface(AndroidUtilities.bold());
        title.setSingleLine();
        texts.addView(title);
        TextView subtitle = new TextView(context);
        subtitle.setText(getString(R.string.NexaSettingsBannerInfo));
        subtitle.setTextColor(0xD9FFFFFF);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitle.setSingleLine();
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        card.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 14, 0, 8, 0));

        ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.msg_arrowright);
        arrow.setColorFilter(new PorterDuffColorFilter(0xCCFFFFFF, PorterDuff.Mode.SRC_IN));
        card.addView(arrow, LayoutHelper.createLinear(24, 24));

        root.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 76, Gravity.TOP, 12, 4, 12, 8));
        return root;
    }

    /** Height of {@link #banner} in the list, dp. */
    public static final int BANNER_HEIGHT_DP = 88;
}
