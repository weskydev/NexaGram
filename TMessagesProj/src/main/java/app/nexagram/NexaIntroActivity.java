package app.nexagram;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.SimpleThemeDescription;
import org.telegram.ui.Components.voip.CellFlickerDrawable;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LoginActivity;

import java.util.ArrayList;

/**
 * NexaGram welcome screen. Replaces Telegram's IntroActivity (see the hook in
 * LaunchActivity) with our own design; behaviour is the same: pages, day/night
 * toggle, "Start Messaging" opens login, "Continue in <language>" link.
 */
public class NexaIntroActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int[] IMAGES = {
            R.drawable.nexa_intro_1, R.drawable.nexa_intro_2, R.drawable.nexa_intro_3,
            R.drawable.nexa_intro_4, R.drawable.nexa_intro_5, R.drawable.nexa_intro_6
    };
    private static final int[] TITLES = {
            R.string.NexaIntro1Title, R.string.NexaIntro2Title, R.string.NexaIntro3Title,
            R.string.NexaIntro4Title, R.string.NexaIntro5Title, R.string.NexaIntro6Title
    };
    private static final int[] TEXTS = {
            R.string.NexaIntro1Text, R.string.NexaIntro2Text, R.string.NexaIntro3Text,
            R.string.NexaIntro4Text, R.string.NexaIntro5Text, R.string.NexaIntro6Text
    };
    private static final int PAGES = IMAGES.length;

    private static final int BUTTON_TOP = 0xFF8F78FF;
    private static final int BUTTON_BOTTOM = 0xFF6A4FF0;

    private final int currentAccount = UserConfig.selectedAccount;

    private BackgroundLayout rootView;
    private ViewPager viewPager;
    private DotsView dotsView;
    private TextView startButton;
    private TextView switchLanguageTextView;
    private RLottieImageView themeIconView;
    private RLottieDrawable darkThemeDrawable;

    private LocaleController.LocaleInfo localeInfo;
    private boolean startPressed;
    private boolean destroyed;
    private boolean isOnLogout;

    public NexaIntroActivity setOnLogout() {
        isOnLogout = true;
        return this;
    }

    @Override
    public boolean onFragmentCreate() {
        MessagesController.getGlobalMainSettings().edit().putLong("intro_crashed_time", System.currentTimeMillis()).apply();
        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setAddToContainer(false);

        rootView = new BackgroundLayout(context);

        viewPager = new ViewPager(context);
        viewPager.setAdapter(new PagesAdapter());
        viewPager.setOffscreenPageLimit(1);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                dotsView.setProgress(position + positionOffset);
                rootView.setProgress(position + positionOffset);
            }
        });
        rootView.addView(viewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        dotsView = new DotsView(context);
        rootView.addView(dotsView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 8, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, 166));

        startButton = createStartButton(context);
        rootView.addView(startButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 54, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 24, 0, 24, 82));

        switchLanguageTextView = new TextView(context);
        switchLanguageTextView.setGravity(Gravity.CENTER);
        switchLanguageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        switchLanguageTextView.setTypeface(AndroidUtilities.bold());
        switchLanguageTextView.setPadding(dp(16), 0, dp(16), 0);
        switchLanguageTextView.setOnClickListener(v -> switchLanguage(v.getContext()));
        rootView.addView(switchLanguageTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 40, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, 28));

        FrameLayout.LayoutParams toggleParams = LayoutHelper.createFrame(56, 56, Gravity.TOP | Gravity.RIGHT, 0, 4, 6, 0);
        toggleParams.topMargin += AndroidUtilities.isTablet() ? 0 : AndroidUtilities.statusBarHeight;
        rootView.addView(createThemeToggle(context), toggleParams);

        fragmentView = rootView;

        NotificationCenter.getGlobalInstance().addWeakObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getInstance(currentAccount).addWeakObserver(this, NotificationCenter.configLoaded);
        ConnectionsManager.getInstance(currentAccount).updateDcSettings();
        LocaleController.getInstance().loadRemoteLanguages(currentAccount);
        checkContinueText();

        updateColors();
        return fragmentView;
    }

    private TextView createStartButton(Context context) {
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{BUTTON_TOP, BUTTON_BOTTOM});
        TextView button = new TextView(context) {
            private final CellFlickerDrawable flicker = new CellFlickerDrawable();

            {
                flicker.drawFrame = false;
                flicker.repeatProgress = 2f;
            }

            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                background.setCornerRadius(h / 2f);
                flicker.setParentWidth(w);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                flicker.draw(canvas, AndroidUtilities.rectTmp, getMeasuredHeight() / 2f, null);
                invalidate();
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                if (width > dp(360)) {
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(dp(360), MeasureSpec.EXACTLY);
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        button.setBackground(background);
        button.setText(LocaleController.getString(R.string.StartMessaging));
        button.setTextColor(Color.WHITE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        button.setTypeface(AndroidUtilities.bold());
        button.setGravity(Gravity.CENTER);
        button.setElevation(dp(6));
        if (Build.VERSION.SDK_INT >= 28) {
            button.setOutlineSpotShadowColor(NexaTheme.ACCENT);
            button.setOutlineAmbientShadowColor(NexaTheme.ACCENT);
        }
        ScaleStateListAnimator.apply(button, .02f, 1.2f);
        button.setOnClickListener(v -> {
            if (startPressed) {
                return;
            }
            startPressed = true;
            presentFragment(new LoginActivity(), true);
            destroyed = true;
        });
        return button;
    }

    private View createThemeToggle(Context context) {
        FrameLayout frame = new FrameLayout(context);
        themeIconView = new RLottieImageView(context);
        frame.addView(themeIconView, LayoutHelper.createFrame(28, 28, Gravity.CENTER));

        boolean dark = Theme.getCurrentTheme().isDark();
        darkThemeDrawable = new RLottieDrawable(R.raw.sun, dp(28), dp(28), true, null);
        darkThemeDrawable.setPlayInDirectionOfCustomEndFrame(true);
        darkThemeDrawable.beginApplyLayerColors();
        darkThemeDrawable.commitApplyLayerColors();
        darkThemeDrawable.setCustomEndFrame(dark ? darkThemeDrawable.getFramesCount() - 1 : 0);
        darkThemeDrawable.setCurrentFrame(dark ? darkThemeDrawable.getFramesCount() - 1 : 0, false);
        themeIconView.setAnimation(darkThemeDrawable);
        themeIconView.setContentDescription(LocaleController.getString(dark ? R.string.AccDescrSwitchToDayTheme : R.string.AccDescrSwitchToNightTheme));

        frame.setOnClickListener(v -> {
            if (DialogsActivity.switchingTheme) {
                return;
            }
            DialogsActivity.switchingTheme = true;

            boolean toDark = !Theme.isCurrentThemeDark();
            Theme.ThemeInfo themeInfo = Theme.getTheme(toDark ? "Night" : "Blue");

            Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE;
            Theme.saveAutoNightThemeConfig();
            Theme.cancelAutoNightThemeCallbacks();

            darkThemeDrawable.setCustomEndFrame(toDark ? darkThemeDrawable.getFramesCount() - 1 : 0);
            themeIconView.playAnimation();

            int[] pos = new int[2];
            themeIconView.getLocationInWindow(pos);
            pos[0] += themeIconView.getMeasuredWidth() / 2;
            pos[1] += themeIconView.getMeasuredHeight() / 2;
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, themeInfo, false, pos, -1, toDark, themeIconView);
            themeIconView.setContentDescription(LocaleController.getString(toDark ? R.string.AccDescrSwitchToDayTheme : R.string.AccDescrSwitchToNightTheme));
        });
        return frame;
    }

    private void switchLanguage(Context context) {
        if (startPressed || localeInfo == null) {
            return;
        }
        startPressed = true;

        AlertDialog loaderDialog = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
        loaderDialog.setCanCancel(false);
        loaderDialog.showDelayed(1000);

        NotificationCenter.getGlobalInstance().addObserver(new NotificationCenter.NotificationCenterDelegate() {
            @Override
            public void didReceivedNotification(int id, int account, Object... args) {
                if (id == NotificationCenter.reloadInterface) {
                    loaderDialog.dismiss();
                    NotificationCenter.getGlobalInstance().removeObserver(this, id);
                    AndroidUtilities.runOnUIThread(() -> {
                        presentFragment(new LoginActivity(), true);
                        destroyed = true;
                    }, 100);
                }
            }
        }, NotificationCenter.reloadInterface);
        LocaleController.getInstance().applyLanguage(localeInfo, true, false, currentAccount);
    }

    /** Same logic as Telegram's intro: offer the system language (or English) as a one-tap switch. */
    private void checkContinueText() {
        LocaleController.LocaleInfo englishInfo = null;
        LocaleController.LocaleInfo systemInfo = null;
        LocaleController.LocaleInfo currentLocaleInfo = LocaleController.getInstance().getCurrentLocaleInfo();
        String systemLang = MessagesController.getInstance(currentAccount).suggestedLangCode;
        String systemDefault = LocaleController.getInstance().getSystemDefaultLocale().getLanguage();
        if (systemLang == null || systemLang.equals("en") && systemDefault != null && !systemDefault.equals("en")) {
            systemLang = systemDefault != null ? systemDefault : "en";
        }

        String arg = systemLang.contains("-") ? systemLang.split("-")[0] : systemLang;
        String alias = LocaleController.getLocaleAlias(arg);
        for (int a = 0; a < LocaleController.getInstance().languages.size(); a++) {
            LocaleController.LocaleInfo info = LocaleController.getInstance().languages.get(a);
            if (info.shortName.equals("en")) {
                englishInfo = info;
            }
            if (info.shortName.replace("_", "-").equals(systemLang) || info.shortName.equals(arg) || info.shortName.equals(alias)) {
                systemInfo = info;
            }
            if (englishInfo != null && systemInfo != null) {
                break;
            }
        }
        if (englishInfo == null || systemInfo == null || englishInfo == systemInfo) {
            return;
        }
        TLRPC.TL_langpack_getStrings req = new TLRPC.TL_langpack_getStrings();
        if (systemInfo != currentLocaleInfo) {
            req.lang_code = systemInfo.getLangCode();
            localeInfo = systemInfo;
        } else {
            req.lang_code = englishInfo.getLangCode();
            localeInfo = englishInfo;
        }
        req.keys.add("ContinueOnThisLanguage");
        String finalSystemLang = systemLang;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (!(response instanceof Vector)) {
                return;
            }
            Vector vector = (Vector) response;
            if (vector.objects.isEmpty() || !(vector.objects.get(0) instanceof TLRPC.TL_langPackString)) {
                return;
            }
            TLRPC.LangPackString string = (TLRPC.LangPackString) vector.objects.get(0);
            AndroidUtilities.runOnUIThread(() -> {
                if (!destroyed) {
                    switchLanguageTextView.setText(string.value);
                    SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                    preferences.edit().putString("language_showed2", finalSystemLang.toLowerCase()).apply();
                }
            });
        }, ConnectionsManager.RequestFlagWithoutLogin);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.suggestedLangpack || id == NotificationCenter.configLoaded) {
            checkContinueText();
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onResume() {
        super.onResume();
        AndroidUtilities.lockOrientation(getParentActivity(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onPause() {
        super.onPause();
        AndroidUtilities.unlockOrientation(getParentActivity());
    }

    @Override
    public boolean hasForceLightStatusBar() {
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        destroyed = true;
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.suggestedLangpack);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.configLoaded);
        MessagesController.getGlobalMainSettings().edit().putLong("intro_crashed_time", 0).apply();
    }

    @Override
    public AnimatorSet onCustomTransitionAnimation(boolean isOpen, Runnable callback) {
        if (isOnLogout) {
            AnimatorSet set = new AnimatorSet().setDuration(50);
            set.playTogether(ValueAnimator.ofFloat());
            return set;
        }
        return null;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return SimpleThemeDescription.createThemeDescriptions(this::updateColors,
                Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhiteBlackText);
    }

    private static boolean isDark() {
        return Theme.getCurrentTheme().isDark();
    }

    private static int bgTop() {
        return isDark() ? 0xFF15121F : 0xFFF7F4FF;
    }

    private static int bgBottom() {
        return isDark() ? 0xFF1D1731 : 0xFFEBE4FF;
    }

    @Override
    public int getNavigationBarColor() {
        return bgBottom();
    }

    private static int titleColor() {
        return isDark() ? 0xFFFFFFFF : 0xFF17132B;
    }

    private static int textColor() {
        return isDark() ? 0xFFADA7C6 : 0xFF6B6684;
    }

    private void updateColors() {
        if (rootView == null) {
            return;
        }
        rootView.updateColors();
        dotsView.invalidate();
        setNavigationBarColor(getNavigationBarColor());
        switchLanguageTextView.setTextColor(isDark() ? 0xFFB3A3FF : NexaTheme.ACCENT);
        darkThemeDrawable.setColorFilter(new PorterDuffColorFilter(isDark() ? 0xFFB3A3FF : NexaTheme.ACCENT, PorterDuff.Mode.SRC_IN));
        for (int i = 0; i < viewPager.getChildCount(); i++) {
            View page = viewPager.getChildAt(i);
            if (page instanceof PageView) {
                ((PageView) page).updateColors();
            }
        }
    }

    /** One page: floating 3D illustration, title, text. */
    private static class PageView extends LinearLayout {
        final TextView titleView;
        final TextView textView;
        ImageView imageView;

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            // image: as big as fits next to the texts, but not huge
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
            int size = Math.min(Math.min(dp(300), width - dp(72)), height - dp(170));
            size = Math.max(size, dp(120));
            LayoutParams lp = (LayoutParams) imageView.getLayoutParams();
            if (lp.width != size) {
                lp.width = lp.height = size;
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        PageView(Context context, int index) {
            super(context);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setPadding(0, (AndroidUtilities.isTablet() ? 0 : AndroidUtilities.statusBarHeight) + dp(56), 0, dp(196));

            float phase = index * 1.1f;
            ImageView imageView = new ImageView(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    float t = (SystemClock.elapsedRealtime() % 4000L) / 4000f;
                    float dy = (float) Math.sin(t * Math.PI * 2 + phase) * dp(5);
                    canvas.save();
                    canvas.translate(0, dy);
                    super.onDraw(canvas);
                    canvas.restore();
                    postInvalidateOnAnimation();
                }
            };
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageResource(IMAGES[index]);
            this.imageView = imageView;
            addView(imageView, new LayoutParams(dp(260), dp(260)));

            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
            titleView.setTypeface(AndroidUtilities.bold());
            titleView.setGravity(Gravity.CENTER);
            titleView.setText(LocaleController.getString(TITLES[index]));
            LayoutParams titleParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            titleParams.topMargin = dp(28);
            titleParams.leftMargin = titleParams.rightMargin = dp(24);
            addView(titleView, titleParams);

            textView = new TextView(context);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setLineSpacing(dp(3), 1f);
            textView.setGravity(Gravity.CENTER);
            textView.setText(LocaleController.getString(TEXTS[index]));
            LayoutParams textParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            textParams.topMargin = dp(10);
            textParams.leftMargin = textParams.rightMargin = dp(36);
            addView(textView, textParams);

            updateColors();
        }

        void updateColors() {
            titleView.setTextColor(titleColor());
            textView.setTextColor(textColor());
        }
    }

    private class PagesAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return PAGES;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            PageView page = new PageView(container.getContext(), position);
            container.addView(page, 0);
            return page;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void unregisterDataSetObserver(@NonNull DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }

    /** Page indicator: violet pill for the current page, soft dots for the rest. */
    private static class DotsView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float progress;

        DotsView(Context context) {
            super(context);
        }

        void setProgress(float progress) {
            this.progress = progress;
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = dp(8) * PAGES + dp(8) * (PAGES - 1) + dp(16);
            setMeasuredDimension(width, dp(8));
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            int inactive = isDark() ? 0x40B3A3FF : 0x337B61FF;
            int active = isDark() ? 0xFFB3A3FF : NexaTheme.ACCENT;
            float x = 0;
            float h = dp(8);
            for (int i = 0; i < PAGES; i++) {
                // 0..1: how "current" this dot is
                float k = Math.max(0f, 1f - Math.abs(progress - i));
                float w = h + dp(16) * k;
                paint.setColor(androidx.core.graphics.ColorUtils.blendARGB(inactive, active, k));
                rect.set(x, 0, x + w, h);
                canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);
                x += w + dp(8);
            }
        }
    }

    /** Root view: soft lilac gradient with waves that drift a little while paging. */
    private static class BackgroundLayout extends FrameLayout {
        private final Paint bgPaint = new Paint();
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path wave = new Path();
        private float progress;
        private Boolean shadersDark;

        BackgroundLayout(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        void setProgress(float progress) {
            this.progress = progress;
            invalidate();
        }

        void updateColors() {
            updateShaders(getWidth(), getHeight());
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            updateShaders(w, h);
        }

        private void updateShaders(int w, int h) {
            if (w <= 0 || h <= 0) {
                return;
            }
            boolean dark = isDark();
            shadersDark = dark;
            bgPaint.setShader(new LinearGradient(0, 0, 0, h,
                    bgTop(), bgBottom(), Shader.TileMode.CLAMP));
            glowPaint.setShader(new RadialGradient(w / 2f, h * 0.30f, w * 0.62f,
                    dark ? 0x407B61FF : 0x5CC9BCFF, 0x007B61FF, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            boolean dark = isDark();
            if (shadersDark == null || shadersDark != dark) {
                // theme flipped since the last full draw: rebuild and repaint everything,
                // otherwise only the dirty parts (button, toggle) get the new colors
                updateShaders(w, h);
                postInvalidate();
            }
            canvas.drawRect(0, 0, w, h, bgPaint);
            canvas.drawRect(0, 0, w, h, glowPaint);

            float shift = -progress * dp(14);

            // top waves
            drawWave(canvas, w, 0, h * 0.13f, h * 0.05f, shift, true, dark ? 0x1A8F78FF : 0x24A08CFF);
            drawWave(canvas, w, 0, h * 0.09f, h * 0.04f, -shift * 0.6f, true, dark ? 0x128F78FF : 0x1FFFFFFF);
            // bottom waves
            drawWave(canvas, w, h, h * 0.86f, h * 0.05f, shift * 0.8f, false, dark ? 0x1A8F78FF : 0x1FA08CFF);
            drawWave(canvas, w, h, h * 0.91f, h * 0.04f, -shift, false, dark ? 0x128F78FF : 0x66FFFFFF);
        }

        private void drawWave(Canvas canvas, int w, float edge, float y, float amp, float shift, boolean top, int color) {
            wave.reset();
            float x0 = -dp(40) + shift;
            float x1 = w + dp(40) + shift;
            float span = x1 - x0;
            wave.moveTo(x0, edge);
            wave.lineTo(x0, y);
            wave.cubicTo(x0 + span * 0.30f, y + amp * (top ? 1.6f : -1.6f),
                    x0 + span * 0.60f, y - amp * (top ? 1.2f : -1.2f),
                    x1, y + amp * (top ? 0.4f : -0.4f));
            wave.lineTo(x1, edge);
            wave.close();
            wavePaint.setColor(color);
            canvas.drawPath(wave, wavePaint);
        }
    }
}
