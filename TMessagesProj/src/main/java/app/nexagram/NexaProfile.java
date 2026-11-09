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
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionIntroActivity;
import org.telegram.ui.ChangeUsernameActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.Text;
import org.telegram.ui.QrShare.QrShareActivity;

import static org.telegram.messenger.LocaleController.getString;

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
    public static final int MUSIC_BLOCK_DP = 60;
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

    /** The chips replace the "Phone" and "Username" rows of the own profile. */
    public static boolean hideRows(boolean myProfile) {
        return lineEnabled(myProfile);
    }

    public static Line attachLine(BaseFragment fragment, ViewGroup parent, int account) {
        Line line = new Line(parent.getContext(), account);
        line.fragment = fragment;
        parent.addView(line, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));
        return line;
    }

    /** Two chips: "@username" on the left, the phone on the right. */
    public static class Line extends View {

        private final int account;
        private BaseFragment fragment;
        private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF usernameChip = new RectF(), phoneChip = new RectF();
        private int pressed; // 0 none, 1 username, 2 phone
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

        private int chipAt(float x, float y) {
            if (getVisibility() != VISIBLE || getAlpha() < 0.5f) {
                return 0;
            }
            if (username != null && hit(usernameChip, x, y)) {
                return 1;
            }
            if (phone != null && hit(phoneChip, x, y)) {
                return 2;
            }
            return 0;
        }

        private static boolean hit(RectF r, float x, float y) {
            return x >= r.left - dp(4) && x <= r.right + dp(4) && y >= r.top - dp(6) && y <= r.bottom + dp(6);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pressed = chipAt(e.getX(), e.getY());
                    invalidate();
                    return pressed != 0;
                case MotionEvent.ACTION_MOVE:
                    if (pressed != 0 && chipAt(e.getX(), e.getY()) != pressed) {
                        pressed = 0;
                        invalidate();
                    }
                    return pressed != 0;
                case MotionEvent.ACTION_UP:
                    if (pressed != 0) {
                        boolean isPhone = pressed == 2;
                        pressed = 0;
                        invalidate();
                        if (fragment != null) {
                            showMenu(fragment, this, isPhone, account);
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_CANCEL:
                    pressed = 0;
                    invalidate();
                    return false;
            }
            return false;
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
            if (username != null) {
                usernameChip.set(x, 0, x + username.getWidth() + pad * 2, h);
                chipPaint.setColor(Theme.multAlpha(color, pressed == 1 ? 0.24f : 0.12f));
                canvas.drawRoundRect(usernameChip, h / 2f, h / 2f, chipPaint);
                username.draw(canvas, x + pad, cy, color, 1f);
                x = usernameChip.right + gap;
            }
            if (phone != null) {
                phoneChip.set(x, 0, x + phone.getWidth() + pad * 2, h);
                chipPaint.setColor(Theme.multAlpha(color, pressed == 2 ? 0.24f : 0.12f));
                canvas.drawRoundRect(phoneChip, h / 2f, h / 2f, chipPaint);
                phone.draw(canvas, x + pad, cy, color, 0.9f);
            }
        }
    }

    /** What the "Username" and "Phone" rows did on tap, now on the chips. */
    public static void showMenu(BaseFragment fragment, View anchor, boolean isPhone, int account) {
        TLRPC.User user = UserConfig.getInstance(account).getCurrentUser();
        if (user == null || fragment.getParentActivity() == null) {
            return;
        }
        ItemOptions o = ItemOptions.makeOptions(fragment, anchor);
        o.setLongPressSelectionEnabled(false);
        if (isPhone) {
            if (TextUtils.isEmpty(user.phone)) {
                return;
            }
            final String number = "+" + user.phone;
            o.add(R.drawable.msg_copy, getString(R.string.Copy), () -> {
                if (AndroidUtilities.addToClipboard(number)) {
                    BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.PhoneCopied)).show();
                }
            });
            o.add(R.drawable.menu_storage_path, getString(R.string.ProfilePhoneEdit), () ->
                    fragment.presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANGE_PHONE_NUMBER)));
        } else {
            final String name = UserObject.getPublicUsername(user);
            if (name == null) {
                return;
            }
            final String link = "https://" + MessagesController.getInstance(account).linkPrefix + "/" + name;
            o.add(R.drawable.msg_copy, getString(R.string.ProfileCopyUsername), () -> {
                if (AndroidUtilities.addToClipboard("@" + name)) {
                    BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.UsernameCopied)).show();
                }
            });
            o.add(R.drawable.msg_link2, getString(R.string.CopyLink), () -> {
                if (AndroidUtilities.addToClipboard(link)) {
                    BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.LinkCopied)).show();
                }
            });
            o.add(R.drawable.msg_qrcode, getString(R.string.QrCode), () -> {
                Bundle args = new Bundle();
                args.putLong("user_id", user.id);
                fragment.presentFragment(new QrShareActivity(args));
            });
            o.add(R.drawable.msg_edit, getString(R.string.ProfileUsernameEdit), () ->
                    fragment.presentFragment(new ChangeUsernameActivity()));
        }
        o.show();
    }

    // ---- music block ----

    public static boolean musicBlock() {
        return NexaDesign.musicBlock();
    }

    /** Extra header height for the block instead of Telegram's small pill. */
    public static int musicExtra() {
        return musicBlock() ? dp(MUSIC_BLOCK_DP - MUSIC_PILL_DP) : 0;
    }

    /**
     * Music card: track cover, "Profile track" label, song title and artist,
     * a play / pause button and the playback progress at the bottom.
     */
    public static class MusicBlock {
        private final View parent;
        private final ImageReceiver cover;
        private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path playPath = new Path();
        private final RectF tile = new RectF();
        private final RectF button = new RectF();
        private final RectF bar = new RectF();
        private Text label, title, artist;
        private CharSequence lastTitle, lastArtist;
        private TLRPC.Document document;
        private LinearGradient gradient;
        private float gradientTop = Float.NaN;

        public MusicBlock(View parent) {
            this.parent = parent;
            cover = new ImageReceiver(parent);
            cover.setRoundRadius(dp(10));
            label = new Text(getString(R.string.NexaProfileTrack), 11);
        }

        public void attach() {
            cover.onAttachedToWindow();
        }

        public void detach() {
            cover.onDetachedFromWindow();
        }

        public void setDocument(TLRPC.Document document) {
            if (document == this.document || document != null && this.document != null && document.id == this.document.id) {
                return;
            }
            this.document = document;
            if (document == null) {
                cover.clearImage();
                return;
            }
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 160);
            if (!(thumb instanceof TLRPC.TL_photoSize) && !(thumb instanceof TLRPC.TL_photoSizeProgressive)) {
                thumb = null;
            }
            if (thumb != null) {
                cover.setImage(ImageLocation.getForDocument(thumb, document), "90_90", null, null, document, 0);
            } else {
                String artwork = MessageObject.getArtworkUrl(document, true);
                if (artwork != null) {
                    cover.setImage(ImageLocation.getForPath(artwork), "90_90", null, null, document, 0);
                } else {
                    cover.clearImage();
                }
            }
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

        /** The track being played (or paused) when it's this card's one, else null. */
        private MessageObject current() {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing == null || document == null) {
                return null;
            }
            TLRPC.Document doc = playing.getDocument();
            return doc != null && doc.id == document.id ? playing : null;
        }

        public boolean isPlaying() {
            return current() != null && !MediaController.getInstance().isMessagePaused();
        }

        /** Play / pause; returns false when playback has to be started from scratch. */
        public boolean toggle() {
            MessageObject playing = current();
            if (playing == null) {
                return false;
            }
            if (MediaController.getInstance().isMessagePaused()) {
                MediaController.getInstance().playMessage(playing);
            } else {
                MediaController.getInstance().pauseMessage(playing);
            }
            parent.invalidate();
            return true;
        }

        public boolean buttonHit(float x, float y) {
            return button.width() > 0 && x >= button.left - dp(8) && x <= button.right + dp(8) && y >= button.top - dp(8) && y <= button.bottom + dp(8);
        }

        public float radius() {
            return dp(16);
        }

        /** Draws the content inside {@code rect}; the card itself is drawn by the caller. */
        public void draw(Canvas canvas, RectF rect, Drawable note, int textColor, float alpha) {
            if (title == null) {
                return;
            }
            final float side = Math.min(dp(44), rect.height() - dp(14));
            if (side <= 0) {
                return;
            }
            tile.set(rect.left + dp(8), rect.centerY() - side / 2f, rect.left + dp(8) + side, rect.centerY() + side / 2f);
            if (cover.hasImageSet() && cover.getBitmap() != null) {
                cover.setImageCoords(tile.left, tile.top, tile.width(), tile.height());
                cover.setAlpha(alpha);
                cover.draw(canvas);
            } else {
                if (gradient == null || gradientTop != tile.top) {
                    gradientTop = tile.top;
                    gradient = new LinearGradient(tile.left, tile.top, tile.right, tile.bottom, 0xFF9B85FF, 0xFF6A4FF0, Shader.TileMode.CLAMP);
                    tilePaint.setShader(gradient);
                }
                tilePaint.setAlpha((int) (0xFF * alpha));
                canvas.drawRoundRect(tile, dp(10), dp(10), tilePaint);
                int iconSize = (int) (side * 0.5f);
                note.setBounds((int) (tile.centerX() - iconSize / 2f), (int) (tile.centerY() - iconSize / 2f), (int) (tile.centerX() + iconSize / 2f), (int) (tile.centerY() + iconSize / 2f));
                note.setAlpha((int) (0xFF * alpha));
                note.draw(canvas);
                note.setAlpha(0xFF);
            }

            // play / pause
            final float bs = Math.min(dp(34), side);
            button.set(rect.right - dp(10) - bs, rect.centerY() - bs / 2f, rect.right - dp(10), rect.centerY() + bs / 2f);
            buttonPaint.setColor(Theme.multAlpha(textColor, 0.18f * alpha));
            canvas.drawCircle(button.centerX(), button.centerY(), bs / 2f, buttonPaint);
            iconPaint.setColor(Theme.multAlpha(textColor, alpha));
            final MessageObject playing = current();
            final boolean isPlaying = playing != null && !MediaController.getInstance().isMessagePaused();
            final float cx = button.centerX(), cy = button.centerY(), s = bs * 0.2f;
            if (isPlaying) {
                final float w = s * 0.55f, gap = s * 0.45f;
                canvas.drawRoundRect(cx - gap - w, cy - s, cx - gap, cy + s, w / 2f, w / 2f, iconPaint);
                canvas.drawRoundRect(cx + gap, cy - s, cx + gap + w, cy + s, w / 2f, w / 2f, iconPaint);
            } else {
                playPath.rewind();
                playPath.moveTo(cx - s * 0.7f, cy - s);
                playPath.lineTo(cx + s * 1.05f, cy);
                playPath.lineTo(cx - s * 0.7f, cy + s);
                playPath.close();
                canvas.drawPath(playPath, iconPaint);
            }

            // texts
            final float textLeft = tile.right + dp(12);
            final float textWidth = Math.max(0, button.left - dp(10) - textLeft);
            title.ellipsize(textWidth);
            artist.ellipsize(textWidth);
            label.ellipsize(textWidth);
            final boolean hasArtist = artist.getWidth() > 0;
            if (rect.height() > dp(50)) {
                final float top = rect.centerY() - (hasArtist ? dp(15) : dp(8));
                label.draw(canvas, textLeft, top, textColor, 0.6f * alpha);
                title.draw(canvas, textLeft, top + dp(hasArtist ? 15.5f : 16), textColor, alpha);
                if (hasArtist) {
                    artist.draw(canvas, textLeft, top + dp(30.5f), textColor, 0.7f * alpha);
                }
            } else if (hasArtist && rect.height() > dp(34)) {
                title.draw(canvas, textLeft, rect.centerY() - dp(8), textColor, alpha);
                artist.draw(canvas, textLeft, rect.centerY() + dp(9), textColor, 0.7f * alpha);
            } else {
                title.draw(canvas, textLeft, rect.centerY(), textColor, alpha);
            }

            // progress of this track as a ring around the button, when it's the one playing
            if (playing != null) {
                final float r = bs / 2f - dp(1.5f);
                bar.set(cx - r, cy - r, cx + r, cy + r);
                progressPaint.setStyle(Paint.Style.STROKE);
                progressPaint.setStrokeCap(Paint.Cap.ROUND);
                progressPaint.setStrokeWidth(dp(2.5f));
                progressPaint.setColor(Theme.multAlpha(textColor, 0.9f * alpha));
                canvas.drawArc(bar, -90, 360 * Utilities.clamp01(playing.audioProgress), false, progressPaint);
                parent.postInvalidateDelayed(isPlaying ? 50 : 400);
            }
        }
    }

    public static int musicNoteIcon() {
        return R.drawable.files_music;
    }
}
