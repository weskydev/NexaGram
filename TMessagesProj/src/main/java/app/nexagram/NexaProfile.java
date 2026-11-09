package app.nexagram;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Text;

/**
 * Own profile and Settings header: the username and phone line under the status,
 * how much of the phone number is shown, and the music block.
 */
public final class NexaProfile {

    public static final int PHONE_FULL = 0;
    public static final int PHONE_CODE = 1;
    public static final int PHONE_HIDDEN = 2;
    private static final String PHONE_MODE = "phone_mode";

    /** Height the username/phone line adds to the profile header. */
    private static final int LINE_DP = 26;
    /** The music block's height, and how much taller it is than Telegram's pill. */
    public static final int MUSIC_BLOCK_DP = 46;
    private static final int MUSIC_PILL_DP = 17;

    private NexaProfile() {
    }

    // ---- phone number ----

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("nexagram", Context.MODE_PRIVATE);
    }

    public static int phoneMode() {
        if (ApplicationLoader.applicationContext == null) {
            return PHONE_FULL;
        }
        return prefs().getInt(PHONE_MODE, PHONE_FULL);
    }

    public static void setPhoneMode(int mode) {
        prefs().edit().putInt(PHONE_MODE, mode).apply();
        NexaDesign.bump();
    }

    /** Own phone as the user wants it shown; null when it's hidden. */
    public static String ownPhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return null;
        }
        String full = PhoneFormat.getInstance().format("+" + phone);
        switch (phoneMode()) {
            case PHONE_HIDDEN:
                return null;
            case PHONE_CODE:
                return countryCode(full) + " •••";
            default:
                return full;
        }
    }

    /** "+380 (93) 123 45 67" → "+380". */
    private static String countryCode(String formatted) {
        int end = 1;
        while (end < formatted.length() && Character.isDigit(formatted.charAt(end))) {
            end++;
        }
        String code = formatted.substring(0, end);
        // without spaces in the format we can't tell where the code ends: keep 3 digits at most
        return code.length() > 4 && formatted.indexOf(' ') < 0 ? code.substring(0, 4) : code;
    }

    /** Settings header: "@username • phone", each part only if shown. */
    public static CharSequence settingsSubtitle(TLRPC.User user, CharSequence telegram) {
        if (!NexaDesign.get(NexaDesign.SETTINGS_DESIGN) && phoneMode() == PHONE_FULL) {
            return telegram;
        }
        String username = UserObject.getPublicUsername(user);
        String phone = user != null ? ownPhone(user.phone) : null;
        StringBuilder sb = new StringBuilder();
        if (username != null) {
            sb.append('@').append(username);
        }
        if (phone != null) {
            if (sb.length() > 0) {
                sb.append("  •  ");
            }
            sb.append(phone);
        }
        return sb;
    }

    // ---- username / phone line in the own profile ----

    public static boolean lineEnabled(boolean myProfile) {
        return myProfile && NexaDesign.profileLine();
    }

    public static int lineHeight(boolean myProfile) {
        return lineEnabled(myProfile) ? dp(LINE_DP) : 0;
    }

    public static Line attachLine(ViewGroup parent, int account) {
        Line line = new Line(parent.getContext(), account);
        parent.addView(line, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));
        return line;
    }

    /** Two chips: "@username" on the left, the phone on the right. */
    public static class Line extends View {

        private final int account;
        private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF chip = new RectF();
        private Text username, phone;
        private String lastUsername, lastPhone;
        private int color;

        Line(Context context, int account) {
            super(context);
            this.account = account;
            setPivotY(0);
        }

        private void refresh() {
            TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
            String u = UserObject.getPublicUsername(user);
            String p = user != null ? ownPhone(user.phone) : null;
            if (TextUtils.equals(u, lastUsername) && TextUtils.equals(p, lastPhone) && (username != null || phone != null || (u == null && p == null))) {
                return;
            }
            lastUsername = u;
            lastPhone = p;
            username = u != null ? new Text("@" + u, 13, AndroidUtilities.bold()) : null;
            phone = p != null ? new Text(p, 13) : null;
            invalidate();
        }

        /** Follows the status text under the name: same place, color and fade. */
        public void follow(View status, int statusColor, float headerDiff, float expand) {
            refresh();
            View parent = (View) getParent();
            if (parent == null) {
                return;
            }
            color = statusColor;
            setTranslationY(status.getY() + status.getHeight() * status.getScaleY() + dp(4) - getTop());
            float shown = Utilities.clamp01((headerDiff - 0.7f) / 0.3f) * (1f - Utilities.clamp01(expand * 4f));
            setAlpha(status.getAlpha() * shown);
            setVisibility(shown <= 0 ? INVISIBLE : VISIBLE);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (username == null && phone == null) {
                return;
            }
            final float pad = dp(10), gap = dp(6), h = dp(24);
            float total = 0;
            float maxChip = (getWidth() - dp(32) - gap) / (username != null && phone != null ? 2f : 1f) - pad * 2;
            if (username != null) {
                username.ellipsize(maxChip);
                total += username.getWidth() + pad * 2;
            }
            if (phone != null) {
                phone.ellipsize(maxChip);
                total += phone.getWidth() + pad * 2;
            }
            if (username != null && phone != null) {
                total += gap;
            }
            float x = (getWidth() - total) / 2f;
            final float cy = h / 2f;
            chipPaint.setColor(Theme.multAlpha(color, 0.12f));
            if (username != null) {
                chip.set(x, 0, x + username.getWidth() + pad * 2, h);
                canvas.drawRoundRect(chip, h / 2f, h / 2f, chipPaint);
                username.draw(canvas, x + pad, cy, color, 1f);
                x = chip.right + gap;
            }
            if (phone != null) {
                chip.set(x, 0, x + phone.getWidth() + pad * 2, h);
                canvas.drawRoundRect(chip, h / 2f, h / 2f, chipPaint);
                phone.draw(canvas, x + pad, cy, color, 0.9f);
            }
        }
    }

    // ---- music block ----

    public static boolean musicBlock() {
        return NexaDesign.musicBlock();
    }

    /** Extra header height for the block instead of Telegram's small pill. */
    public static int musicExtra() {
        return musicBlock() ? dp(MUSIC_BLOCK_DP - MUSIC_PILL_DP) : 0;
    }

    /** Music block: accent tile with a note, song title and artist on two lines, arrow. */
    public static class MusicBlock {
        private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path arrow = new Path();
        private final RectF tile = new RectF();
        private Text title, artist;
        private CharSequence lastTitle, lastArtist;
        private LinearGradient gradient;
        private float gradientTop = Float.NaN;

        public MusicBlock() {
            arrowPaint.setStyle(Paint.Style.STROKE);
            arrowPaint.setStrokeCap(Paint.Cap.ROUND);
            arrowPaint.setStrokeJoin(Paint.Join.ROUND);
            arrowPaint.setStrokeWidth(dp(1.6f));
            arrow.moveTo(0, -dp(5));
            arrow.lineTo(dp(5), 0);
            arrow.lineTo(0, dp(5));
        }

        public void setText(CharSequence title, CharSequence artist) {
            if (TextUtils.equals(title, lastTitle) && TextUtils.equals(artist, lastArtist)) {
                return;
            }
            lastTitle = title;
            lastArtist = artist;
            this.title = new Text(title == null ? "" : title, 14, AndroidUtilities.bold());
            this.artist = new Text(artist == null ? "" : artist, 12.5f);
        }

        public float radius() {
            return dp(16);
        }

        /** Draws the content inside {@code rect}; the card itself is drawn by the caller. */
        public void draw(Canvas canvas, RectF rect, Drawable note, int textColor, float alpha) {
            if (title == null) {
                return;
            }
            final float side = Math.min(dp(32), rect.height() - dp(12));
            if (side <= 0) {
                return;
            }
            tile.set(rect.left + dp(8), rect.centerY() - side / 2f, rect.left + dp(8) + side, rect.centerY() + side / 2f);
            if (gradient == null || gradientTop != tile.top) {
                gradientTop = tile.top;
                gradient = new LinearGradient(tile.left, tile.top, tile.right, tile.bottom, 0xFF9B85FF, 0xFF6A4FF0, Shader.TileMode.CLAMP);
                tilePaint.setShader(gradient);
            }
            tilePaint.setAlpha((int) (0xFF * alpha));
            canvas.drawRoundRect(tile, dp(10), dp(10), tilePaint);
            int iconSize = (int) (side * 0.55f);
            note.setBounds((int) (tile.centerX() - iconSize / 2f), (int) (tile.centerY() - iconSize / 2f), (int) (tile.centerX() + iconSize / 2f), (int) (tile.centerY() + iconSize / 2f));
            note.setAlpha((int) (0xFF * alpha));
            note.draw(canvas);
            note.setAlpha(0xFF);

            final float textLeft = tile.right + dp(10);
            final float textWidth = Math.max(0, rect.right - dp(24) - textLeft);
            title.ellipsize(textWidth);
            artist.ellipsize(textWidth);
            boolean twoLines = rect.height() > dp(34) && artist.getWidth() > 0;
            if (twoLines) {
                title.draw(canvas, textLeft, rect.centerY() - dp(8), textColor, alpha);
                artist.draw(canvas, textLeft, rect.centerY() + dp(9), textColor, 0.6f * alpha);
            } else {
                title.draw(canvas, textLeft, rect.centerY(), textColor, alpha);
            }

            arrowPaint.setColor(Theme.multAlpha(textColor, 0.45f * alpha));
            canvas.save();
            canvas.translate(rect.right - dp(16), rect.centerY());
            canvas.drawPath(arrow, arrowPaint);
            canvas.restore();
        }
    }

    public static int musicNoteIcon() {
        return R.drawable.files_music;
    }
}
