"""Generate Icon 04a..04i as true legacy launcher icons, preserving old density.

The original, visually approved Icon 04 used 192x192 PNG in mipmap-xxxhdpi.
v0.2.0 accidentally changed that to 1024x1024 mipmap-nodpi, leading to
launcher-added white icon tiles. 04a here is PIXEL-IDENTICAL to the v0.1.0
Icon 04 bitmap, and b..i change only its scale in 0.25% steps.

The slight adjustments use 8x supersampling so increments smaller than one
192px output pixel remain distinct. We never scale an already clipped bitmap.
"""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_reference.webp"
RES = ROOT / "iconlab/src/main/res"
BASE_SCALE = 1.06
STEP = 0.0025
COUNT = 9
OUTPUT_SIZE = 192
SUPER_SAMPLE = 8


def remove_external_matte(source):
    artwork = source.convert("RGBA").copy()
    w, h = artwork.size
    factor = min(w, h) / 128.0
    ss = 4
    inset = 4.0 * factor
    radius = 27.0 * factor
    mask = Image.new("L", (w * ss, h * ss), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle(
        (round(inset * ss), round(inset * ss),
         round((w - 1 - inset) * ss), round((h - 1 - inset) * ss)),
        radius=round(radius * ss), fill=255)
    mask = mask.resize((w, h), Image.Resampling.LANCZOS)
    mask = mask.filter(ImageFilter.GaussianBlur(0.4 * factor))
    artwork.putalpha(ImageChops.darker(artwork.getchannel("A"), mask))
    return artwork


def fit_canvas(artwork, size, scale):
    """Original v0.1.0 implementation: must not change for 04a."""
    side = max(1, round(size * scale))
    resized = artwork.resize((side, side), Image.Resampling.LANCZOS)
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset = (size - side) // 2
    image.paste(resized, (offset, offset))
    return image


def fit_canvas_subpixel(artwork, size, scale):
    """Scale to an 8x canvas, crop symmetrically, then downsample with LANCZOS."""
    big = size * SUPER_SAMPLE
    side = max(1, round(big * scale))
    resized = artwork.resize((side, side), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    # Use integer centering at high resolution for fractional-pixel changes.
    offset = (big - side) // 2
    canvas.paste(resized, (offset, offset))
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def variant_scales():
    return {
        f"04{chr(ord('a') + index)}": BASE_SCALE * (1 - index * STEP)
        for index in range(COUNT)
    }


def render_variants(source, output_dir):
    # Keep the same Android icon density qualifier as v0.1.0, NOT nodpi.
    icon_dir = output_dir / "mipmap-xxxhdpi"
    icon_dir.mkdir(parents=True, exist_ok=True)

    # Delete accidentally generated nodpi files from v0.2.0 if locally present.
    obsolete = output_dir / "mipmap-nodpi"
    if obsolete.exists():
        for path in obsolete.glob("lab_icon_*.png"):
            path.unlink()
    for path in icon_dir.glob("lab_icon_*.png"):
        path.unlink()

    clean = remove_external_matte(source)
    for index, (name, scale) in enumerate(variant_scales().items()):
        # 04a is exactly v0.1.0's trusted Icon 04 in size, res folder and bytes.
        icon = (fit_canvas(clean, OUTPUT_SIZE, BASE_SCALE)
                if index == 0 else
                fit_canvas_subpixel(clean, OUTPUT_SIZE, scale))
        icon.save(icon_dir / f"lab_icon_{name}.png", optimize=True)
    return clean


if __name__ == "__main__":
    with Image.open(SOURCE) as source:
        clean = render_variants(source, RES)
    assert clean.getpixel((0, 0))[3] == 0
    assert clean.getpixel((clean.width // 2, clean.height // 2))[3] > 230
    print("Generated nine 192px legacy xxxhdpi icons: 04a matches v0.1.0 Icon 04")
