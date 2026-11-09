package app.nexagram;

import static org.telegram.messenger.LocaleController.getString;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * App icon on the home screen (Settings → NexaGram → Appearance → App icon).
 * Each icon is an <activity-alias> "app.nexagram.icon.<Id>" in the manifest; only one is enabled.
 * Pictures are made by nexagram/tools/make_alt_icons.py.
 */
public final class NexaIcons {

    public static final String DEFAULT = "default";
    public static final String[] IDS = {DEFAULT, "cosmos", "galaxy", "aurora", "nebula", "moon", "sunset", "ocean", "glass", "gold", "night"};
    private static final int[] TITLES = {R.string.NexaIconDefault, R.string.NexaIconCosmos, R.string.NexaIconGalaxy, R.string.NexaIconAurora,
            R.string.NexaIconNebula, R.string.NexaIconMoon, R.string.NexaIconSunset, R.string.NexaIconOcean, R.string.NexaIconGlass,
            R.string.NexaIconGold, R.string.NexaIconNight};
    private static final int[] PREVIEWS = {0, R.mipmap.nexa_alt_cosmos_bg, R.mipmap.nexa_alt_galaxy_bg, R.mipmap.nexa_alt_aurora_bg,
            R.mipmap.nexa_alt_nebula_bg, R.mipmap.nexa_alt_moon_bg, R.mipmap.nexa_alt_sunset_bg, R.mipmap.nexa_alt_ocean_bg,
            R.mipmap.nexa_alt_glass_bg, R.mipmap.nexa_alt_gold_bg, R.mipmap.nexa_alt_night_bg};

    private NexaIcons() {
    }

    private static ComponentName component(Context ctx, String id) {
        if (DEFAULT.equals(id)) {
            return new ComponentName(ctx.getPackageName(), "org.telegram.messenger.DefaultIcon");
        }
        return new ComponentName(ctx.getPackageName(), "app.nexagram.icon." + Character.toUpperCase(id.charAt(0)) + id.substring(1));
    }

    /** true when one of our extra icons is on (Telegram must not switch back to its default then). */
    public static boolean anyEnabled() {
        return !DEFAULT.equals(current());
    }

    public static String current() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            PackageManager pm = ctx.getPackageManager();
            for (int i = 1; i < IDS.length; i++) {
                if (pm.getComponentEnabledSetting(component(ctx, IDS[i])) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return IDS[i];
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return DEFAULT;
    }

    public static void set(String id) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        try {
            // turn the new one on first, so the app never has no icon at all
            pm.setComponentEnabledSetting(component(ctx, id), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            for (String other : IDS) {
                if (!other.equals(id)) {
                    pm.setComponentEnabledSetting(component(ctx, other), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    /** Row of icons to pick from. */
    public static class Picker extends HorizontalScrollView {

        private final LinearLayout row;
        private String selected = current();

        public Picker(Context context) {
            super(context);
            setHorizontalScrollBarEnabled(false);
            setClipToPadding(false);
            setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
            row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            addView(row, new FrameLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
            for (int i = 0; i < IDS.length; i++) {
                row.addView(item(context, i), LayoutHelper.createLinear(84, LayoutHelper.MATCH_PARENT));
            }
        }

        private View item(Context context, int index) {
            LinearLayout box = new LinearLayout(context);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            box.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(8));
            IconView icon = new IconView(context, index);
            box.addView(icon, LayoutHelper.createLinear(68, 68));
            TextView title = new TextView(context);
            title.setText(getString(TITLES[index]));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            title.setGravity(Gravity.CENTER);
            title.setLines(2);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            box.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 6, 4, 0));
            box.setOnClickListener(v -> {
                String id = IDS[index];
                if (id.equals(selected)) {
                    return;
                }
                selected = id;
                set(id);
                for (int i = 0; i < row.getChildCount(); i++) {
                    ((LinearLayout) row.getChildAt(i)).getChildAt(0).invalidate();
                }
            });
            return box;
        }

        private class IconView extends View {
            private final int index;
            private Drawable drawable;
            private final Path clip = new Path();
            private final RectF rect = new RectF();
            private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);

            IconView(Context context, int index) {
                super(context);
                this.index = index;
                try {
                    drawable = index == 0
                            ? context.getResources().getDrawable(context.getApplicationInfo().icon, null)
                            : context.getResources().getDrawable(PREVIEWS[index], null);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
                ring.setStyle(Paint.Style.STROKE);
                ring.setStrokeWidth(AndroidUtilities.dp(2.5f));
            }

            @Override
            protected void onDraw(Canvas canvas) {
                int w = getWidth(), h = getHeight();
                float inset = AndroidUtilities.dp(5);
                rect.set(inset, inset, w - inset, h - inset);
                float r = rect.width() * 0.26f;
                if (drawable != null) {
                    clip.rewind();
                    clip.addRoundRect(rect, r, r, Path.Direction.CW);
                    canvas.save();
                    canvas.clipPath(clip);
                    // the layers are 108dp wide and only the middle 72dp is the visible icon
                    float extra = rect.width() * 0.25f;
                    if (android.os.Build.VERSION.SDK_INT >= 26 && drawable instanceof android.graphics.drawable.AdaptiveIconDrawable) {
                        android.graphics.drawable.AdaptiveIconDrawable a = (android.graphics.drawable.AdaptiveIconDrawable) drawable;
                        drawLayer(canvas, a.getBackground(), extra);
                        drawLayer(canvas, a.getForeground(), extra);
                    } else if (index == 0) {
                        drawLayer(canvas, drawable, 0);
                    } else {
                        drawLayer(canvas, drawable, extra);
                    }
                    canvas.restore();
                }
                if (IDS[index].equals(selected)) {
                    ring.setColor(NexaTheme.accent());
                    float o = AndroidUtilities.dp(2);
                    rect.inset(-o, -o);
                    canvas.drawRoundRect(rect, r + o, r + o, ring);
                }
            }

            private void drawLayer(Canvas canvas, Drawable d, float extra) {
                if (d == null) {
                    return;
                }
                d.setBounds((int) (rect.left - extra), (int) (rect.top - extra), (int) (rect.right + extra), (int) (rect.bottom + extra));
                d.draw(canvas);
            }
        }
    }
}
