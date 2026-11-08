package app.nexagram;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;

/** NexaGram look for the Contacts tab (and the "New message" screen, which shares its list). */
public final class NexaContacts {

    /** Violet gradients for the colorful row icons (first row, second row). */
    private static final int[][] ICON_COLORS = {
            {0xFF9B85FF, 0xFF6A4FF0},
            {0xFFC08CFF, 0xFF9356F0},
    };

    private NexaContacts() {
    }

    private static boolean enabled() {
        return NexaDesign.get(NexaDesign.CONTACTS_DESIGN);
    }

    public static int iconTop(int row, int telegram) {
        return enabled() && row < ICON_COLORS.length ? ICON_COLORS[row][0] : telegram;
    }

    public static int iconBottom(int row, int telegram) {
        return enabled() && row < ICON_COLORS.length ? ICON_COLORS[row][1] : telegram;
    }

    /** Soft accent-tinted gray behind the contact cards; plain Telegram gray when switched off. */
    public static void background(View listView) {
        listView.setBackground(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                int gray = Theme.getColor(Theme.key_windowBackgroundGray);
                if (enabled()) {
                    gray = ColorUtils.blendARGB(gray, NexaChatList.accent(), Theme.isCurrentThemeDark() ? 0.03f : 0.025f);
                }
                canvas.drawColor(gray);
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
        });
    }
}
