package app.nexagram;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AnimatedFileNative;
import org.telegram.ui.Components.voip.AnimatedFileInfo;

import java.util.HashMap;

/**
 * Sends a video from the gallery as a round video message: center square crop,
 * 384x384, first 60 seconds. Uses Telegram's own converter, the same one the
 * photo editor uses for cropped videos.
 */
public final class NexaRoundVideo {

    public static final String KEY = "feature_round_video";

    private static final int SIZE = 384;
    private static final long MAX_DURATION_MS = 60_000;
    private static final int BITRATE = 1_000_000;

    private NexaRoundVideo() {
    }

    public static boolean isEnabled() {
        return NexaDesign.get(KEY);
    }

    /** The single selected gallery item, if it is a video we can send as a circle. */
    public static MediaController.PhotoEntry selectedVideo(ChatActivity chat, HashMap<Object, Object> selected) {
        if (!isEnabled() || chat == null || selected == null || selected.size() != 1) {
            return null;
        }
        Object item = selected.values().iterator().next();
        if (!(item instanceof MediaController.PhotoEntry)) {
            return null;
        }
        MediaController.PhotoEntry entry = (MediaController.PhotoEntry) item;
        if (!entry.isVideo || entry.path == null) {
            return null;
        }
        if (chat.getCurrentChat() != null && !ChatObject.canSendRoundVideo(chat.getCurrentChat())) {
            return null;
        }
        return entry;
    }

    /** Builds the round video settings and sends it. Returns false if the file can't be converted. */
    public static boolean send(ChatActivity chat, MediaController.PhotoEntry entry, boolean notify, int scheduleDate) {
        VideoEditedInfo info = buildInfo(entry);
        if (info == null) {
            return false;
        }
        MediaController.PhotoEntry toSend = new MediaController.PhotoEntry(0, 0, 0, entry.path, 0, true, 0, 0, 0);
        toSend.ttl = 0;
        chat.sendMedia(toSend, info, notify, scheduleDate, 0, false, 0);
        return true;
    }

    private static VideoEditedInfo buildInfo(MediaController.PhotoEntry entry) {
        int[] params = new int[AnimatedFileInfo.PARAM_NUM_COUNT];
        try {
            AnimatedFileNative.getVideoInfo(entry.path, params, 0);
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
        if (params[AnimatedFileInfo.PARAM_NUM_SUPPORTED_VIDEO_CODEC] == 0) {
            return null;
        }
        int width = params[AnimatedFileInfo.PARAM_NUM_WIDTH];
        int height = params[AnimatedFileInfo.PARAM_NUM_HEIGHT];
        int rotation = params[AnimatedFileInfo.PARAM_NUM_ROTATION];
        long durationMs = params[AnimatedFileInfo.PARAM_NUM_DURATION];
        if (width <= 0 || height <= 0) {
            return null;
        }

        VideoEditedInfo info = new VideoEditedInfo();
        info.roundVideo = true;
        info.originalPath = entry.path;
        info.originalWidth = width;
        info.originalHeight = height;
        info.rotationValue = rotation;
        info.framerate = Math.max(1, Math.min(30, params[AnimatedFileInfo.PARAM_NUM_FRAMERATE]));
        info.originalDuration = durationMs * 1000;

        // trim chosen in Telegram's editor, if any; a circle can't be longer than a minute
        long startUs = 0;
        long endUs = durationMs * 1000;
        if (entry.editedInfo != null) {
            if (entry.editedInfo.startTime > 0) {
                startUs = entry.editedInfo.startTime;
            }
            if (entry.editedInfo.endTime > 0) {
                endUs = entry.editedInfo.endTime;
            }
        }
        endUs = Math.min(endUs, startUs + MAX_DURATION_MS * 1000);
        info.startTime = startUs;
        info.endTime = endUs;
        info.estimatedDuration = (endUs - startUs) / 1000;

        // the frame as the viewer sees it (rotation applied)
        boolean sideways = rotation == 90 || rotation == 270;
        int shownW = sideways ? height : width;
        int shownH = sideways ? width : height;
        int side = Math.min(shownW, shownH);
        float scale = SIZE / (float) side;
        info.resultWidth = Math.round(width * scale);
        info.resultHeight = Math.round(height * scale);
        info.bitrate = BITRATE;
        info.estimatedSize = Math.max(1, info.estimatedDuration * BITRATE / 8000);

        MediaController.CropState crop = new MediaController.CropState();
        crop.cropPw = side / (float) shownW;
        crop.cropPh = side / (float) shownH;
        crop.cropScale = 1f;
        crop.transformWidth = SIZE;
        crop.transformHeight = SIZE;
        info.cropState = crop;
        return info;
    }
}
