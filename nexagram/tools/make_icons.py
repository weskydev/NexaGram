"""Builds NexaGram launcher/notification/Play assets from nexagram/branding sources."""
import os
import sys

from PIL import Image, ImageChops, ImageDraw

SRC = sys.argv[1]          # nexagram/branding
RES = sys.argv[2]          # TMessagesProj_App/src/main/res
OUT = os.path.join(SRC, "out")
os.makedirs(OUT, exist_ok=True)

DENS = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
CANVAS = 1296              # work canvas = 108dp * 12
GLYPH = 0.42               # glyph size relative to the 108dp canvas (safe zone is 66dp ~ 0.61 circle)


def load(name):
    return Image.open(os.path.join(SRC, name))


def fit_glyph(img, alpha_cut):
    """Center the glyph (by its opaque bounding box) on a CANVAS, scaled to GLYPH."""
    img = img.convert("RGBA")
    a = img.split()[3]
    box = a.point(lambda v: 255 if v > alpha_cut else 0).getbbox()
    glyph = img.crop(box)
    scale = GLYPH * CANVAS / max(glyph.size)
    # keep a little of the glow around the glyph
    pad = int(max(glyph.size) * 0.08)
    box = (max(0, box[0] - pad), max(0, box[1] - pad), min(img.width, box[2] + pad), min(img.height, box[3] + pad))
    glyph = img.crop(box)
    glyph = glyph.resize((round(glyph.width * scale), round(glyph.height * scale)), Image.LANCZOS)
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    canvas.alpha_composite(glyph, ((CANVAS - glyph.width) // 2, (CANVAS - glyph.height) // 2))
    return canvas


def full_bleed(img, inset_ratio):
    """Cut away rounded corners by cropping to the center and filling the canvas."""
    img = img.convert("RGB")
    i = int(img.width * inset_ratio)
    return img.crop((i, i, img.width - i, img.height - i)).resize((CANVAS, CANVAS), Image.LANCZOS)


def save_mipmaps(img, name, dp):
    for d, k in DENS.items():
        folder = os.path.join(RES, f"mipmap-{d}")
        os.makedirs(folder, exist_ok=True)
        px = round(dp * k)
        img.resize((px, px), Image.LANCZOS).save(os.path.join(folder, name + ".png"), optimize=True)


def mask(size, round_shape):
    m = Image.new("L", (size, size), 0)
    dr = ImageDraw.Draw(m)
    if round_shape:
        dr.ellipse((0, 0, size - 1, size - 1), fill=255)
    else:
        dr.rounded_rectangle((0, 0, size - 1, size - 1), radius=int(size * 0.22), fill=255)
    return m


background = full_bleed(load("icon_background.png"), 0.10)
foreground = fit_glyph(load("icon_foreground.png"), 128)

mono = fit_glyph(load("icon_monochrome.png"), 128)
# monochrome must be a clean single-color silhouette: drop faint noise, force white
ma = mono.split()[3].point(lambda v: 0 if v < 60 else v)
mono = Image.merge("RGBA", (ma.point(lambda v: 255),) * 3 + (ma,))

# adaptive icon layers (108dp)
save_mipmaps(background, "nexa_icon_background", 108)
save_mipmaps(foreground, "nexa_icon_foreground", 108)
save_mipmaps(mono, "nexa_icon_monochrome", 108)

# legacy icons for Android < 8 (48dp): visible part of the adaptive icon = central 72dp
full = background.convert("RGBA")
full.alpha_composite(foreground)
c = CANVAS // 6
visible = full.crop((c, c, CANVAS - c, CANVAS - c))
for name, rnd in (("ic_launcher", False), ("ic_launcher_round", True)):
    out = visible.copy()
    out.putalpha(mask(out.width, rnd))
    save_mipmaps(out, name, 48)

# notification icon (24dp, white silhouette)
notif = load("icon_notification.png").convert("RGBA")
na = notif.split()[3]
notif = Image.merge("RGBA", (na.point(lambda v: 255),) * 3 + (na,))
for d, k in DENS.items():
    folder = os.path.join(RES, f"drawable-{d}")
    os.makedirs(folder, exist_ok=True)
    px = round(24 * k)
    notif.resize((px, px), Image.LANCZOS).save(os.path.join(folder, "notification.png"), optimize=True)

# Google Play assets
play = load("play_icon.png").convert("RGB")
i = int(play.width * 0.065)
play.crop((i, i, play.width - i, play.height - i)).resize((512, 512), Image.LANCZOS).save(os.path.join(OUT, "play_icon_512.png"))

fg = load("feature_graphic.png").convert("RGB")
w, h = 1024, round(fg.height * 1024 / fg.width)
fg = fg.resize((w, h), Image.LANCZOS)
top = (h - 500) // 2
fg.crop((0, top, 1024, top + 500)).save(os.path.join(OUT, "feature_graphic_1024x500.png"))

# preview sheet: circle, squircle, themed (mono) and legacy
prev = Image.new("RGBA", (4 * 300 + 50, 350), (40, 40, 48, 255))
for n, (img, rnd) in enumerate(((visible, True), (visible, False))):
    t = img.resize((260, 260), Image.LANCZOS)
    t.putalpha(mask(260, rnd))
    prev.alpha_composite(t, (20 + n * 300, 45))
themed = Image.new("RGBA", visible.size, (225, 220, 245, 255))
mvis = mono.crop((c, c, CANVAS - c, CANVAS - c))
tint = Image.merge("RGBA", (mvis.split()[3].point(lambda v: 70),) * 1 + (mvis.split()[3].point(lambda v: 50),) + (mvis.split()[3].point(lambda v: 140),) + (mvis.split()[3],))
themed.alpha_composite(tint)
t = themed.resize((260, 260), Image.LANCZOS)
t.putalpha(mask(260, True))
prev.alpha_composite(t, (620, 45))
legacy = Image.open(os.path.join(RES, "mipmap-xxxhdpi", "ic_launcher.png")).convert("RGBA").resize((192, 192))
prev.alpha_composite(legacy, (950, 80))
prev.save(os.path.join(OUT, "preview.png"))
print("done")
