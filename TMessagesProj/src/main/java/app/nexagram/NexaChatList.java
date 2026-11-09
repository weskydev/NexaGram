package app.nexagram;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Components.RecyclerListView;

/**
 * NexaGram chat list look: every chat sits in its own rounded card on a soft gray
 * background, avatars are rounded squares, pinned chats get a light accent tint.
 */
public final class NexaChatList {

    private static final int SIDE_DP = 10;
    private static final int GAP_DP = 3;
    private static final int CARD_RADIUS_DP = 18;
    /** Rounded-square avatar; Telegram uses 28dp (a circle) for 56dp avatars. */
    private static final int AVATAR_RADIUS_DP = 17;

    private NexaChatList() {
    }

    public static int accent() {
        return NexaTheme.isEnabled() ? NexaTheme.accent() : Theme.getColor(Theme.key_chats_actionBackground);
    }

    private static boolean isDark() {
        return Theme.isCurrentThemeDark();
    }

    /** Radius for a regular (round) chat avatar in the chat list. */
    public static int avatarRadius(int telegramRadius) {
        if (!NexaDesign.get(NexaDesign.SQUARE_AVATARS)) {
            return telegramRadius;
        }
        // same shape at any size: 28dp circle radius -> 17dp corner
        return telegramRadius == dp(28) ? dp(AVATAR_RADIUS_DP) : Math.round(telegramRadius * AVATAR_RADIUS_DP / 28f);
    }

    /** True for our rounded-square avatars, so the story ring follows the same shape. */
    public static boolean isSquareAvatar(org.telegram.messenger.ImageReceiver avatar) {
        if (avatar == null) {
            return false;
        }
        int[] radius = avatar.getRoundRadius();
        return NexaDesign.get(NexaDesign.SQUARE_AVATARS) && radius != null && radius[0] > 1
                && radius[0] < avatar.getImageWidth() / 2f - dp(2);
    }

    /** Turns the main chat list into cards. Call once for the main dialogs list. */
    public static void install(RecyclerListView listView) {
        listView.setBackground(new ListBackground());
        listView.setSelectorRadius(NexaDesign.get(NexaDesign.CHAT_CARDS) ? dp(CARD_RADIUS_DP) : 0);
        listView.addItemDecoration(new Cards(listView));
    }

    private static int cardColor(boolean pinned) {
        int white = Theme.getColor(Theme.key_windowBackgroundWhite);
        return pinned ? ColorUtils.blendARGB(white, accent(), isDark() ? 0.14f : 0.07f) : white;
    }

    private static final ViewOutlineProvider CARD_OUTLINE = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(CARD_RADIUS_DP));
        }
    };

    private static final class Cards extends RecyclerView.ItemDecoration {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final RecyclerListView listView;
        private int seenVersion = NexaDesign.version();

        Cards(RecyclerListView listView) {
            this.listView = listView;
        }

        /** A switch was flipped in NexaGram settings: re-measure and rebind the visible chats. */
        private void checkVersion(RecyclerView parent) {
            if (seenVersion == NexaDesign.version()) {
                return;
            }
            seenVersion = NexaDesign.version();
            parent.post(() -> {
                listView.setSelectorRadius(NexaDesign.get(NexaDesign.CHAT_CARDS) ? dp(CARD_RADIUS_DP) : 0);
                listView.invalidateItemDecorations();
                if (listView.getAdapter() != null) {
                    listView.getAdapter().notifyDataSetChanged();
                }
            });
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            if (!(view instanceof DialogCell)) {
                return;
            }
            boolean cards = NexaDesign.get(NexaDesign.CHAT_CARDS);
            if (cards) {
                outRect.set(dp(SIDE_DP), dp(GAP_DP), dp(SIDE_DP), dp(GAP_DP));
            }
            if (cards != (view.getOutlineProvider() == CARD_OUTLINE)) {
                view.setOutlineProvider(cards ? CARD_OUTLINE : ViewOutlineProvider.BACKGROUND);
                view.setClipToOutline(cards);
            }
        }

        @Override
        public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            checkVersion(parent);
            boolean cards = NexaDesign.get(NexaDesign.CHAT_CARDS);
            boolean tint = NexaDesign.get(NexaDesign.PINNED_TINT);
            if (!cards && !tint) {
                return;
            }
            for (int i = 0, n = parent.getChildCount(); i < n; i++) {
                View child = parent.getChildAt(i);
                if (!(child instanceof DialogCell) || child.getAlpha() <= 0f) {
                    continue;
                }
                boolean pinned = tint && ((DialogCell) child).getIsPinned();
                if (!cards && !pinned) {
                    continue;
                }
                paint.setColor(cardColor(pinned));
                paint.setAlpha((int) (paint.getAlpha() * child.getAlpha()));
                float y = child.getY();
                rect.set(child.getLeft(), y, child.getRight(), y + child.getHeight());
                float r = cards ? dp(CARD_RADIUS_DP) : 0;
                canvas.drawRoundRect(rect, r, r, paint);
            }
        }
    }

    /** Soft gray behind the cards; reads the theme on every draw so day/night switches just work. */
    private static final class ListBackground extends Drawable {
        @Override
        public void draw(@NonNull Canvas canvas) {
            if (!NexaDesign.get(NexaDesign.CHAT_CARDS)) {
                canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                return;
            }
            int gray = Theme.getColor(Theme.key_windowBackgroundGray);
            canvas.drawColor(ColorUtils.blendARGB(gray, accent(), isDark() ? 0.03f : 0.025f));
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    // ---- search field ----

    /** Light accent-tinted search pill instead of Telegram's gray one. */
    public static Drawable searchBackground(Drawable telegram, boolean plainField) {
        if (!plainField || !NexaDesign.get(NexaDesign.ACCENT_SEARCH)) {
            return telegram;
        }
        int white = Theme.getColor(Theme.key_windowBackgroundWhite);
        int color = ColorUtils.blendARGB(white, accent(), isDark() ? 0.16f : 0.09f);
        return Theme.createRoundRectDrawable(dp(20), color);
    }

    public static void tintSearchIcon(ImageView icon) {
        if (!NexaDesign.get(NexaDesign.ACCENT_SEARCH)) {
            return;
        }
        icon.setColorFilter(new PorterDuffColorFilter(accent(), PorterDuff.Mode.SRC_IN));
    }
}
