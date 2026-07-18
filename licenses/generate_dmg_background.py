#!/usr/bin/env python3
"""
Generate MD3-style DMG background for JMCL.
Minimalist: two rounded outline cards + clean geometric arrow. No text.
Requires: pip3 install Pillow
"""

import sys
import os
import math

try:
    from PIL import Image, ImageDraw, ImageFilter
except ImportError:
    print("ERROR: Pillow not installed. Run: pip3 install Pillow", file=sys.stderr)
    sys.exit(1)

# ─── MD3 Light Theme Tokens ─────────────────────────────────────────────────
SURFACE       = (254, 247, 255)
PRIMARY       = (103, 80, 164)
OUTLINE       = (202, 196, 208)

# ─── Layout ─────────────────────────────────────────────────────────────────
W, H        = 660, 400
CARD_W      = 140
CARD_H      = 140
RADIUS      = 22
GAP         = 280
CENTER_Y    = H // 2 - 12

LEFT_CX     = W // 2 - GAP // 2
RIGHT_CX    = W // 2 + GAP // 2
ARROW_CX    = W // 2
ARROW_CY    = CENTER_Y


def card_rect(cx, cy):
    return [cx - CARD_W // 2, cy - CARD_H // 2, cx + CARD_W // 2, cy + CARD_H // 2]


def card_shadow_rect(cx, cy, spread=10):
    return [cx - CARD_W // 2 - spread, cy - CARD_H // 2 - spread,
            cx + CARD_W // 2 + spread, cy + CARD_H // 2 + spread]


def draw_arrow(draw, cx, cy, shaft_w=84, head_w=18, lw=4, color=PRIMARY):
    """Draw a clean geometric chevron arrow → with rounded shaft ends."""
    hw = shaft_w // 2

    # Horizontal shaft with rounded caps
    shaft_left  = cx - hw
    shaft_right = cx + hw - head_w
    draw.rounded_rectangle(
        [shaft_left, cy - lw, shaft_right, cy + lw],
        radius=lw, fill=color
    )

    # Chevron head ( > ) — two clean angled lines
    tip = (cx + hw, cy)
    top = (tip[0] - head_w, cy - head_w)
    bot = (tip[0] - head_w, cy + head_w)
    draw.line([top, tip, bot], fill=color, width=lw, joint="curve")


def main():
    if len(sys.argv) < 2:
        print("Usage: generate_dmg_background.py <output_path>", file=sys.stderr)
        sys.exit(1)

    output_path = sys.argv[1]
    os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)

    img = Image.new("RGBA", (W, H), SURFACE + (255,))
    draw = ImageDraw.Draw(img)

    # ── 1. Soft shadows ──
    for cx, cy in ((LEFT_CX, CENTER_Y), (RIGHT_CX, CENTER_Y)):
        sr = card_shadow_rect(cx, cy, spread=12)
        shadow = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ImageDraw.Draw(shadow).rounded_rectangle(
            sr, radius=RADIUS + 12, fill=(0, 0, 0, 28)
        )
        shadow = shadow.filter(ImageFilter.GaussianBlur(radius=12))
        img.paste(shadow, (0, 0), shadow)

    # ── 2. Two outline cards ──
    for cx, cy in ((LEFT_CX, CENTER_Y), (RIGHT_CX, CENTER_Y)):
        cr = card_rect(cx, cy)
        draw.rounded_rectangle(cr, radius=RADIUS, fill=(255, 255, 255, 200))
        draw.rounded_rectangle(cr, radius=RADIUS, outline=OUTLINE, width=1)

    # ── 3. Arrow ──
    draw_arrow(draw, ARROW_CX, ARROW_CY)

    # ── Save as both TIFF + PNG ──
    rgb = Image.new("RGB", img.size, SURFACE)
    rgb.paste(img, (0, 0), img)

    tiff_path = os.path.splitext(output_path)[0] + ".tiff"
    rgb.save(tiff_path, "TIFF")
    rgb.save(output_path, "PNG")

    tiff_kb = os.path.getsize(tiff_path) / 1024
    png_kb = os.path.getsize(output_path) / 1024
    print(f"DMG background: {tiff_path} ({tiff_kb:.1f} KB), {output_path} ({png_kb:.1f} KB)")


if __name__ == "__main__":
    main()
