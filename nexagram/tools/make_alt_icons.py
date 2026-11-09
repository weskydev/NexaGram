"""Builds the extra launcher icons (Settings -> NexaGram -> Appearance -> App icon).

Put square pictures into nexagram/branding/icons/<id>.png (ids below), then run:
    python nexagram/tools/make_alt_icons.py
A missing picture gets a temporary icon made from the main NexaGram icon.
Output goes to TMessagesProj/src/main/res (mipmap-*/nexa_alt_<id>*).
"""
import colorsys
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, "nexagram", "branding", "icons")
RES = os.path.join(ROOT, "TMessagesProj", "src", "main", "res")
MAIN = os.path.join(ROOT, "TMessagesProj_App", "src", "main", "res", "mipmap-xxxhdpi")

# keep in sync with app.nexagram.NexaIcons.IDS (without "default")
IDS = ["cosmos", "galaxy", "aurora", "nebula", "moon", "sunset", "ocean", "glass", "gold", "night"]
# temporary look while there is no picture: (hue shift in degrees, background brightness)
PLACEHOLDER = {
    "cosmos": (0, 0.25), "galaxy": (60, 0.3), "aurora": (-110, 0.3), "nebula": (80, 0.45), "moon": (-30, 0.2),
    "sunset": (130, 1.0), "ocean": (-60, 1.0), "glass": (-20, 1.0), "gold": (170, 0.2), "night": (0, 0.12),
}
DENS = {"xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
BIG = 1296  # 108dp * 12: the adaptive icon layer, of which the middle 72dp is visible


def placeholder(icon_id):
    bg = Image.open(os.path.join(MAIN, "nexa_icon_background.png")).convert("RGB").resize((BIG, BIG), Image.LANCZOS)
    fg = Image.open(os.path.join(MAIN, "nexa_icon_foreground.png")).convert("RGBA").resize((BIG, BIG), Image.LANCZOS)
    shift, light = PLACEHOLDER[icon_id]

    def recolor(img):
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                p = px[x, y]
                h, s, v = colorsys.rgb_to_hsv(p[0] / 255, p[1] / 255, p[2] / 255)
                r, g, b = colorsys.hsv_to_rgb((h + shift / 360) % 1, s, v)
                px[x, y] = (int(r * 255), int(g * 255), int(b * 255)) + tuple(p[3:])
        return img

    small = 216
    bg = recolor(bg.resize((small, small))).resize((BIG, BIG), Image.BICUBIC)
    bg = Image.eval(bg, lambda c: int(c * light))
    fgs = recolor(fg.resize((small * 2, small * 2), Image.LANCZOS)).resize((BIG, BIG), Image.BICUBIC)
    bg = bg.convert("RGBA")
    bg.alpha_composite(fgs)
    return bg.convert("RGB")


def from_picture(path):
    """Square picture -> 108dp layer: the picture fills the visible middle, a blurred copy fills the edges."""
    img = Image.open(path).convert("RGB")
    side = min(img.size)
    img = img.crop(((img.width - side) // 2, (img.height - side) // 2, (img.width + side) // 2, (img.height + side) // 2))
    # generators often draw their own rounded corners and a rim: cut them away
    cut = int(side * 0.075)
    img = img.crop((cut, cut, side - cut, side - cut))
    # the picture takes the middle 62dp of the 72dp visible icon, so round masks never cut the logo;
    # around it the background goes on as a mirror of the picture's own edges (softly blurred)
    inner = int(BIG * 62 / 108)
    pic = img.resize((inner, inner), Image.LANCZOS)
    pad = (BIG - inner) // 2
    arr = np.pad(np.asarray(pic), ((pad, BIG - inner - pad), (pad, BIG - inner - pad), (0, 0)), mode="symmetric")
    edge = Image.fromarray(arr).filter(ImageFilter.GaussianBlur(6))
    fade = int(inner * 0.05)
    mask = Image.new("L", (inner, inner), 0)
    ImageDraw.Draw(mask).rectangle((fade, fade, inner - fade, inner - fade), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(fade / 2))
    edge.paste(pic, (pad, pad), mask)
    return edge


def rounded(img, size):
    """Old (pre Android 8) launcher icon: visible part with rounded corners."""
    cut = BIG * 18 // 108
    vis = img.crop((cut, cut, BIG - cut, BIG - cut)).resize((size, size), Image.LANCZOS).convert("RGBA")
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=size * 4 // 5, fill=255)
    vis.putalpha(mask.resize((size, size), Image.LANCZOS))
    return vis


def main():
    anydpi = os.path.join(RES, "mipmap-anydpi-v26")
    os.makedirs(anydpi, exist_ok=True)
    for icon_id in IDS:
        path = os.path.join(SRC, icon_id + ".png")
        layer = from_picture(path) if os.path.exists(path) else placeholder(icon_id)
        for d, k in DENS.items():
            folder = os.path.join(RES, "mipmap-" + d)
            os.makedirs(folder, exist_ok=True)
            layer.resize((108 * k, 108 * k), Image.LANCZOS).save(os.path.join(folder, "nexa_alt_%s_bg.webp" % icon_id), quality=90)
            rounded(layer, 48 * k).save(os.path.join(folder, "nexa_alt_%s.webp" % icon_id), quality=90)
        with open(os.path.join(anydpi, "nexa_alt_%s.xml" % icon_id), "w", encoding="utf-8") as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                    '<!-- NexaGram: made by nexagram/tools/make_alt_icons.py -->\n'
                    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                    '    <background android:drawable="@mipmap/nexa_alt_%s_bg" />\n'
                    '    <foreground android:drawable="@android:color/transparent" />\n'
                    '    <monochrome android:drawable="@mipmap/nexa_alt_monochrome" />\n'
                    '</adaptive-icon>\n' % icon_id)
        print(icon_id, "picture" if os.path.exists(path) else "temporary")
    # themed (Android 13+) icon: the same white N as the main icon
    for d in DENS:
        src = os.path.join(ROOT, "TMessagesProj_App", "src", "main", "res", "mipmap-" + d, "nexa_icon_monochrome.png")
        Image.open(src).save(os.path.join(RES, "mipmap-" + d, "nexa_alt_monochrome.png"))


if __name__ == "__main__":
    main()
